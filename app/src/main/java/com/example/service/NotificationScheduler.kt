package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.EventItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"

    fun scheduleReminder(context: Context, event: EventItem, categoryName: String? = null) {
        if (event.reminderMinutesBefore < 0 || event.startTime.isNullOrBlank() || event.date.isNullOrBlank()) {
            return
        }

        try {
            val date = LocalDate.parse(event.date)
            val time = LocalTime.parse(event.startTime)
            val eventDateTime = LocalDateTime.of(date, time)
            val triggerDateTime = eventDateTime.minusMinutes(event.reminderMinutesBefore.toLong())

            val now = LocalDateTime.now()
            if (triggerDateTime.isBefore(now)) {
                // Time has already passed
                return
            }

            val triggerEpochMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
                putExtra(NotificationReceiver.EXTRA_TITLE, event.title)
                val timeNote = if (event.reminderMinutesBefore == 0) {
                    "Agora: ${event.startTime}"
                } else {
                    "Em ${event.reminderMinutesBefore} min (${event.startTime})"
                }
                putExtra(NotificationReceiver.EXTRA_TIME_TEXT, timeNote)
                putExtra(NotificationReceiver.EXTRA_CATEGORY, categoryName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder for ${event.id}: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, eventId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling reminder for $eventId: ${e.message}")
        }
    }
}
