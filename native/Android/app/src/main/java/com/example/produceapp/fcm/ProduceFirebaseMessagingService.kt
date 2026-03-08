package com.example.produceapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.produceapp.BuildConfig
import com.example.produceapp.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ProduceFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "produce_price_alerts"
        private const val CHANNEL_NAME = "農產品價格提醒"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "菜價提醒"
        val body = message.notification?.body ?: ""
        val produceId = message.data["produceId"]

        showNotification(title, body, produceId)
    }

    private fun showNotification(title: String, body: String, produceId: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            produceId?.let { putExtra("produceId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendTokenToBackend(fcmToken: String) {
        val prefs = getSharedPreferences("produce_prefs", Context.MODE_PRIVATE)
        val jwt = prefs.getString("jwt_token", null) ?: return

        Thread {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply { put("token", fcmToken) }
                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}fcm/token")
                    .addHeader("Authorization", "Bearer $jwt")
                    .post(body)
                    .build()

                client.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
