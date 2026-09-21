package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Category
import com.example.model.EventItem
import com.example.model.EventOccurrence
import com.example.model.EventStatus
import com.example.ui.components.EventCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun event_card_screenshot() {
    val cat = Category("work", "Trabalho", 0xFF0284C7, "work")
    val event = EventItem(
        id = "test-1",
        type = EventItem.TYPE_EVENT,
        title = "Bloco de Foco Profundo",
        description = "Revisão técnica de arquitetura e implementação",
        date = "2026-09-21",
        startTime = "14:00",
        endTime = "16:00",
        categoryId = "work"
    )
    val occ = EventOccurrence(
        event = event,
        category = cat,
        occurrenceDate = LocalDate.of(2026, 9, 21),
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(16, 0),
        status = EventStatus.NOW,
        isRecurring = false,
        hasConflict = false,
        progressFraction = 0.5f,
        remainingMinutes = 60
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        EventCard(
            occurrence = occ,
            onToggleComplete = {},
            onClick = {},
            onEdit = {},
            onDuplicate = {},
            onDelete = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
