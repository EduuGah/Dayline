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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EventItem
import com.example.ui.theme.DaylineError
import com.example.ui.theme.DaylineSky
import com.example.ui.theme.DaylineSuccess
import com.example.ui.viewmodel.DaylineViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun InboxScreen(
    viewModel: DaylineViewModel,
    modifier: Modifier = Modifier
) {
    val unscheduledTasks by viewModel.unscheduledTasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var newTaskTitle by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedDurationMinutes by remember { mutableIntStateOf(30) }

    // Schedule Task Dialog
    var schedulingTask by remember { mutableStateOf<EventItem?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Inbox de Tarefas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tarefas pendentes que precisam de um encaixe no seu dia. Defina a duração estimada e agende nos espaços livres.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Add Input Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            placeholder = { Text("Ex: Fazer exercício de JavaScript...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inbox_quick_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (newTaskTitle.isNotBlank()) {
                                    val task = EventItem(
                                        id = UUID.randomUUID().toString(),
                                        type = EventItem.TYPE_TASK,
                                        title = newTaskTitle.trim(),
                                        date = null,
                                        durationMinutes = selectedDurationMinutes,
                                        categoryId = selectedCategoryId ?: categories.firstOrNull()?.id
                                    )
                                    viewModel.saveEvent(task)
                                    newTaskTitle = ""
                                }
                            },
                            enabled = newTaskTitle.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
                            modifier = Modifier.testTag("inbox_quick_add_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Duration selector chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Duração:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        listOf(15, 30, 45, 60).forEach { mins ->
                            val isSelected = selectedDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) DaylineSky else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { selectedDurationMinutes = mins }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "~${mins}m",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Categoria:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        categories.take(4).forEach { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val catColor = Color(cat.colorHex)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        selectedCategoryId = if (isSelected) null else cat.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
            if (unscheduledTasks.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Inbox vazio!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Todas as suas tarefas foram encaixadas na timeline.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "${unscheduledTasks.size} afazeres a encaixar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inbox_tasks_list")
                ) {
                    items(unscheduledTasks, key = { it.id }) { task ->
                        val isDone = task.isCompleted
                        val cat = categories.find { it.id == task.categoryId }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Complete toggle
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDone) DaylineSuccess else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { viewModel.toggleTaskComplete(task) }
                                            .testTag("inbox_toggle_${task.id}")
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Duration pill
                                            Text(
                                                text = "~${task.durationMinutes} min",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DaylineSky
                                            )

                                            cat?.let {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "• ${it.name}",
                                                    fontSize = 11.sp,
                                                    color = Color(it.colorHex),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Schedule button
                                    Button(
                                        onClick = { schedulingTask = task },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ),
                                        modifier = Modifier.testTag("schedule_task_btn_${task.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Encaixar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deleteOccurrence(task, selectedDate, null) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = DaylineError.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
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

    // Tactile BottomSheet to Schedule Task into Timeline
    schedulingTask?.let { task ->
        val defaultTime = remember {
            LocalTime.now().plusHours(1).withMinute(0).format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        com.example.ui.components.DaylineTimePickerBottomSheet(
            initialTime = defaultTime,
            title = "Encaixar \"${task.title}\"",
            onDismiss = { schedulingTask = null },
            onConfirm = { targetHour ->
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
                val startLt = com.example.domain.TimelineCalculator.parseTime(targetHour) ?: LocalTime.now()
                val endLt = startLt.plusMinutes(task.durationMinutes.toLong())

                viewModel.scheduleInboxTask(
                    task = task,
                    date = selectedDate,
                    startTime = targetHour,
                    endTime = endLt.format(timeFmt)
                )
                schedulingTask = null
            }
        )
    }
}
