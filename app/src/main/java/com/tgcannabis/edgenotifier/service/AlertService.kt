package com.tgcannabis.edgenotifier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tgcannabis.edgenotifier.R

class AlertService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var client: WebSocketClient

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        client = WebSocketClient(this, "ws://181.57.18.22:8085/ws/websocket")
        client.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createPersistentNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        client.close()
        super.onDestroy()
    }

    private fun createPersistentNotification(): Notification {
        val channelId = "alerts_channel"
        val channelName = "Alert Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitor de Alertas")
            .setContentText("Conectado al Edge Hub")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
    }
}