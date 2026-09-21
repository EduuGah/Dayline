package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EventOccurrence
import com.example.ui.theme.DaylineSky
import com.example.ui.viewmodel.DaylineNavTab
import com.example.ui.viewmodel.DaylineViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: DaylineViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val occurrences by viewModel.timelineOccurrences.collectAsStateWithLifecycle()
    val weekOccurrences by viewModel.weekOccurrences.collectAsStateWithLifecycle()

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Mês, 1 = Semana
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val ptLocale = Locale("pt", "BR")

    // Map dates to event count
    val eventsByDate = remember(allEvents) {
        allEvents.groupBy { it.date ?: "" }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // View Mode Selector (Mês / Semana)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (viewMode == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { viewMode = 0 }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (viewMode == 0) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Visão Mensal",
                            fontSize = 12.sp,
                            fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (viewMode == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { viewMode = 1 }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarViewWeek,
                            contentDescription = null,
                            tint = if (viewMode == 1) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Visão Semanal",
                            fontSize = 12.sp,
                            fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (viewMode == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (viewMode == 0) {
                // VISÃO MENSAL
                // Month Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mês anterior"
                        )
                    }

                    val monthTitle = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", ptLocale))
                        .replaceFirstChar { it.uppercase() }

                    Text(
                        text = monthTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Próximo mês"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekday Headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Month Grid with density dots
                val firstDayOfMonth = currentYearMonth.atDay(1)
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = Monday ... 7 = Sunday
                val emptyCellsBefore = startDayOfWeek - 1
                val totalCells = ((emptyCellsBefore + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (weekIndex in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dayIndex in 0 until 7) {
                                val cellIndex = weekIndex * 7 + dayIndex
                                val dayNumber = cellIndex - emptyCellsBefore + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = currentYearMonth.atDay(dayNumber)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == LocalDate.now()
                                    val eventCount = eventsByDate[cellDate.toString()]?.size ?: 0

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) DaylineSky else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.selectDate(cellDate)
                                            }
                                            .testTag("cal_day_$dayNumber")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )

                                            // Density dots: 1 dot (1 event), 2 dots (2-3), 3 dots (4+)
                                            if (eventCount > 0) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val dotColor = if (isSelected) Color.White else DaylineSky
                                                val numDots = when {
                                                    eventCount >= 4 -> 3
                                                    eventCount >= 2 -> 2
                                                    else -> 1
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    repeat(numDots) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(3.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selected Day Summary Card & Blocks
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptLocale))
                                        .replaceFirstChar { it.uppercase() },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${occurrences.size} compromissos agendados",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.setTab(DaylineNavTab.TIMELINE)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("open_timeline_from_cal")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Abrir Linha do Tempo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (occurrences.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Nenhum compromisso marcado para este dia.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(occurrences) { occ ->
                                    val catColor = occ.category?.colorHex?.let { Color(it) } ?: DaylineSky
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${occ.startTime ?: "--:--"} - ${occ.endTime ?: "--:--"}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor,
                                                modifier = Modifier.width(86.dp)
                                            )
                                            Text(
                                                text = occ.event.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // VISÃO SEMANAL (7 dias da semana corrente)
                val startOfWeek = selectedDate.with(DayOfWeek.MONDAY)
                val weekDays: List<LocalDate> = (0L..6L).map { startOfWeek.plusDays(it) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Semana de ${startOfWeek.format(DateTimeFormatter.ofPattern("d 'de' MMMM", ptLocale))}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items = weekDays, key = { it.toString() }) { day: LocalDate ->
                        val dayOccs: List<EventOccurrence> = weekOccurrences[day] ?: emptyList()
                        val isDaySelected = day == selectedDate
                        val isDayToday = day == LocalDate.now()
                        val dayLabel = day.format(DateTimeFormatter.ofPattern("EEEE", ptLocale))
                            .replaceFirstChar { it.uppercase() }
                        val dayDateStr = day.format(DateTimeFormatter.ofPattern("d MMM", ptLocale))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDaySelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    if (isDaySelected) DaylineSky else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    viewModel.selectDate(day)
                                    viewModel.setTab(DaylineNavTab.TIMELINE)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dayLabel,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDayToday) DaylineSky else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• $dayDateStr",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isDayToday) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(DaylineSky)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("HOJE", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${dayOccs.size} compromissos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (dayOccs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        dayOccs.take(3).forEach { occ: EventOccurrence ->
                                            val catColor = occ.category?.colorHex?.let { Color(it) } ?: DaylineSky
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${occ.startTime ?: "--:--"}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catColor
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = occ.event.title,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        if (dayOccs.size > 3) {
                                            Text(
                                                text = "+ mais ${dayOccs.size - 3} itens...",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Dia livre",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
