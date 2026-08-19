package com.cocbot.capture

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.cocbot.CocBotApp
import com.cocbot.R
import com.cocbot.util.BotLog

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var latestBitmap: Bitmap? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startForegroundNotification()
                    acquireWakeLock()
                    startProjection(resultCode, data)
                }
            }
            ACTION_STOP -> {
                stopProjection()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val notification = NotificationCompat.Builder(this, CocBotApp.CHANNEL_ID)
                .setContentTitle("CoC Bot")
                .setContentText("Screen capture active")
                .setSmallIcon(R.drawable.ic_bot)
                .setOngoing(true)
                .build()
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            val notification = NotificationCompat.Builder(this, CocBotApp.CHANNEL_ID)
                .setContentTitle("CoC Bot")
                .setContentText("Screen capture active")
                .setSmallIcon(R.drawable.ic_bot)
                .setOngoing(true)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CocBot::ScreenCapture"
        ).apply { acquire(4 * 60 * 60 * 1000L) } // 4 hours max
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        // Callback required on Android 14+ (API 34)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopProjection()
            }
        }, null)

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val dpi = metrics.densityDpi

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * w

                val bmp = Bitmap.createBitmap(
                    w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)

                // Crop to exact screen size (remove row padding)
                val cropped = if (rowPadding > 0) {
                    Bitmap.createBitmap(bmp, 0, 0, w, h).also { bmp.recycle() }
                } else bmp

                latestBitmap?.recycle()
                latestBitmap = cropped
            } finally {
                image.close()
            }
        }, null)

        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "CocBotCapture", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
        BotLog.i("Screen capture started (${w}x${h})")
        instance = this
    }

    private fun stopProjection() {
        BotLog.i("Screen capture stopped")
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        latestBitmap?.recycle()
        latestBitmap = null
        wakeLock?.let { if (it.isHeld) it.release() }
        instance = null
    }

    override fun onDestroy() {
        stopProjection()
        super.onDestroy()
    }

    /** Grab latest captured frame. Returns null if not capturing. */
    fun captureFrame(): Bitmap? {
        return latestBitmap?.let { Bitmap.createBitmap(it) }
    }

    companion object {
        const val ACTION_START = "com.cocbot.START_CAPTURE"
        const val ACTION_STOP = "com.cocbot.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        var instance: ScreenCaptureService? = null
            private set

        fun startCapture(ctx: Context, resultCode: Int, data: Intent) {
            val intent = Intent(ctx, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            ctx.startForegroundService(intent)
        }

        fun stopCapture(ctx: Context) {
            val intent = Intent(ctx, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            ctx.startService(intent)
        }
    }
}