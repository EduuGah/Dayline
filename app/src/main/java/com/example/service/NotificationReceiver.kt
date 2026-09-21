package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dayline_reminders"
        const val CHANNEL_NAME = "Lembretes da Linha do Tempo"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TIME_TEXT = "extra_time_text"
        const val EXTRA_CATEGORY = "extra_category"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Compromisso na sua linha do tempo"
        val timeText = intent.getStringExtra(EXTRA_TIME_TEXT) ?: "Horário do compromisso"
        val category = intent.getStringExtra(EXTRA_CATEGORY)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações e lembretes para compromissos do Dayline"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_event_id", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentSubtitle = if (!category.isNullOrBlank()) "$timeText • $category" else timeText

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentSubtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)
    }
}
