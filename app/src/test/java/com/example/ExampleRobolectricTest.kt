package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.RecurrenceEngine
import com.example.domain.TimelineCalculator
import com.example.model.Category
import com.example.model.EventItem
import com.example.model.EventStatus
import com.example.model.RecurrenceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dayline", appName)
    }

    @Test
    fun `recurrence engine resolves daily recurring event`() {
        val rule = RecurrenceRule(
            id = "rule-1",
            frequency = "DAILY",
            interval = 1,
            startsOnDate = "2026-09-01"
        )
        val template = EventItem(
            id = "evt-1",
            type = EventItem.TYPE_EVENT,
            title = "Treino Diário",
            date = "2026-09-01",
            startTime = "07:00",
            endTime = "08:00",
            recurrenceRuleId = "rule-1"
        )

        val targetDate = LocalDate.of(2026, 9, 21)
        val occurrences = RecurrenceEngine.resolveOccurrencesForDate(
            targetDate = targetDate,
            rules = listOf(rule),
            templateEvents = listOf(template),
            exceptions = emptyList(),
            overrideEventsMap = emptyMap()
        )

        assertEquals(1, occurrences.size)
        assertEquals("Treino Diário", occurrences[0].title)
        assertEquals("2026-09-21", occurrences[0].date)
    }

    @Test
    fun `timeline calculator builds occurrences and detects status`() {
        val cat = Category("cat_1", "Trabalho", 0xFF0284C7, "work")
        val event = EventItem(
            id = "evt-work",
            type = EventItem.TYPE_EVENT,
            title = "Reunião de Alinhamento",
            date = "2026-09-21",
            startTime = "10:00",
            endTime = "11:30",
            categoryId = "cat_1"
        )

        val targetDate = LocalDate.of(2026, 9, 21)
        val currentTime = LocalTime.of(10, 30)

        val occurrences = TimelineCalculator.buildOccurrences(
            targetDate = targetDate,
            currentDate = targetDate,
            currentTime = currentTime,
            events = listOf(event),
            categoriesMap = mapOf("cat_1" to cat)
        )

        assertEquals(1, occurrences.size)
        val occ = occurrences[0]
        assertEquals(EventStatus.NOW, occ.status)
        assertEquals(60L, occ.remainingMinutes) // 10:30 to 11:30 is 60 min

        val rows = TimelineCalculator.buildTimelineRows(occurrences)
        assertTrue(rows.isNotEmpty())
    }
}
