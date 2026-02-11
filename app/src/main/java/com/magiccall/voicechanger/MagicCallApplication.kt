package com.magiccall.voicechanger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MagicCallApplication : Application() {

    companion object {
        const val CHANNEL_ID = "voice_changer_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Changer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice changing notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
