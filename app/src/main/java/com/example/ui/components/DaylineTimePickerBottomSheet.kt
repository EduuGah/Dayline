package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.ui.theme.DaylineSky
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Modern tactile mobile-first Time Picker Bottom Sheet.
 * Features:
 * - Vertical tactile wheel picker for Hours (00-23) and Minutes (5-min intervals or 1-min switch)
 * - Quick shortcuts: "Agora", "+15 min", "+30 min", "+1h"
 * - Clean Material 3 styling, spacious touch targets (48dp+)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaylineTimePickerBottomSheet(
    title: String = "Escolher horário",
    initialTime: String? = null,
    minuteStep: Int = 5,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parsedInitial = remember(initialTime) {
        try {
            if (!initialTime.isNullOrBlank()) LocalTime.parse(initialTime) else LocalTime.now().withSecond(0)
        } catch (e: Exception) {
            LocalTime.now().withSecond(0)
        }
    }

    var selectedHour by remember { mutableIntStateOf(parsedInitial.hour) }
    var selectedMinute by remember { mutableIntStateOf(parsedInitial.minute) }
    var useFinePrecision: Boolean by remember { mutableStateOf<Boolean>(minuteStep == 1 || (parsedInitial.minute % 5 != 0)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("time_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = DaylineSky,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Precision toggle (5m vs 1m)
                TextButton(
                    onClick = { useFinePrecision = !useFinePrecision },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (useFinePrecision) "Passo 1 min" else "Passo 5 min",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DaylineSky
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wheel Pickers Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                // Center highlight indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DaylineSky.copy(alpha = 0.15f))
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hours Wheel
                    WheelColumn(
                        items = (0..23).toList(),
                        selectedItem = selectedHour,
                        onItemSelected = { selectedHour = it },
                        formatItem = { String.format("%02d", it) },
                        modifier = Modifier.width(72.dp)
                    )

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minutes Wheel
                    val minuteList = remember(useFinePrecision) {
                        if (useFinePrecision) (0..59).toList() else (0..55 step 5).toList()
                    }

                    val normalizedMinute = remember(selectedMinute, useFinePrecision) {
                        if (!useFinePrecision && selectedMinute % 5 != 0) {
                            ((selectedMinute / 5) * 5)
                        } else {
                            selectedMinute
                        }
                    }

                    WheelColumn(
                        items = minuteList,
                        selectedItem = if (minuteList.contains(normalizedMinute)) normalizedMinute else minuteList.first(),
                        onItemSelected = { selectedMinute = it },
                        formatItem = { String.format("%02d", it) },
                        modifier = Modifier.width(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contextual Shortcuts
            Text(
                text = "Atalhos rápidos",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Shortcut: Agora
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    onClick = {
                        val now = LocalTime.now()
                        selectedHour = now.hour
                        selectedMinute = if (useFinePrecision) now.minute else ((now.minute / 5) * 5)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Agora",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                // Shortcut: +15 min
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    onClick = {
                        val current = LocalTime.of(selectedHour, selectedMinute).plusMinutes(15)
                        selectedHour = current.hour
                        selectedMinute = current.minute
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+15m",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                // Shortcut: +30 min
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    onClick = {
                        val current = LocalTime.of(selectedHour, selectedMinute).plusMinutes(30)
                        selectedHour = current.hour
                        selectedMinute = current.minute
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+30m",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                // Shortcut: +1 hora
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    onClick = {
                        val current = LocalTime.of(selectedHour, selectedMinute).plusHours(1)
                        selectedHour = current.hour
                        selectedMinute = current.minute
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+1h",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
                        onConfirm(formatted)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("confirm_time_picker")
                ) {
                    Text(
                        text = "Confirmar ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tactile wheel column with snap behavior.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> WheelColumn(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    formatItem: (T) -> String,
    modifier: Modifier = Modifier
) {
    val itemHeight = 44.dp
    val initialIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Center index observation
    val centerIndex by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialIndex
            else {
                val centerOffset = listState.layoutInfo.viewportStartOffset +
                    (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    Math.abs(itemCenter - centerOffset)
                }
                closest?.index ?: initialIndex
            }
        }
    }

    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices) {
            onItemSelected(items[centerIndex])
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = 68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(180.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isSelected = index == centerIndex

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
            ) {
                Text(
                    text = formatItem(item),
                    fontSize = if (isSelected) 24.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
