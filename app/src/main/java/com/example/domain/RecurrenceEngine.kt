package com.example.domain

import com.example.model.EventItem
import com.example.model.RecurrenceException
import com.example.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object RecurrenceEngine {

    /**
     * Determines if a recurrence rule produces an occurrence on the target date.
     */
    fun matchesDate(rule: RecurrenceRule, targetDate: LocalDate): Boolean {
        val startDate = try {
            LocalDate.parse(rule.startsOnDate)
        } catch (e: Exception) {
            return false
        }

        if (targetDate.isBefore(startDate)) {
            return false
        }

        if (!rule.endsOnDate.isNullOrBlank()) {
            val endDate = try {
                LocalDate.parse(rule.endsOnDate)
            } catch (e: Exception) {
                null
            }
            if (endDate != null && targetDate.isAfter(endDate)) {
                return false
            }
        }

        val interval = if (rule.interval > 0) rule.interval else 1

        return when (rule.frequency) {
            "DAILY" -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, targetDate)
                daysBetween % interval == 0L
            }
            "WEEKDAYS" -> {
                val dow = targetDate.dayOfWeek
                dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            }
            "WEEKENDS" -> {
                val dow = targetDate.dayOfWeek
                dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY
            }
            "WEEKLY" -> {
                if (targetDate.dayOfWeek == startDate.dayOfWeek) {
                    val weeksBetween = ChronoUnit.WEEKS.between(startDate, targetDate)
                    weeksBetween % interval == 0L
                } else {
                    false
                }
            }
            "CUSTOM_DAYS" -> {
                val mask = rule.weekdaysMask.split(",").map { it.trim().uppercase() }
                mask.contains(targetDate.dayOfWeek.name)
            }
            "MONTHLY" -> {
                if (targetDate.dayOfMonth == startDate.dayOfMonth) {
                    val monthsBetween = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), targetDate.withDayOfMonth(1))
                    monthsBetween % interval == 0L
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * Resolves all occurrences for a specific date given rules, template items, exceptions, and overrides.
     */
    fun resolveOccurrencesForDate(
        targetDate: LocalDate,
        rules: List<RecurrenceRule>,
        templateEvents: List<EventItem>,
        exceptions: List<RecurrenceException>,
        overrideEventsMap: Map<String, EventItem>
    ): List<EventItem> {
        val dateString = targetDate.toString()
        val templateByRuleId = templateEvents.associateBy { it.recurrenceRuleId }
        val exceptionsByRuleAndDate = exceptions.groupBy { "${it.recurrenceRuleId}_${it.originalOccurrenceDate}" }

        val result = mutableListOf<EventItem>()

        for (rule in rules) {
            if (!matchesDate(rule, targetDate)) continue

            val key = "${rule.id}_$dateString"
            val matchingExceptions = exceptionsByRuleAndDate[key]

            if (matchingExceptions != null && matchingExceptions.isNotEmpty()) {
                val exception = matchingExceptions.first()
                if (exception.action == "SKIPPED") {
                    // Occurrence is cancelled for this date
                    continue
                } else if (exception.action == "MODIFIED" && exception.overrideItemId != null) {
                    val override = overrideEventsMap[exception.overrideItemId]
                    if (override != null) {
                        result.add(override)
                    }
                    continue
                }
            }

            // Normal recurring occurrence
            val template = templateByRuleId[rule.id]
            if (template != null) {
                // Synthetic instance on target date
                result.add(
                    template.copy(
                        date = dateString
                    )
                )
            }
        }

        return result
    }
}
