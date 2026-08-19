package com.cocbot.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.cocbot.R
import com.cocbot.util.BotLog

/**
 * Floating overlay panel showing bot status + controls.
 * Draggable, collapsible.
 */
class OverlayManager(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isShowing = false

    var onStartClick: (() -> Unit)? = null
    var onStopClick: (() -> Unit)? = null

    private var statusText: TextView? = null
    private var logText: TextView? = null
    private var logScroll: ScrollView? = null
    private var btnStart: Button? = null
    private var btnStop: Button? = null

    fun show() {
        if (isShowing) return
        val inflater = LayoutInflater.from(ctx)
        overlayView = inflater.inflate(R.layout.overlay_panel, null)

        statusText = overlayView!!.findViewById(R.id.tvStatus)
        logText = overlayView!!.findViewById(R.id.tvLog)
        logScroll = overlayView!!.findViewById(R.id.scrollLog)
        btnStart = overlayView!!.findViewById(R.id.btnStart)
        btnStop = overlayView!!.findViewById(R.id.btnStop)

        btnStart?.setOnClickListener { onStartClick?.invoke() }
        btnStop?.setOnClickListener { onStopClick?.invoke() }

        // Log listener
        BotLog.onNewLine = { line ->
            logText?.post {
                logText?.append("$line\n")
                logScroll?.fullScroll(View.FOCUS_DOWN)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 100
        }

        // Draggable
        setupDrag(overlayView!!, params)

        wm.addView(overlayView, params)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        BotLog.onNewLine = null
        wm.removeView(overlayView)
        overlayView = null
        isShowing = false
    }

    fun updateStatus(status: String) {
        statusText?.post { statusText?.text = status }
    }

    fun setRunning(running: Boolean) {
        btnStart?.post {
            btnStart?.isEnabled = !running
            btnStop?.isEnabled = running
        }
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initX = 0; var initY = 0
        var initTouchX = 0f; var initTouchY = 0f

        val dragHandle = view.findViewById<View>(R.id.dragHandle)
        (dragHandle ?: view).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    initTouchX = event.rawX; initTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX + (event.rawX - initTouchX).toInt()
                    params.y = initY + (event.rawY - initTouchY).toInt()
                    wm.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }
}