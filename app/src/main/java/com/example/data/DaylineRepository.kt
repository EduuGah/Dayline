package com.example.data

import com.example.model.Category
import com.example.model.EventItem
import com.example.model.RecurrenceException
import com.example.model.RecurrenceRule
import com.example.model.RecurrenceScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class DaylineRepository(private val dao: DaylineDao) {

    val allEvents: Flow<List<EventItem>> = dao.getAllEvents()
    val unscheduledTasks: Flow<List<EventItem>> = dao.getUnscheduledTasks()
    val categories: Flow<List<Category>> = dao.getAllCategories()
    val recurrenceRules: Flow<List<RecurrenceRule>> = dao.getAllRecurrenceRules()
    val recurrenceExceptions: Flow<List<RecurrenceException>> = dao.getAllRecurrenceExceptions()
    val recurringTemplates: Flow<List<EventItem>> = dao.getRecurringTemplateEvents()

    fun getSingleEventsForDate(date: String): Flow<List<EventItem>> {
        return dao.getSingleEventsForDate(date)
    }

    suspend fun getEventById(id: String): EventItem? = withContext(Dispatchers.IO) {
        dao.getEventById(id)
    }

    suspend fun checkAndSeedDefaults() = withContext(Dispatchers.IO) {
        if (dao.getCategoriesCount() == 0) {
            val defaults = listOf(
                Category(id = "cat_work", name = "Trabalho", colorHex = 0xFF2563EB, iconName = "work"),
                Category(id = "cat_routine", name = "Rotina", colorHex = 0xFFD97706, iconName = "repeat"),
                Category(id = "cat_fitness", name = "Treino & Saúde", colorHex = 0xFF059669, iconName = "fitness"),
                Category(id = "cat_study", name = "Estudos", colorHex = 0xFF7C3AED, iconName = "school"),
                Category(id = "cat_personal", name = "Pessoal", colorHex = 0xFFE11D48, iconName = "person"),
                Category(id = "cat_leisure", name = "Lazer", colorHex = 0xFF0891B2, iconName = "celebration")
            )
            dao.insertCategories(defaults)

            // Seed an initial welcoming sample timeline for today so user immediately understands Dayline
            val today = LocalDate.now().toString()
            val sampleEvents = listOf(
                EventItem(
                    id = UUID.randomUUID().toString(),
                    type = EventItem.TYPE_EVENT,
                    title = "Café da manhã & planejamento",
                    date = today,
                    startTime = "08:00",
                    endTime = "08:30",
                    durationMinutes = 30,
                    categoryId = "cat_routine",
                    priority = EventItem.PRIORITY_NORMAL,
                    reminderMinutesBefore = 5
                ),
                EventItem(
                    id = UUID.randomUUID().toString(),
                    type = EventItem.TYPE_EVENT,
                    title = "Revisão e foco do dia",
                    date = today,
                    startTime = "09:00",
                    endTime = "12:00",
                    durationMinutes = 180,
                    categoryId = "cat_work",
                    priority = EventItem.PRIORITY_HIGH,
                    reminderMinutesBefore = 10
                ),
                EventItem(
                    id = UUID.randomUUID().toString(),
                    type = EventItem.TYPE_EVENT,
                    title = "Almoço & descanso",
                    date = today,
                    startTime = "12:30",
                    endTime = "13:30",
                    durationMinutes = 60,
                    categoryId = "cat_routine",
                    priority = EventItem.PRIORITY_NORMAL,
                    reminderMinutesBefore = 0
                ),
                EventItem(
                    id = UUID.randomUUID().toString(),
                    type = EventItem.TYPE_EVENT,
                    title = "Estudos ou projeto prático",
                    date = today,
                    startTime = "15:00",
                    endTime = "16:30",
                    durationMinutes = 90,
                    categoryId = "cat_study",
                    priority = EventItem.PRIORITY_NORMAL,
                    reminderMinutesBefore = 10
                ),
                EventItem(
                    id = UUID.randomUUID().toString(),
                    type = EventItem.TYPE_TASK,
                    title = "Comprar suplemento e organizar mesa",
                    date = null, // Inbox task
                    categoryId = "cat_personal"
                )
            )
            for (ev in sampleEvents) {
                dao.insertEvent(ev)
            }
        }
    }

    suspend fun saveEvent(event: EventItem) = withContext(Dispatchers.IO) {
        dao.insertEvent(event.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleEventCompletion(event: EventItem) = withContext(Dispatchers.IO) {
        val updated = event.copy(
            isCompleted = !event.isCompleted,
            completedAt = if (!event.isCompleted) System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertEvent(updated)
    }

    suspend fun deleteEventById(id: String) = withContext(Dispatchers.IO) {
        dao.deleteEventById(id)
    }

    suspend fun saveCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteCategoryById(id)
    }

    /**
     * Creates a new recurring series.
     */
    suspend fun createRecurringSeries(
        eventTemplate: EventItem,
        rule: RecurrenceRule
    ) = withContext(Dispatchers.IO) {
        dao.insertRecurrenceRule(rule)
        val template = eventTemplate.copy(
            id = UUID.randomUUID().toString(),
            recurrenceRuleId = rule.id,
            date = rule.startsOnDate,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertEvent(template)
    }

    /**
     * Edits a recurring occurrence with scope handling.
     */
    suspend fun editRecurringOccurrence(
        currentEvent: EventItem,
        updatedEvent: EventItem,
        targetDate: LocalDate,
        scope: RecurrenceScope
    ) = withContext(Dispatchers.IO) {
        val ruleId = currentEvent.recurrenceRuleId ?: return@withContext
        val rule = dao.getRecurrenceRuleById(ruleId) ?: return@withContext

        when (scope) {
            RecurrenceScope.THIS_ONLY -> {
                val overrideId = UUID.randomUUID().toString()
                val overrideEvent = updatedEvent.copy(
                    id = overrideId,
                    date = targetDate.toString(),
                    recurrenceRuleId = null,
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertEvent(overrideEvent)

                val exception = RecurrenceException(
                    id = UUID.randomUUID().toString(),
                    recurrenceRuleId = ruleId,
                    originalOccurrenceDate = targetDate.toString(),
                    action = "MODIFIED",
                    overrideItemId = overrideId
                )
                dao.insertRecurrenceException(exception)
            }
            RecurrenceScope.THIS_AND_FUTURE -> {
                // End old rule before target date
                val previousDay = targetDate.minusDays(1).toString()
                val oldRuleUpdated = rule.copy(endsOnDate = previousDay)
                dao.updateRecurrenceRule(oldRuleUpdated)

                // Create new rule starting on targetDate
                val newRuleId = UUID.randomUUID().toString()
                val newRule = rule.copy(
                    id = newRuleId,
                    startsOnDate = targetDate.toString()
                )
                dao.insertRecurrenceRule(newRule)

                val newTemplate = updatedEvent.copy(
                    id = UUID.randomUUID().toString(),
                    recurrenceRuleId = newRuleId,
                    date = targetDate.toString(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertEvent(newTemplate)
            }
            RecurrenceScope.ALL_SERIES -> {
                // Update template
                dao.insertEvent(
                    updatedEvent.copy(
                        id = currentEvent.id,
                        recurrenceRuleId = ruleId,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Deletes a recurring occurrence with scope handling.
     */
    suspend fun deleteRecurringOccurrence(
        currentEvent: EventItem,
        targetDate: LocalDate,
        scope: RecurrenceScope
    ) = withContext(Dispatchers.IO) {
        val ruleId = currentEvent.recurrenceRuleId ?: run {
            dao.deleteEventById(currentEvent.id)
            return@withContext
        }
        val rule = dao.getRecurrenceRuleById(ruleId) ?: return@withContext

        when (scope) {
            RecurrenceScope.THIS_ONLY -> {
                val exception = RecurrenceException(
                    id = UUID.randomUUID().toString(),
                    recurrenceRuleId = ruleId,
                    originalOccurrenceDate = targetDate.toString(),
                    action = "SKIPPED"
                )
                dao.insertRecurrenceException(exception)
            }
            RecurrenceScope.THIS_AND_FUTURE -> {
                val previousDay = targetDate.minusDays(1).toString()
                val oldRuleUpdated = rule.copy(endsOnDate = previousDay)
                dao.updateRecurrenceRule(oldRuleUpdated)
            }
            RecurrenceScope.ALL_SERIES -> {
                dao.deleteExceptionsByRuleId(ruleId)
                dao.deleteEventsByRecurrenceRuleId(ruleId)
                dao.deleteRecurrenceRuleById(ruleId)
            }
        }
    }

    /**
     * Applies a routine template for a specific date.
     */
    suspend fun applyRoutineTemplate(
        templateType: String,
        targetDate: LocalDate
    ) = withContext(Dispatchers.IO) {
        val dateStr = targetDate.toString()
        val templateItems = when (templateType) {
            "WORKDAY" -> listOf(
                EventItem(id = UUID.randomUUID().toString(), title = "Acordar & Alongamento", date = dateStr, startTime = "07:00", endTime = "07:30", categoryId = "cat_routine"),
                EventItem(id = UUID.randomUUID().toString(), title = "Café da manhã", date = dateStr, startTime = "07:30", endTime = "08:15", categoryId = "cat_routine"),
                EventItem(id = UUID.randomUUID().toString(), title = "Bloco de Trabalho Manhã", date = dateStr, startTime = "08:30", endTime = "12:30", categoryId = "cat_work", priority = EventItem.PRIORITY_HIGH),
                EventItem(id = UUID.randomUUID().toString(), title = "Almoço & Descanso", date = dateStr, startTime = "12:30", endTime = "13:30", categoryId = "cat_routine"),
                EventItem(id = UUID.randomUUID().toString(), title = "Bloco de Trabalho Tarde", date = dateStr, startTime = "13:30", endTime = "18:00", categoryId = "cat_work", priority = EventItem.PRIORITY_HIGH),
                EventItem(id = UUID.randomUUID().toString(), title = "Treino / Academia", date = dateStr, startTime = "18:30", endTime = "19:30", categoryId = "cat_fitness"),
                EventItem(id = UUID.randomUUID().toString(), title = "Jantar & Lazer", date = dateStr, startTime = "20:00", endTime = "21:30", categoryId = "cat_leisure"),
                EventItem(id = UUID.randomUUID().toString(), title = "Leitura & Desconectar", date = dateStr, startTime = "22:00", endTime = "22:45", categoryId = "cat_personal")
            )
            "WEEKEND" -> listOf(
                EventItem(id = UUID.randomUUID().toString(), title = "Acordar sem pressa", date = dateStr, startTime = "09:00", endTime = "09:45", categoryId = "cat_routine"),
                EventItem(id = UUID.randomUUID().toString(), title = "Caminhada ao ar livre", date = dateStr, startTime = "10:15", endTime = "11:30", categoryId = "cat_fitness"),
                EventItem(id = UUID.randomUUID().toString(), title = "Almoço especial", date = dateStr, startTime = "12:30", endTime = "14:00", categoryId = "cat_leisure"),
                EventItem(id = UUID.randomUUID().toString(), title = "Tempo livre / Hobbies", date = dateStr, startTime = "15:00", endTime = "18:00", categoryId = "cat_personal"),
                EventItem(id = UUID.randomUUID().toString(), title = "Cinema ou amigos", date = dateStr, startTime = "19:30", endTime = "22:30", categoryId = "cat_leisure")
            )
            "STUDY" -> listOf(
                EventItem(id = UUID.randomUUID().toString(), title = "Revisão teórica matutina", date = dateStr, startTime = "09:00", endTime = "11:00", categoryId = "cat_study", priority = EventItem.PRIORITY_HIGH),
                EventItem(id = UUID.randomUUID().toString(), title = "Resolução de exercícios", date = dateStr, startTime = "11:15", endTime = "12:30", categoryId = "cat_study"),
                EventItem(id = UUID.randomUUID().toString(), title = "Pausa para almoço", date = dateStr, startTime = "12:30", endTime = "14:00", categoryId = "cat_routine"),
                EventItem(id = UUID.randomUUID().toString(), title = "Projeto prático de código", date = dateStr, startTime = "14:30", endTime = "17:30", categoryId = "cat_study", priority = EventItem.PRIORITY_HIGH),
                EventItem(id = UUID.randomUUID().toString(), title = "Resumo e anotações", date = dateStr, startTime = "17:45", endTime = "18:30", categoryId = "cat_study")
            )
            else -> emptyList()
        }

        for (item in templateItems) {
            dao.insertEvent(item)
        }
    }
}
