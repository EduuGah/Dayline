package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DaylineRepository
import com.example.domain.RecurrenceEngine
import com.example.domain.TimelineCalculator
import com.example.model.Category
import com.example.model.CategoryTimeStat
import com.example.model.DaySummary
import com.example.model.EventItem
import com.example.model.EventOccurrence
import com.example.model.EventStatus
import com.example.model.RecurrenceRule
import com.example.model.RecurrenceScope
import com.example.model.TimelineRow
import com.example.service.NotificationScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class PostponeConflictInfo(
    val occurrence: EventOccurrence,
    val minutesToAdd: Long,
    val newStartTime: String,
    val newEndTime: String,
    val conflictingEventTitle: String,
    val conflictingTime: String
)

data class DaylineUndoAction(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val undo: suspend () -> Unit
)

enum class DaylineNavTab {
    TIMELINE,
    CALENDAR,
    INBOX,
    STATS,
    SETTINGS
}

class DaylineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DaylineRepository = DaylineRepository(
        AppDatabase.getInstance(application).daylineDao()
    )

    // Real-time ticking clock
    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    private val _currentRealDate = MutableStateFlow(LocalDate.now())
    val currentRealDate: StateFlow<LocalDate> = _currentRealDate.asStateFlow()

    // Selected View Date
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(DaylineNavTab.TIMELINE)
    val currentTab: StateFlow<DaylineNavTab> = _currentTab.asStateFlow()

    // Conflict detection for postpone actions
    private val _postponeConflict = MutableStateFlow<PostponeConflictInfo?>(null)
    val postponeConflict: StateFlow<PostponeConflictInfo?> = _postponeConflict.asStateFlow()

    fun dismissPostponeConflict() {
        _postponeConflict.value = null
    }

    // Global Undo system for quick actions (Concluir, Adiar, Excluir)
    private val _activeUndoAction = MutableStateFlow<DaylineUndoAction?>(null)
    val activeUndoAction: StateFlow<DaylineUndoAction?> = _activeUndoAction.asStateFlow()

    fun triggerUndo() {
        val action = _activeUndoAction.value ?: return
        _activeUndoAction.value = null
        viewModelScope.launch {
            action.undo()
        }
    }

    fun dismissUndo() {
        _activeUndoAction.value = null
    }

    // Filter
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
        }
        startClockTicker()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                _currentTime.value = LocalTime.now()
                _currentRealDate.value = LocalDate.now()
                delay(10_000) // Update every 10 seconds for real-time live indicator
            }
        }
    }

    // Base Database Flows
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unscheduledTasks: StateFlow<List<EventItem>> = repository.unscheduledTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<EventItem>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Timeline computation for the selected date
    val timelineOccurrences: StateFlow<List<EventOccurrence>> = combine(
        _selectedDate,
        _currentRealDate,
        _currentTime,
        repository.allEvents,
        repository.recurrenceRules,
        repository.recurrenceExceptions,
        repository.categories,
        _selectedCategoryFilter
    ) { args ->
        val selDate = args[0] as LocalDate
        val realDate = args[1] as LocalDate
        val timeNow = args[2] as LocalTime
        val allEvts = args[3] as List<EventItem>
        val rules = args[4] as List<RecurrenceRule>
        val exceptions = args[5] as List<com.example.model.RecurrenceException>
        val cats = args[6] as List<Category>
        val filterCat = args[7] as String?

        val selDateStr = selDate.toString()
        val catsMap = cats.associateBy { it.id }

        // Single standalone events on this date
        val singleEvents = allEvts.filter { it.date == selDateStr && it.recurrenceRuleId == null }

        // Recurring templates
        val templates = allEvts.filter { it.recurrenceRuleId != null }
        val overridesMap = allEvts.associateBy { it.id }

        // Resolve recurring events
        val recurringOccurrences = RecurrenceEngine.resolveOccurrencesForDate(
            targetDate = selDate,
            rules = rules,
            templateEvents = templates,
            exceptions = exceptions,
            overrideEventsMap = overridesMap
        )

        val combinedEvents = (singleEvents + recurringOccurrences).let { list ->
            if (filterCat != null) list.filter { it.categoryId == filterCat } else list
        }

        TimelineCalculator.buildOccurrences(
            targetDate = selDate,
            currentDate = realDate,
            currentTime = timeNow,
            events = combinedEvents,
            categoriesMap = catsMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rows for timeline with free intervals
    val timelineRows: StateFlow<List<TimelineRow>> = combine(
        timelineOccurrences
    ) { occurrencesArray ->
        val occurrences = occurrencesArray[0]
        TimelineCalculator.buildTimelineRows(occurrences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Day summary
    val daySummary: StateFlow<DaySummary> = combine(
        _selectedDate,
        _currentRealDate,
        _currentTime,
        timelineOccurrences
    ) { selDate, realDate, timeNow, occurrences ->
        TimelineCalculator.computeDaySummary(selDate, realDate, timeNow, occurrences)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DaySummary(date = LocalDate.now())
    )

    // Category stats for selected date
    val categoryStats: StateFlow<List<CategoryTimeStat>> = combine(
        timelineOccurrences,
        categories
    ) { occurrences, cats ->
        TimelineCalculator.computeCategoryStats(occurrences, cats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Week Occurrences (Monday to Sunday for selected date)
    val weekOccurrences: StateFlow<Map<LocalDate, List<EventOccurrence>>> = combine(
        _selectedDate,
        _currentRealDate,
        _currentTime,
        repository.allEvents,
        repository.recurrenceRules,
        repository.recurrenceExceptions,
        repository.categories
    ) { args ->
        val selDate = args[0] as LocalDate
        val realDate = args[1] as LocalDate
        val timeNow = args[2] as LocalTime
        val allEvts = args[3] as List<EventItem>
        val rules = args[4] as List<RecurrenceRule>
        val exceptions = args[5] as List<com.example.model.RecurrenceException>
        val cats = args[6] as List<Category>

        val catsMap = cats.associateBy { it.id }
        val overridesMap = allEvts.associateBy { it.id }
        val templates = allEvts.filter { it.recurrenceRuleId != null }

        // Determine Monday of current week
        val monday = selDate.minusDays((selDate.dayOfWeek.value - 1).toLong())
        val daysOfWeek = (0..6).map { monday.plusDays(it.toLong()) }

        val resultMap = mutableMapOf<LocalDate, List<EventOccurrence>>()
        for (day in daysOfWeek) {
            val dayStr = day.toString()
            val singleEvents = allEvts.filter { it.date == dayStr && it.recurrenceRuleId == null }
            val recurring = RecurrenceEngine.resolveOccurrencesForDate(
                targetDate = day,
                rules = rules,
                templateEvents = templates,
                exceptions = exceptions,
                overrideEventsMap = overridesMap
            )
            val combined = singleEvents + recurring
            resultMap[day] = TimelineCalculator.buildOccurrences(
                targetDate = day,
                currentDate = realDate,
                currentTime = timeNow,
                events = combined,
                categoriesMap = catsMap
            )
        }
        resultMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Week Summary (Weekly stats)
    val weekSummary: StateFlow<com.example.model.WeekSummary> = combine(
        _selectedDate,
        weekOccurrences
    ) { selDate: LocalDate, weekMap: Map<LocalDate, List<EventOccurrence>> ->
        val monday = selDate.minusDays((selDate.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val allOccs = weekMap.values.flatten()
        TimelineCalculator.computeWeekSummary(monday, sunday, allOccs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.example.model.WeekSummary(LocalDate.now(), LocalDate.now())
    )

    // Search results
    val searchResults: StateFlow<List<EventItem>> = combine(
        _searchQuery,
        allEvents
    ) { query, events ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            events.filter {
                it.title.lowercase().contains(q) ||
                        (it.description?.lowercase()?.contains(q) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User actions
    fun setTab(tab: DaylineNavTab) {
        _currentTab.value = tab
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Set of occurrence / event IDs currently undergoing asynchronous persistence
    private val _pendingActionIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingActionIds: StateFlow<Set<String>> = _pendingActionIds.asStateFlow()

    fun completeEvent(occurrence: EventOccurrence) {
        val event = occurrence.event
        val eventId = event.id
        if (_pendingActionIds.value.contains(eventId)) return

        viewModelScope.launch {
            _pendingActionIds.value = _pendingActionIds.value + eventId
            try {
                if (event.recurrenceRuleId != null) {
                    val overrideId = UUID.randomUUID().toString()
                    val overrideEvent = event.copy(
                        id = overrideId,
                        date = occurrence.occurrenceDate.toString(),
                        isCompleted = true,
                        completedAt = System.currentTimeMillis(),
                        recurrenceRuleId = null
                    )
                    repository.saveEvent(overrideEvent)

                    val exception = com.example.model.RecurrenceException(
                        id = UUID.randomUUID().toString(),
                        recurrenceRuleId = event.recurrenceRuleId,
                        originalOccurrenceDate = occurrence.occurrenceDate.toString(),
                        action = "MODIFIED",
                        overrideItemId = overrideId
                    )
                    val db = AppDatabase.getInstance(getApplication())
                    db.daylineDao().insertRecurrenceException(exception)
                } else {
                    val updated = event.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveEvent(updated)

                    _activeUndoAction.value = DaylineUndoAction(
                        message = "Concluído: \"${event.title}\"",
                        undo = {
                            repository.saveEvent(event)
                        }
                    )
                }
            } finally {
                _pendingActionIds.value = _pendingActionIds.value - eventId
            }
        }
    }

    fun reopenEvent(occurrence: EventOccurrence) {
        val event = occurrence.event
        val eventId = event.id
        if (_pendingActionIds.value.contains(eventId)) return

        viewModelScope.launch {
            _pendingActionIds.value = _pendingActionIds.value + eventId
            try {
                if (event.recurrenceRuleId != null) {
                    val overrideId = UUID.randomUUID().toString()
                    val overrideEvent = event.copy(
                        id = overrideId,
                        date = occurrence.occurrenceDate.toString(),
                        isCompleted = false,
                        completedAt = null,
                        recurrenceRuleId = null
                    )
                    repository.saveEvent(overrideEvent)

                    val exception = com.example.model.RecurrenceException(
                        id = UUID.randomUUID().toString(),
                        recurrenceRuleId = event.recurrenceRuleId,
                        originalOccurrenceDate = occurrence.occurrenceDate.toString(),
                        action = "MODIFIED",
                        overrideItemId = overrideId
                    )
                    val db = AppDatabase.getInstance(getApplication())
                    db.daylineDao().insertRecurrenceException(exception)
                } else {
                    val updated = event.copy(
                        isCompleted = false,
                        completedAt = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveEvent(updated)

                    _activeUndoAction.value = DaylineUndoAction(
                        message = "Reaberto: \"${event.title}\"",
                        undo = {
                            repository.saveEvent(event.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
                        }
                    )
                }
            } finally {
                _pendingActionIds.value = _pendingActionIds.value - eventId
            }
        }
    }

    fun toggleComplete(occurrence: EventOccurrence) {
        val isDone = occurrence.status == EventStatus.COMPLETED || occurrence.event.isCompleted
        if (isDone) {
            reopenEvent(occurrence)
        } else {
            completeEvent(occurrence)
        }
    }

    fun toggleTaskComplete(task: EventItem) {
        viewModelScope.launch {
            repository.toggleEventCompletion(task)
        }
    }

    fun saveEvent(event: EventItem, rule: RecurrenceRule? = null) {
        viewModelScope.launch {
            if (rule != null) {
                repository.createRecurringSeries(event, rule)
            } else {
                repository.saveEvent(event)
                val catName = categories.value.find { it.id == event.categoryId }?.name
                NotificationScheduler.scheduleReminder(getApplication(), event, catName)
            }
        }
    }

    fun editRecurringOccurrence(
        currentEvent: EventItem,
        updatedEvent: EventItem,
        targetDate: LocalDate,
        scope: RecurrenceScope
    ) {
        viewModelScope.launch {
            repository.editRecurringOccurrence(currentEvent, updatedEvent, targetDate, scope)
            if (updatedEvent.reminderMinutesBefore >= 0) {
                val catName = categories.value.find { it.id == updatedEvent.categoryId }?.name
                NotificationScheduler.scheduleReminder(getApplication(), updatedEvent, catName)
            }
        }
    }

    fun deleteOccurrence(
        currentEvent: EventItem,
        targetDate: LocalDate,
        scope: RecurrenceScope?
    ) {
        viewModelScope.launch {
            NotificationScheduler.cancelReminder(getApplication(), currentEvent.id)
            if (currentEvent.recurrenceRuleId != null && scope != null) {
                repository.deleteRecurringOccurrence(currentEvent, targetDate, scope)
            } else {
                repository.deleteEventById(currentEvent.id)
                _activeUndoAction.value = DaylineUndoAction(
                    message = "Excluído: \"${currentEvent.title}\"",
                    undo = {
                        repository.saveEvent(currentEvent)
                    }
                )
            }
        }
    }

    fun scheduleInboxTask(task: EventItem, date: LocalDate, startTime: String, endTime: String? = null) {
        viewModelScope.launch {
            val updated = task.copy(
                type = EventItem.TYPE_EVENT,
                date = date.toString(),
                startTime = startTime,
                endTime = endTime
            )
            repository.saveEvent(updated)
            val catName = categories.value.find { it.id == updated.categoryId }?.name
            NotificationScheduler.scheduleReminder(getApplication(), updated, catName)
        }
    }

    fun postponeEvent(occurrence: EventOccurrence, minutesToAdd: Long, force: Boolean = false) {
        val event = occurrence.event
        val eventId = event.id
        if (_pendingActionIds.value.contains(eventId)) return

        viewModelScope.launch {
            val start = occurrence.startTime?.plusMinutes(minutesToAdd) ?: return@launch
            val durationMins = if (occurrence.startTime != null && occurrence.endTime != null) {
                java.time.Duration.between(occurrence.startTime, occurrence.endTime).toMinutes().coerceAtLeast(10)
            } else {
                event.durationMinutes.toLong().coerceAtLeast(15)
            }
            val end = occurrence.endTime?.plusMinutes(minutesToAdd) ?: start.plusMinutes(durationMins)
            val timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

            if (!force) {
                val dayOccurrences = timelineOccurrences.value
                val conflictOcc = dayOccurrences.firstOrNull { other: EventOccurrence ->
                    other.event.id != occurrence.event.id &&
                    other.startTime != null && other.endTime != null &&
                    other.startTime < end && start < other.endTime
                }

                if (conflictOcc != null) {
                    _postponeConflict.value = PostponeConflictInfo(
                        occurrence = occurrence,
                        minutesToAdd = minutesToAdd,
                        newStartTime = start.format(timeFmt),
                        newEndTime = end.format(timeFmt),
                        conflictingEventTitle = conflictOcc.event.title,
                        conflictingTime = "${conflictOcc.startTime?.format(timeFmt)} - ${conflictOcc.endTime?.format(timeFmt)}"
                    )
                    return@launch
                }
            }

            _pendingActionIds.value = _pendingActionIds.value + eventId
            try {
                val updatedEvent = event.copy(
                    startTime = start.format(timeFmt),
                    endTime = end.format(timeFmt)
                )

                if (event.recurrenceRuleId != null) {
                    editRecurringOccurrence(event, updatedEvent, occurrence.occurrenceDate, RecurrenceScope.THIS_ONLY)
                } else {
                    repository.saveEvent(updatedEvent)
                    val catName = categories.value.find { it.id == updatedEvent.categoryId }?.name
                    NotificationScheduler.scheduleReminder(getApplication(), updatedEvent, catName)
                }

                _activeUndoAction.value = DaylineUndoAction(
                    message = "Adiado em +${minutesToAdd}m: \"${event.title}\"",
                    undo = {
                        repository.saveEvent(event)
                    }
                )
            } finally {
                _pendingActionIds.value = _pendingActionIds.value - eventId
            }
        }
    }

    fun createFocusBlock(date: LocalDate, startTime: String, endTime: String, title: String = "Bloco de Foco") {
        viewModelScope.launch {
            val workCat = categories.value.find {
                it.name.contains("Trabalho", ignoreCase = true) || it.name.contains("Estudos", ignoreCase = true)
            }
            val event = EventItem(
                id = UUID.randomUUID().toString(),
                type = EventItem.TYPE_EVENT,
                title = title,
                date = date.toString(),
                startTime = startTime,
                endTime = endTime,
                categoryId = workCat?.id ?: categories.value.firstOrNull()?.id
            )
            saveEvent(event)
        }
    }

    fun duplicateEvent(occurrence: EventOccurrence) {
        viewModelScope.launch {
            val original = occurrence.event
            val duplicate = original.copy(
                id = UUID.randomUUID().toString(),
                title = "${original.title} (Cópia)",
                date = _selectedDate.value.toString(),
                recurrenceRuleId = null,
                isCompleted = false,
                completedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveEvent(duplicate)
        }
    }

    fun applyTemplate(templateName: String) {
        viewModelScope.launch {
            repository.applyRoutineTemplate(templateName, _selectedDate.value)
        }
    }

    fun addCategory(name: String, colorHex: Long, iconName: String) {
        viewModelScope.launch {
            val newCat = Category(
                id = "cat_${UUID.randomUUID().toString().take(8)}",
                name = name,
                colorHex = colorHex,
                iconName = iconName
            )
            repository.saveCategory(newCat)
        }
    }
}
