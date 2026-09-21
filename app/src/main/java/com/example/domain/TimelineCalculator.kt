package com.example.domain

import com.example.model.Category
import com.example.model.CategoryTimeStat
import com.example.model.DaySummary
import com.example.model.EventItem
import com.example.model.EventOccurrence
import com.example.model.EventStatus
import com.example.model.FreeInterval
import com.example.model.TimelineRow
import com.example.model.WeekSummary
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimelineCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseTime(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            LocalTime.parse(timeStr.trim(), timeFormatter)
        } catch (e: Exception) {
            try {
                LocalTime.parse(timeStr.trim())
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Builds occurrences, checks overlaps, computes statuses and progress.
     */
    fun buildOccurrences(
        targetDate: LocalDate,
        currentDate: LocalDate,
        currentTime: LocalTime,
        events: List<EventItem>,
        categoriesMap: Map<String, Category>
    ): List<EventOccurrence> {
        val timedEvents = events.filter { !it.startTime.isNullOrBlank() }

        val occurrencesWithTimes = timedEvents.mapNotNull { item ->
            val start = parseTime(item.startTime) ?: return@mapNotNull null
            val end = parseTime(item.endTime) ?: start.plusMinutes(item.durationMinutes.toLong().coerceAtLeast(10))
            val category = item.categoryId?.let { categoriesMap[it] }

            val status: EventStatus = when {
                item.isCompleted -> EventStatus.COMPLETED
                targetDate.isBefore(currentDate) -> {
                    if (item.type == EventItem.TYPE_TASK && !item.isCompleted) EventStatus.OVERDUE else EventStatus.PAST
                }
                targetDate.isAfter(currentDate) -> EventStatus.UPCOMING
                else -> {
                    // targetDate == currentDate
                    if (currentTime.isBefore(start)) {
                        EventStatus.UPCOMING
                    } else if (!currentTime.isBefore(start) && currentTime.isBefore(end)) {
                        EventStatus.NOW
                    } else {
                        if (item.type == EventItem.TYPE_TASK && !item.isCompleted) EventStatus.OVERDUE else EventStatus.PAST
                    }
                }
            }

            var progress = 0f
            var remainingMins: Long? = null

            if (status == EventStatus.NOW) {
                val totalMins = Duration.between(start, end).toMinutes().coerceAtLeast(1)
                val elapsedMins = Duration.between(start, currentTime).toMinutes().coerceAtLeast(0)
                progress = (elapsedMins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f)
                remainingMins = Duration.between(currentTime, end).toMinutes().coerceAtLeast(0)
            } else if (status == EventStatus.UPCOMING && targetDate == currentDate) {
                remainingMins = Duration.between(currentTime, start).toMinutes().coerceAtLeast(0)
            }

            EventOccurrence(
                event = item,
                occurrenceDate = targetDate,
                startTime = start,
                endTime = end,
                status = status,
                progressFraction = progress,
                remainingMinutes = remainingMins,
                category = category,
                isRecurring = item.recurrenceRuleId != null,
                hasConflict = false
            )
        }.sortedWith(compareBy({ it.startTime }, { it.endTime }))

        // Mark conflicts (overlapping items)
        val result = occurrencesWithTimes.mapIndexed { index, occurrence ->
            val hasConflict = occurrencesWithTimes.indices.any { otherIdx ->
                if (otherIdx == index) return@any false
                val other = occurrencesWithTimes[otherIdx]
                occurrence.startTime != null && occurrence.endTime != null &&
                        other.startTime != null && other.endTime != null &&
                        occurrence.startTime.isBefore(other.endTime) &&
                        other.startTime.isBefore(occurrence.endTime)
            }
            occurrence.copy(hasConflict = hasConflict)
        }

        return result
    }

    /**
     * Builds timeline rows including free intervals between events.
     */
    fun buildTimelineRows(occurrences: List<EventOccurrence>): List<TimelineRow> {
        if (occurrences.isEmpty()) return emptyList()

        val rows = mutableListOf<TimelineRow>()

        for (i in occurrences.indices) {
            val current = occurrences[i]
            rows.add(TimelineRow.EventRow(current))

            if (i < occurrences.size - 1) {
                val next = occurrences[i + 1]
                val currentEnd = current.endTime
                val nextStart = next.startTime

                if (currentEnd != null && nextStart != null && currentEnd.isBefore(nextStart)) {
                    val gapMinutes = Duration.between(currentEnd, nextStart).toMinutes()
                    if (gapMinutes >= 15) {
                        rows.add(
                            TimelineRow.FreeIntervalRow(
                                FreeInterval(
                                    startTime = currentEnd,
                                    endTime = nextStart,
                                    durationMinutes = gapMinutes
                                )
                            )
                        )
                    }
                }
            }
        }

        return rows
    }

    /**
     * Summarizes the day for top header & stats.
     */
    fun computeDaySummary(
        targetDate: LocalDate,
        currentDate: LocalDate,
        currentTime: LocalTime,
        occurrences: List<EventOccurrence>
    ): DaySummary {
        val total = occurrences.size
        val completed = occurrences.count { it.status == EventStatus.COMPLETED || it.event.isCompleted }
        val current = occurrences.find { it.status == EventStatus.NOW }
        val next = occurrences.find { it.status == EventStatus.UPCOMING }

        val minutesToNext = if (targetDate == currentDate && next?.startTime != null) {
            Duration.between(currentTime, next.startTime).toMinutes().coerceAtLeast(0)
        } else {
            null
        }

        var busyMinutes = 0L
        var realizedMinutes = 0L

        for (occ in occurrences) {
            if (occ.startTime != null && occ.endTime != null) {
                val itemMins = Duration.between(occ.startTime, occ.endTime).toMinutes().coerceAtLeast(0)
                busyMinutes += itemMins

                when (occ.status) {
                    EventStatus.COMPLETED -> realizedMinutes += itemMins
                    EventStatus.NOW -> {
                        val elapsed = Duration.between(occ.startTime, currentTime).toMinutes().coerceAtLeast(0)
                        realizedMinutes += elapsed.coerceAtMost(itemMins)
                    }
                    EventStatus.PAST -> realizedMinutes += itemMins
                    else -> {}
                }
            }
        }

        val totalDayMinutes = 16 * 60L // ~16 active waking hours (07:00 to 23:00)
        val freeMinutes = (totalDayMinutes - busyMinutes).coerceAtLeast(0)

        return DaySummary(
            date = targetDate,
            totalItems = total,
            completedItems = completed,
            currentOccurrence = current,
            nextOccurrence = next,
            minutesToNext = minutesToNext,
            totalBusyMinutes = busyMinutes,
            realizedMinutes = realizedMinutes,
            totalFreeMinutes = freeMinutes
        )
    }

    /**
     * Computes weekly summary (RF-050)
     */
    fun computeWeekSummary(
        startOfWeek: LocalDate,
        endOfWeek: LocalDate,
        allOccurrencesInWeek: List<EventOccurrence>
    ): WeekSummary {
        var totalBusy = 0L
        var realized = 0L
        var completed = 0

        val dailyMinutes = mutableMapOf<LocalDate, Long>()

        for (occ in allOccurrencesInWeek) {
            if (occ.startTime != null && occ.endTime != null) {
                val mins = Duration.between(occ.startTime, occ.endTime).toMinutes().coerceAtLeast(0)
                totalBusy += mins
                dailyMinutes[occ.occurrenceDate] = (dailyMinutes[occ.occurrenceDate] ?: 0L) + mins
                if (occ.status == EventStatus.COMPLETED || occ.event.isCompleted) {
                    realized += mins
                    completed++
                } else if (occ.status == EventStatus.PAST) {
                    realized += mins
                }
            }
        }

        val totalItems = allOccurrencesInWeek.size
        val routineRate = if (totalItems > 0) (completed.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f) else 0f
        val punctualityRate = if (totalItems > 0) (realized.toFloat() / totalBusy.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) else 0f

        val totalFreeMinutes = (7L * 24L * 60L - totalBusy).coerceAtLeast(0L)
        val avgFreeMinutesPerDay = totalFreeMinutes / 7L

        val ptLocale = java.util.Locale("pt", "BR")
        val busiestEntry = dailyMinutes.maxByOrNull { it.value }
        val busiestDayName = busiestEntry?.key?.format(DateTimeFormatter.ofPattern("EEEE", ptLocale))
            ?.replaceFirstChar { it.uppercase() } ?: "Nenhum"
        val busiestDayMinutes = busiestEntry?.value ?: 0L

        return WeekSummary(
            startOfWeek = startOfWeek,
            endOfWeek = endOfWeek,
            totalBusyMinutes = totalBusy,
            realizedMinutes = realized,
            completedItems = completed,
            totalItems = totalItems,
            routineRate = routineRate,
            punctualityRate = punctualityRate,
            avgFreeMinutesPerDay = avgFreeMinutesPerDay,
            busiestDayName = busiestDayName,
            busiestDayMinutes = busiestDayMinutes
        )
    }

    /**
     * Computes time allocation across categories.
     */
    fun computeCategoryStats(
        occurrences: List<EventOccurrence>,
        categories: List<Category>
    ): List<CategoryTimeStat> {
        val catMap = categories.associateBy { it.id }
        val minutesPerCat = mutableMapOf<String, Long>()
        val countPerCat = mutableMapOf<String, Int>()

        var totalMinutes = 0L

        for (occ in occurrences) {
            val catId = occ.event.categoryId ?: "uncategorized"
            val duration = if (occ.startTime != null && occ.endTime != null) {
                Duration.between(occ.startTime, occ.endTime).toMinutes().coerceAtLeast(0)
            } else {
                occ.event.durationMinutes.toLong()
            }
            minutesPerCat[catId] = (minutesPerCat[catId] ?: 0L) + duration
            countPerCat[catId] = (countPerCat[catId] ?: 0) + 1
            totalMinutes += duration
        }

        if (totalMinutes == 0L) return emptyList()

        return minutesPerCat.mapNotNull { (catId, mins) ->
            val cat = catMap[catId] ?: Category(
                id = catId,
                name = if (catId == "uncategorized") "Geral" else "Outros",
                colorHex = 0xFF64748B,
                iconName = "folder"
            )
            CategoryTimeStat(
                category = cat,
                totalMinutes = mins,
                count = countPerCat[catId] ?: 1,
                percentage = (mins.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.totalMinutes }
    }
}
