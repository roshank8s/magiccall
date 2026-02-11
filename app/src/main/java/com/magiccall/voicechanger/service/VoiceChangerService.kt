package com.magiccall.voicechanger.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.magiccall.voicechanger.MagicCallApplication
import com.magiccall.voicechanger.MainActivity
import com.magiccall.voicechanger.R
import com.magiccall.voicechanger.audio.AudioEngine
import com.magiccall.voicechanger.audio.VoiceEffect

class VoiceChangerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): VoiceChangerService = this@VoiceChangerService
    }

    private val binder = LocalBinder()
    private lateinit var audioEngine: AudioEngine

    val isRunning: Boolean get() = audioEngine.isRunning

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVoiceChanger()
            ACTION_STOP -> stopVoiceChanger()
        }
        return START_STICKY
    }

    private fun startVoiceChanger() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        audioEngine.start()
    }

    private fun stopVoiceChanger() {
        audioEngine.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun setEffect(effect: VoiceEffect?) {
        audioEngine.setEffect(effect)
    }

    fun setAmplitudeCallback(callback: ((Float) -> Unit)?) {
        audioEngine.onAmplitudeUpdate = callback
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MagicCallApplication.CHANNEL_ID)
            .setContentTitle("MagicCall Active")
            .setContentText("Voice changer is running")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        audioEngine.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.magiccall.START"
        const val ACTION_STOP = "com.magiccall.STOP"
        const val NOTIFICATION_ID = 1001
    }
}
