package com.example.model

import java.time.LocalDate
import java.time.LocalTime

enum class EventStatus {
    PAST,
    NOW,
    UPCOMING,
    COMPLETED,
    OVERDUE
}

enum class RecurrenceScope {
    THIS_ONLY,
    THIS_AND_FUTURE,
    ALL_SERIES
}

data class EventOccurrence(
    val event: EventItem,
    val occurrenceDate: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val status: EventStatus,
    val progressFraction: Float = 0f,
    val remainingMinutes: Long? = null,
    val category: Category? = null,
    val isRecurring: Boolean = false,
    val hasConflict: Boolean = false
)

data class FreeInterval(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Long
)

sealed interface TimelineRow {
    data class EventRow(val occurrence: EventOccurrence) : TimelineRow
    data class FreeIntervalRow(val interval: FreeInterval) : TimelineRow
}

data class DaySummary(
    val date: LocalDate,
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val currentOccurrence: EventOccurrence? = null,
    val nextOccurrence: EventOccurrence? = null,
    val minutesToNext: Long? = null,
    val totalBusyMinutes: Long = 0,
    val realizedMinutes: Long = 0,
    val totalFreeMinutes: Long = 0
)

data class WeekSummary(
    val startOfWeek: LocalDate,
    val endOfWeek: LocalDate,
    val totalBusyMinutes: Long = 0,
    val realizedMinutes: Long = 0,
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val routineRate: Float = 0f,
    val punctualityRate: Float = 0f,
    val avgFreeMinutesPerDay: Long = 0,
    val busiestDayName: String = "",
    val busiestDayMinutes: Long = 0
)

data class CategoryTimeStat(
    val category: Category,
    val totalMinutes: Long,
    val count: Int,
    val percentage: Float
)
