package com.tgcannabis.edgenotifier.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tgcannabis.edgenotifier.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import org.json.JSONObject

class WebSocketClient(private val context: Context, url: String) {
    private val stompClient: StompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
    private val disposables = CompositeDisposable()

    fun connect() {
        disposables.add(
            stompClient.lifecycle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ lifecycleEvent ->
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.OPENED -> {
                            Log.d("WS", "Conectado al WebSocket")
                            subscribeToAlerts() // suscribirse SOLO cuando esté conectado
                        }

                        LifecycleEvent.Type.ERROR -> {
                            Log.e("WS", "Error WebSocket", lifecycleEvent.exception)
                        }

                        LifecycleEvent.Type.CLOSED -> {
                            Log.d("WS", "Conexión WebSocket cerrada")
                        }

                        else -> {}
                    }
                }, { error ->
                    Log.e("WS", "Error en el ciclo de vida del WebSocket", error)
                })
        )

        stompClient.connect()
    }

    private fun subscribeToAlerts() {
        disposables.add(
            stompClient.topic("/topic/alerts")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage ->
                    val payload = topicMessage.payload
                    Log.d("WS", "Alerta recibida: $payload")
                    showAlertNotification(payload)
                }, { error ->
                    Log.e("WS", "Error al suscribirse a alertas", error)
                })
        )
    }

    private fun showAlertNotification(message: String) {
        Log.d("ws", "Generando notificación...")

        try {
            val json = JSONObject(message)

            val sensorType = json.getString("sensorType")
            val alertType = json.getString("alertType")
            val currentValue = json.getDouble("currentValue")
            val durationSeconds = json.getLong("durationSeconds")
            val alertMessage = json.getString("message")

            val channelId = "incoming_alerts"
            val channelName = "Alertas del Edge Hub"
            val notificationId = System.currentTimeMillis().toInt()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                manager.createNotificationChannel(channel)
            }

            val title = "⚠️ Alerta: $sensorType $alertType"
            val content = "$alertMessage\nValor actual: $currentValue\nDuración: ${durationSeconds}s"

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // para contenido largo
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            manager.notify(notificationId, notification)

        } catch (e: Exception) {
            Log.e("ws", "Error al generar la notificación: ${e.message}", e)
        }
    }

    fun close() {
        stompClient.disconnect()
        disposables.clear()
    }
}