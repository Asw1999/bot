package com.cocbot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

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
    }
}