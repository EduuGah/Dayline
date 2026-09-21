package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurrence_rules")
data class RecurrenceRule(
    @PrimaryKey val id: String,
    val frequency: String, // DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY, CUSTOM_DAYS
    val interval: Int = 1,
    val weekdaysMask: String = "", // e.g. "MONDAY,TUESDAY,WEDNESDAY"
    val startsOnDate: String, // YYYY-MM-DD
    val endsOnDate: String? = null, // YYYY-MM-DD
    val count: Int? = null
)
