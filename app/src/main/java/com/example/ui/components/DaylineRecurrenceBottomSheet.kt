package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecurrenceRule
import com.example.ui.theme.DaylineSky
import java.time.LocalDate
import java.util.UUID

/**
 * Mobile-first Recurrence BottomSheet with Progressive Disclosure:
 * Level 1: Standard presets ("Não se repete", "Todos os dias", "Dias úteis", "Fins de semana", "Semanal", "Personalizado").
 * Level 2: Custom weekday selector (S, T, Q, Q, S, S, D) and interval only if "Personalizado" is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaylineRecurrenceBottomSheet(
    currentRule: RecurrenceRule?,
    targetDate: LocalDate,
    onDismiss: () -> Unit,
    onRuleSelected: (RecurrenceRule?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var frequency by remember {
        mutableStateOf(currentRule?.frequency ?: "NONE")
    }
    var intervalWeeks by remember {
        mutableIntStateOf(currentRule?.interval ?: 1)
    }
    var selectedWeekdays by remember {
        mutableStateOf(
            currentRule?.weekdaysMask?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    val options = listOf(
        "NONE" to "Não se repete",
        "DAILY" to "Todos os dias",
        "WEEKDAYS" to "Dias úteis (Seg a Sex)",
        "WEEKENDS" to "Fins de semana (Sáb e Dom)",
        "WEEKLY" to "Semanal",
        "CUSTOM" to "Personalizado"
    )

    val weekdayChips = listOf(
        "MONDAY" to "S",
        "TUESDAY" to "T",
        "WEDNESDAY" to "Q",
        "THURSDAY" to "Q",
        "FRIDAY" to "S",
        "SATURDAY" to "S",
        "SUNDAY" to "D"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = DaylineSky,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Repetição",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (key, label) ->
                    val isSelected = frequency == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DaylineSky.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { frequency = key }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DaylineSky else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = DaylineSky,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Progressive Disclosure: Custom configuration when "CUSTOM" is active
            AnimatedVisibility(visible = frequency == "CUSTOM") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "Repetir nos dias:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdayChips.forEach { (dayKey, displayLetter) ->
                            val isDaySelected = selectedWeekdays.contains(dayKey)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDaySelected) DaylineSky else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable {
                                        selectedWeekdays = if (isDaySelected) {
                                            selectedWeekdays - dayKey
                                        } else {
                                            selectedWeekdays + dayKey
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayLetter,
                                    fontSize = 13.sp,
                                    fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDaySelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val rule = if (frequency == "NONE") {
                        null
                    } else {
                        RecurrenceRule(
                            id = currentRule?.id ?: UUID.randomUUID().toString(),
                            frequency = frequency,
                            interval = intervalWeeks,
                            weekdaysMask = if (frequency == "CUSTOM") selectedWeekdays.joinToString(",") else "",
                            startsOnDate = targetDate.toString()
                        )
                    }
                    onRuleSelected(rule)
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Confirmar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
