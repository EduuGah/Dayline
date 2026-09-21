package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_items")
data class EventItem(
    @PrimaryKey val id: String,
    val type: String = TYPE_EVENT, // EVENT, TASK
    val title: String,
    val description: String? = null,
    val date: String? = null, // YYYY-MM-DD (null for Inbox unscheduled tasks)
    val startTime: String? = null, // HH:mm
    val endTime: String? = null, // HH:mm
    val durationMinutes: Int = 30,
    val isAllDay: Boolean = false,
    val categoryId: String? = null,
    val priority: String = PRIORITY_NORMAL, // LOW, NORMAL, HIGH
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val reminderMinutesBefore: Int = -1, // -1 = no reminder, 0 = at time, 10 = 10 min before, etc.
    val recurrenceRuleId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EVENT = "EVENT"
        const val TYPE_TASK = "TASK"

        const val PRIORITY_LOW = "LOW"
        const val PRIORITY_NORMAL = "NORMAL"
        const val PRIORITY_HIGH = "HIGH"
    }
}
