package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurrence_exceptions")
data class RecurrenceException(
    @PrimaryKey val id: String,
    val recurrenceRuleId: String,
    val originalOccurrenceDate: String, // YYYY-MM-DD
    val action: String, // SKIPPED, MODIFIED
    val overrideItemId: String? = null
)
