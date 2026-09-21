package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EventItem
import com.example.model.EventOccurrence
import com.example.model.EventStatus
import com.example.model.RecurrenceRule
import com.example.model.RecurrenceScope
import com.example.model.TimelineRow
import com.example.ui.components.AddEditEventSheet
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DateHeaderBar
import com.example.ui.components.EventCard
import com.example.ui.components.FreeIntervalCard
import com.example.ui.components.NowIndicator
import com.example.ui.components.RecurrenceScopeDialog
import com.example.ui.components.RoutineTemplatesDialog
import com.example.ui.theme.DaylineSky
import com.example.ui.viewmodel.DaylineViewModel
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: DaylineViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentRealDate by viewModel.currentRealDate.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val daySummary by viewModel.daySummary.collectAsStateWithLifecycle()
    val timelineRows by viewModel.timelineRows.collectAsStateWithLifecycle()
    val occurrences by viewModel.timelineOccurrences.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val inboxTasks by viewModel.unscheduledTasks.collectAsStateWithLifecycle()
    val postponeConflict by viewModel.postponeConflict.collectAsStateWithLifecycle()
    val pendingActionIds by viewModel.pendingActionIds.collectAsStateWithLifecycle()
    val activeUndoAction by viewModel.activeUndoAction.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(activeUndoAction) {
        val action = activeUndoAction
        if (action != null) {
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "Desfazer",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.triggerUndo()
            } else {
                viewModel.dismissUndo()
            }
        }
    }

    val isToday = selectedDate == currentRealDate

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }
    var slotStartTime by remember { mutableStateOf<String?>(null) }
    var slotEndTime by remember { mutableStateOf<String?>(null) }

    var showTemplatesDialog by remember { mutableStateOf(false) }
    var selectedFreeInterval by remember { mutableStateOf<com.example.model.FreeInterval?>(null) }

    // Recurring Scope Dialogs
    var pendingRecurringEdit by remember { mutableStateOf<Pair<EventItem, EventItem>?>(null) }
    var pendingRecurringDelete by remember { mutableStateOf<EventItem?>(null) }

    Scaffold(
        topBar = {
            Column {
                DateHeaderBar(
                    selectedDate = selectedDate,
                    isToday = isToday,
                    daySummary = daySummary,
                    onPreviousDay = { viewModel.previousDay() },
                    onNextDay = { viewModel.nextDay() },
                    onGoToToday = { viewModel.goToToday() },
                    onOpenTemplates = { showTemplatesDialog = true }
                )
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryFilter,
                    onSelectCategory = { viewModel.setCategoryFilter(it) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    slotStartTime = null
                    slotEndTime = null
                    showAddEditDialog = true
                },
                containerColor = DaylineSky,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_event_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Novo compromisso",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("global_snackbar_host")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (occurrences.isEmpty()) {
                // Empty state for the day
                EmptyTimelineView(
                    isToday = isToday,
                    onAddEvent = {
                        editingEvent = null
                        slotStartTime = null
                        slotEndTime = null
                        showAddEditDialog = true
                    },
                    onUseTemplate = { showTemplatesDialog = true }
                )
            } else {
                // Timeline List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("timeline_list")
                ) {
                    // If today and current time is before the first item
                    if (isToday) {
                        val firstItem = occurrences.firstOrNull()
                        if (firstItem?.startTime != null && currentTime.isBefore(firstItem.startTime)) {
                            item(key = "now_top") {
                                NowIndicator(currentTime = currentTime)
                            }
                        }
                    }

                    items(
                        items = timelineRows,
                        key = { row ->
                            when (row) {
                                is TimelineRow.EventRow -> "event_${row.occurrence.event.id}_${row.occurrence.occurrenceDate}"
                                is TimelineRow.FreeIntervalRow -> "free_${row.interval.startTime}_${row.interval.endTime}"
                            }
                        }
                    ) { row ->
                        when (row) {
                            is TimelineRow.EventRow -> {
                                val occ = row.occurrence

                                // If this occurrence is happening right now, show NowIndicator before it
                                if (isToday && occ.status == EventStatus.NOW) {
                                    NowIndicator(currentTime = currentTime)
                                }

                                EventCard(
                                    occurrence = occ,
                                    onToggleComplete = { viewModel.toggleComplete(occ) },
                                    onComplete = { viewModel.completeEvent(occ) },
                                    onReopen = { viewModel.reopenEvent(occ) },
                                    onClick = {
                                        editingEvent = occ.event
                                        showAddEditDialog = true
                                    },
                                    onEdit = {
                                        editingEvent = occ.event
                                        showAddEditDialog = true
                                    },
                                    onDuplicate = { viewModel.duplicateEvent(occ) },
                                    onDelete = {
                                        if (occ.isRecurring) {
                                            pendingRecurringDelete = occ.event
                                        } else {
                                            viewModel.deleteOccurrence(occ.event, selectedDate, null)
                                        }
                                    },
                                    onPostpone = { minutes ->
                                        viewModel.postponeEvent(occ, minutes)
                                    },
                                    isLocked = pendingActionIds.contains(occ.event.id)
                                )
                            }
                            is TimelineRow.FreeIntervalRow -> {
                                // If current time falls inside this free interval, render NowIndicator inside the interval
                                if (isToday && !currentTime.isBefore(row.interval.startTime) && currentTime.isBefore(row.interval.endTime)) {
                                    NowIndicator(currentTime = currentTime)
                                }

                                FreeIntervalCard(
                                    interval = row.interval,
                                    onClickInterval = { interval ->
                                        selectedFreeInterval = interval
                                    }
                                )
                            }
                        }
                    }

                    // If today and current time is after all items
                    if (isToday) {
                        val lastItem = occurrences.lastOrNull()
                        if (lastItem?.endTime != null && !currentTime.isBefore(lastItem.endTime)) {
                            item(key = "now_bottom") {
                                NowIndicator(currentTime = currentTime)
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Screen / Fullscreen Sheet
    if (showAddEditDialog) {
        AddEditEventSheet(
            initialEvent = editingEvent,
            targetDate = selectedDate,
            initialStartTime = slotStartTime,
            initialEndTime = slotEndTime,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { updated: EventItem, rule: RecurrenceRule? ->
                showAddEditDialog = false
                if (editingEvent != null && editingEvent?.recurrenceRuleId != null) {
                    // Asking scope for recurring item
                    pendingRecurringEdit = Pair(editingEvent!!, updated)
                } else {
                    viewModel.saveEvent(updated, rule)
                }
            },
            onDelete = editingEvent?.let { evt ->
                {
                    showAddEditDialog = false
                    if (evt.recurrenceRuleId != null) {
                        pendingRecurringDelete = evt
                    } else {
                        viewModel.deleteOccurrence(evt, selectedDate, null)
                    }
                }
            }
        )
    }

    // Routine Templates Dialog
    if (showTemplatesDialog) {
        RoutineTemplatesDialog(
            onSelectTemplate = { templateType ->
                showTemplatesDialog = false
                viewModel.applyTemplate(templateType)
            },
            onDismiss = { showTemplatesDialog = false }
        )
    }

    // Free Interval Bottom Sheet with Smart Inbox Slotting & Focus Blocks
    selectedFreeInterval?.let { interval ->
        com.example.ui.components.FreeIntervalBottomSheet(
            interval = interval,
            inboxTasks = inboxTasks,
            onDismiss = { selectedFreeInterval = null },
            onAddEventHere = { start, end ->
                selectedFreeInterval = null
                slotStartTime = start
                slotEndTime = end
                editingEvent = null
                showAddEditDialog = true
            },
            onCreateFocusBlock = { start, end ->
                selectedFreeInterval = null
                viewModel.createFocusBlock(selectedDate, start, end)
            },
            onScheduleInboxTask = { task, startTime ->
                selectedFreeInterval = null
                val timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                val startLt = com.example.domain.TimelineCalculator.parseTime(startTime) ?: java.time.LocalTime.of(12, 0)
                val endLt = startLt.plusMinutes(task.durationMinutes.toLong())
                viewModel.scheduleInboxTask(
                    task = task,
                    date = selectedDate,
                    startTime = startTime,
                    endTime = endLt.format(timeFmt)
                )
            }
        )
    }

    // Recurring Scope Dialog for Edit
    pendingRecurringEdit?.let { (original, updated) ->
        RecurrenceScopeDialog(
            title = "Editar série recorrente",
            onScopeSelected = { scope ->
                viewModel.editRecurringOccurrence(original, updated, selectedDate, scope)
                pendingRecurringEdit = null
            },
            onDismiss = { pendingRecurringEdit = null }
        )
    }

    // Recurring Scope Dialog for Delete
    pendingRecurringDelete?.let { eventToDelete ->
        RecurrenceScopeDialog(
            title = "Excluir compromisso recorrente",
            onScopeSelected = { scope ->
                viewModel.deleteOccurrence(eventToDelete, selectedDate, scope)
                pendingRecurringDelete = null
            },
            onDismiss = { pendingRecurringDelete = null }
        )
    }

    // Postpone Conflict Confirmation Dialog
    postponeConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPostponeConflict() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Conflito de horário",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Adiar \"${conflict.occurrence.event.title}\" para ${conflict.newStartTime} - ${conflict.newEndTime} " +
                        "vai sobrepor o compromisso \"${conflict.conflictingEventTitle}\" (${conflict.conflictingTime}).\n\n" +
                        "Deseja adiar mesmo assim ou ajustar manualmente para outro horário?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.postponeEvent(conflict.occurrence, conflict.minutesToAdd, force = true)
                        viewModel.dismissPostponeConflict()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Adiar mesmo assim", color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { viewModel.dismissPostponeConflict() }
                    ) {
                        Text("Cancelar")
                    }
                    TextButton(
                        onClick = {
                            val evt = conflict.occurrence.event
                            viewModel.dismissPostponeConflict()
                            editingEvent = evt
                            slotStartTime = conflict.newStartTime
                            slotEndTime = conflict.newEndTime
                            showAddEditDialog = true
                        }
                    ) {
                        Text("Ajustar horário")
                    }
                }
            }
        )
    }
}

@Composable
private fun EmptyTimelineView(
    isToday: Boolean,
    onAddEvent: () -> Unit,
    onUseTemplate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("empty_timeline_view")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DaylineSky.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = DaylineSky,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isToday) "Nenhum compromisso para hoje" else "Linha do tempo livre",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organize seus blocos de rotina, trabalho, estudos e descanso para acompanhar o dia em tempo real.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddEvent,
            colors = ButtonDefaults.buttonColors(containerColor = DaylineSky),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("empty_add_event_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Criar Primeiro Evento")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onUseTemplate,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .testTag("empty_template_btn")
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Inserir Rotina Pronta")
        }
    }
}
