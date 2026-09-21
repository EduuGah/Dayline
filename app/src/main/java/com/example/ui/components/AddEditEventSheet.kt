package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Category
import com.example.model.EventItem
import com.example.model.RecurrenceRule
import com.example.ui.theme.DaylineError
import com.example.ui.theme.DaylineSky
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Mobile-first dedicated Fullscreen Screen for Creating and Editing events/routines.
 *
 * Distinct Architecture & Craft principles:
 * 1. Dedicated Fullscreen with clean TopAppBar ("←" Back vs "×" Discard).
 * 2. Android Back handling with unsaved changes confirmation dialog.
 * 3. Primary interaction model is "Start + Duration" instead of calculating start & end times.
 * 4. Automatic smart duration preservation: shifting start preserves total duration.
 * 5. Symmetrical time stepping (-15, +15, -30, +30) and tactile BottomSheets for Time, Date, Duration, Recurrence, and Reminders.
 * 6. Progressive disclosure: Advanced options (Recurrence, Reminders, Notes) live in clean rows with bottom sheets.
 * 7. Distinct Create vs Edit views: Edit spotlights current status, timing and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEventSheet(
    initialEvent: EventItem? = null,
    targetDate: LocalDate = LocalDate.now(),
    initialStartTime: String? = null,
    initialEndTime: String? = null,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (event: EventItem, rule: RecurrenceRule?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = initialEvent != null

    // Title & Description
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var isTaskType by remember { mutableStateOf(initialEvent?.type == EventItem.TYPE_TASK) }

    // Date
    var selectedDate by remember {
        mutableStateOf(
            initialEvent?.date?.let {
                try { LocalDate.parse(it) } catch (e: Exception) { targetDate }
            } ?: targetDate
        )
    }

    // Default start time: 15 minutes in the future rounded to 5 mins
    val defaultStart = remember {
        val now = LocalTime.now()
        val roundedMins = ((now.minute + 4) / 5 * 5) % 60
        now.withMinute(roundedMins).withSecond(0).format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    var startTime by remember {
        mutableStateOf(initialEvent?.startTime ?: initialStartTime ?: defaultStart)
    }

    // Duration in minutes (core concept)
    var durationMinutes by remember {
        val initialMins = if (initialEvent?.startTime != null && initialEvent.endTime != null) {
            try {
                val s = LocalTime.parse(initialEvent.startTime)
                val e = LocalTime.parse(initialEvent.endTime)
                Duration.between(s, e).toMinutes().toInt().coerceAtLeast(15)
            } catch (e: Exception) {
                initialEvent.durationMinutes.coerceAtLeast(15)
            }
        } else if (initialStartTime != null && initialEndTime != null) {
            try {
                val s = LocalTime.parse(initialStartTime)
                val e = LocalTime.parse(initialEndTime)
                Duration.between(s, e).toMinutes().toInt().coerceAtLeast(15)
            } catch (e: Exception) {
                30
            }
        } else {
            initialEvent?.durationMinutes?.coerceAtLeast(15) ?: 30
        }
        mutableIntStateOf(initialMins)
    }

    // Rule: Preserve duration on start time adjustment
    var preserveDurationOnStartChange by remember { mutableStateOf(true) }

    // Category
    var selectedCategoryId by remember {
        mutableStateOf(initialEvent?.categoryId ?: categories.firstOrNull()?.id)
    }

    // Recurrence Rule
    var recurrenceRule by remember { mutableStateOf<RecurrenceRule?>(null) }

    // Reminder
    var reminderMinutes by remember { mutableIntStateOf(initialEvent?.reminderMinutesBefore ?: 10) }

    // Bottom sheet visibility states
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showRecurrencePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }

    // Discard Confirmation Dialog state
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Check if any modification was made
    val hasUnsavedChanges by remember {
        derivedStateOf {
            val titleChanged = title != (initialEvent?.title ?: "")
            val descChanged = description != (initialEvent?.description ?: "")
            val startChanged = startTime != (initialEvent?.startTime ?: initialStartTime ?: defaultStart)
            val catChanged = selectedCategoryId != (initialEvent?.categoryId ?: categories.firstOrNull()?.id)
            titleChanged || descChanged || startChanged || catChanged || recurrenceRule != null
        }
    }

    // Calculated End Time from Start + Duration
    val computedEndTime by remember {
        derivedStateOf {
            try {
                val s = LocalTime.parse(startTime)
                s.plusMinutes(durationMinutes.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                "15:00"
            }
        }
    }

    val attemptDismiss = {
        if (hasUnsavedChanges) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
        }
    }

    // Intercept Android hardware/gesture back
    BackHandler {
        if (showTimePickerForStart || showTimePickerForEnd || showDatePicker ||
            showDurationPicker || showRecurrencePicker || showReminderPicker) {
            showTimePickerForStart = false
            showTimePickerForEnd = false
            showDatePicker = false
            showDurationPicker = false
            showRecurrencePicker = false
            showReminderPicker = false
        } else if (hasUnsavedChanges) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
        }
    }

    // Fullscreen Dialog wrapping Scaffold
    Dialog(
        onDismissRequest = attemptDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) "Editar compromisso" else "Novo compromisso",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = attemptDismiss) {
                            Icon(
                                imageVector = if (isEditMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                                contentDescription = if (isEditMode) "Voltar" else "Fechar"
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val event = EventItem(
                                        id = initialEvent?.id ?: UUID.randomUUID().toString(),
                                        type = if (isTaskType) EventItem.TYPE_TASK else EventItem.TYPE_EVENT,
                                        title = title.trim(),
                                        description = description.trim().ifBlank { null },
                                        date = if (isTaskType) null else selectedDate.toString(),
                                        startTime = if (isTaskType) null else startTime,
                                        endTime = if (isTaskType) null else computedEndTime,
                                        durationMinutes = durationMinutes,
                                        categoryId = selectedCategoryId,
                                        priority = initialEvent?.priority ?: EventItem.PRIORITY_NORMAL,
                                        reminderMinutesBefore = reminderMinutes,
                                        isCompleted = initialEvent?.isCompleted ?: false,
                                        completedAt = initialEvent?.completedAt,
                                        recurrenceRuleId = initialEvent?.recurrenceRuleId
                                    )
                                    onSave(event, recurrenceRule)
                                }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.testTag("top_bar_save_action")
                        ) {
                            Text(
                                text = if (isEditMode) "Salvar" else "Criar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (title.isNotBlank()) DaylineSky else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Event Type Switcher (Event with time vs Inbox task without time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isTaskType) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isTaskType = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Compromisso na Timeline",
                            fontSize = 13.sp,
                            fontWeight = if (!isTaskType) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isTaskType) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTaskType) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isTaskType = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tarefa Inbox (sem horário)",
                            fontSize = 13.sp,
                            fontWeight = if (isTaskType) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTaskType) DaylineSky else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title Input
                Text(
                    text = "O QUE VOCÊ VAI FAZER?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: Reunião de alinhamento, Treino...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DaylineSky,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                if (!isTaskType) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // WHEN & DURATION SECTION (Core innovation)
                    Text(
                        text = "QUANDO & DURAÇÃO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date & Time Spotlight Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Date Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = DaylineSky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (selectedDate == LocalDate.now()) "Hoje" else if (selectedDate == LocalDate.now().plusDays(1)) "Amanhã" else selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )

                            // Start Time Row with Tactile Stepping and Picker Trigger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Início",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = startTime,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DaylineSky,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showTimePickerForStart = true }
                                            .testTag("start_time_display")
                                    )
                                }

                                // Quick Symmetrical Steppers (-30m, -15m, +15m, +30m)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    StepButton("-30m") {
                                        try {
                                            val t = LocalTime.parse(startTime).minusMinutes(30)
                                            startTime = t.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } catch (e: Exception) {}
                                    }
                                    StepButton("-15m") {
                                        try {
                                            val t = LocalTime.parse(startTime).minusMinutes(15)
                                            startTime = t.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } catch (e: Exception) {}
                                    }
                                    StepButton("+15m") {
                                        try {
                                            val t = LocalTime.parse(startTime).plusMinutes(15)
                                            startTime = t.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } catch (e: Exception) {}
                                    }
                                    StepButton("+30m") {
                                        try {
                                            val t = LocalTime.parse(startTime).plusMinutes(30)
                                            startTime = t.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        } catch (e: Exception) {}
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )

                            // Duration Chips Selector (15m, 30m, 45m, 1h, 1h30, 2h)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Duração",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Término: $computedEndTime",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 90 to "1h30").forEach { (mins, label) ->
                                        val isSelected = durationMinutes == mins
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) DaylineSky else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { durationMinutes = mins }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // More durations sheet trigger
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (durationMinutes !in listOf(15, 30, 45, 60, 90)) DaylineSky else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showDurationPicker = true }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (durationMinutes !in listOf(15, 30, 45, 60, 90)) "${durationMinutes}m" else "+",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (durationMinutes !in listOf(15, 30, 45, 60, 90)) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CATEGORY SECTION
                Text(
                    text = "CATEGORIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.take(4).forEach { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCategoryId = cat.id }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // PROGRESSIVE DISCLOSURE: ADVANCED OPTIONS
                Text(
                    text = "CONFIGURAÇÕES ADICIONAIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        // Recurrence row
                        if (initialEvent?.recurrenceRuleId == null) {
                            SettingRow(
                                icon = Icons.Default.Repeat,
                                title = "Repetição",
                                value = recurrenceRule?.frequency?.let {
                                    when (it) {
                                        "DAILY" -> "Todos os dias"
                                        "WEEKDAYS" -> "Dias úteis"
                                        "WEEKENDS" -> "Fins de semana"
                                        "WEEKLY" -> "Semanal"
                                        "CUSTOM" -> "Personalizado"
                                        else -> "Não se repete"
                                    }
                                } ?: "Não se repete",
                                onClick = { showRecurrencePicker = true }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            )
                        }

                        // Reminder row
                        SettingRow(
                            icon = Icons.Default.Notifications,
                            title = "Lembrete",
                            value = when (reminderMinutes) {
                                -1 -> "Sem lembrete"
                                0 -> "No horário"
                                5 -> "5 min antes"
                                10 -> "10 min antes"
                                15 -> "15 min antes"
                                30 -> "30 min antes"
                                60 -> "1 hora antes"
                                else -> "$reminderMinutes min antes"
                            },
                            onClick = { showReminderPicker = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )

                        // Notes row with inline expandable field
                        var showNotesInput by remember { mutableStateOf(description.isNotBlank()) }
                        if (!showNotesInput) {
                            SettingRow(
                                icon = Icons.Default.Edit,
                                title = "Notas",
                                value = if (description.isNotBlank()) description.take(20) + "..." else "Adicionar notas...",
                                onClick = { showNotesInput = true }
                            )
                        } else {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "Notas / Detalhes",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    placeholder = { Text("Instruções, links ou pauta...") },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (isEditMode && onDelete != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirmation = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DaylineError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("delete_event_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Excluir compromisso", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // --- SECONDARY BOTTOM SHEETS ---

    // 1. TimePicker for Start Time
    if (showTimePickerForStart) {
        DaylineTimePickerBottomSheet(
            initialTime = startTime,
            title = "Horário de início",
            onDismiss = { showTimePickerForStart = false },
            onConfirm = { newStart ->
                startTime = newStart
            }
        )
    }

    // 2. DatePicker
    if (showDatePicker) {
        DaylineDatePickerBottomSheet(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                selectedDate = newDate
            }
        )
    }

    // 3. Duration Sheet
    if (showDurationPicker) {
        DaylineDurationBottomSheet(
            currentDurationMinutes = durationMinutes,
            onDismiss = { showDurationPicker = false },
            onDurationSelected = { newDuration ->
                durationMinutes = newDuration
            }
        )
    }

    // 4. Recurrence Sheet
    if (showRecurrencePicker) {
        DaylineRecurrenceBottomSheet(
            currentRule = recurrenceRule,
            targetDate = selectedDate,
            onDismiss = { showRecurrencePicker = false },
            onRuleSelected = { rule ->
                recurrenceRule = rule
            }
        )
    }

    // 5. Reminder Sheet
    if (showReminderPicker) {
        DaylineReminderBottomSheet(
            currentReminderMinutes = reminderMinutes,
            onDismiss = { showReminderPicker = false },
            onReminderSelected = { mins ->
                reminderMinutes = mins
            }
        )
    }

    // --- CONFIRMATION DIALOGS ---

    // Discard Unsaved Changes Confirmation
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = {
                Text(text = "Descartar alterações?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Você fez mudanças neste compromisso. Se sair agora, elas serão perdidas.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DaylineError)
                ) {
                    Text("Descartar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Continuar editando")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(text = "Excluir compromisso?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Tem certeza que deseja excluir \"${title.ifBlank { "este compromisso" }}\"?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DaylineError)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DaylineSky,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
