package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.DaylineSky
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Mobile-first Date Picker BottomSheet with contextual quick jumps:
 * - "Hoje"
 * - "Amanhã"
 * - "Próxima segunda"
 * - Compact visual month calendar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaylineDatePickerBottomSheet(
    initialDate: LocalDate,
    title: String = "Escolher data",
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    val ptBr = Locale("pt", "BR")
    val monthTitle = remember(displayedMonth) {
        val monthName = displayedMonth.month.getDisplayName(TextStyle.FULL, ptBr)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBr) else it.toString() }
        "$monthName ${displayedMonth.year}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("dayline_date_picker_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contextual Shortcuts: Hoje, Amanhã, Próxima segunda
            val today = remember { LocalDate.now() }
            val tomorrow = remember { today.plusDays(1) }
            val nextMonday = remember { today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateShortcutPill(
                    label = "Hoje",
                    isSelected = selectedDate == today,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDate = today
                        displayedMonth = YearMonth.from(today)
                    }
                )
                DateShortcutPill(
                    label = "Amanhã",
                    isSelected = selectedDate == tomorrow,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDate = tomorrow
                        displayedMonth = YearMonth.from(tomorrow)
                    }
                )
                DateShortcutPill(
                    label = "Próx. Seg",
                    isSelected = selectedDate == nextMonday,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedDate = nextMonday
                        displayedMonth = YearMonth.from(nextMonday)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Month Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Mês anterior",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = monthTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo mês",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekday labels: D S T Q Q S S
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days grid for the current month
            val daysInMonth = remember(displayedMonth) {
                val firstDayOfMonth = displayedMonth.atDay(1)
                // Sunday = 7 in DayOfWeek, convert to 0 for Sunday
                val firstDayOffset = if (firstDayOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfMonth.dayOfWeek.value
                val totalDays = displayedMonth.lengthOfMonth()

                val list = mutableListOf<LocalDate?>()
                for (i in 0 until firstDayOffset) {
                    list.add(null)
                }
                for (d in 1..totalDays) {
                    list.add(displayedMonth.atDay(d))
                }
                list
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(daysInMonth.size) { index ->
                    val date = daysInMonth[index]
                    if (date != null) {
                        val isSelected = date == selectedDate
                        val isTodayDate = date == today

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> DaylineSky
                                        isTodayDate -> DaylineSky.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    selectedDate = date
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isTodayDate) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> Color.White
                                    isTodayDate -> DaylineSky
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary CTA
            Button(
                onClick = {
                    onDateSelected(selectedDate)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_date_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DateShortcutPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) DaylineSky else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
