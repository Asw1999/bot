package com.cocbot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.opencv.android.OpenCVLoader
import com.cocbot.util.BotLog

class CocBotApp : Application() {
    companion object {
        const val CHANNEL_ID = "cocbot_service"
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        // Initialize OpenCV
        if (OpenCVLoader.initLocal()) {
            BotLog.i("OpenCV loaded successfully")
        } else {
            BotLog.e("OpenCV initialization failed")
        }
    }
}