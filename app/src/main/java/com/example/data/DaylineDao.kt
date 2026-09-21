package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Category
import com.example.model.EventItem
import com.example.model.RecurrenceException
import com.example.model.RecurrenceRule
import kotlinx.coroutines.flow.Flow

@Dao
interface DaylineDao {

    // --- EVENTS & TASKS ---
    @Query("SELECT * FROM event_items ORDER BY startTime ASC, createdAt ASC")
    fun getAllEvents(): Flow<List<EventItem>>

    @Query("SELECT * FROM event_items WHERE date = :date AND recurrenceRuleId IS NULL ORDER BY startTime ASC")
    fun getSingleEventsForDate(date: String): Flow<List<EventItem>>

    @Query("SELECT * FROM event_items WHERE type = 'TASK' AND (date IS NULL OR date = '') ORDER BY createdAt DESC")
    fun getUnscheduledTasks(): Flow<List<EventItem>>

    @Query("SELECT * FROM event_items WHERE recurrenceRuleId IS NOT NULL")
    fun getRecurringTemplateEvents(): Flow<List<EventItem>>

    @Query("SELECT * FROM event_items WHERE id = :id")
    suspend fun getEventById(id: String): EventItem?

    @Query("SELECT * FROM event_items WHERE id = :id")
    fun getEventByIdFlow(id: String): Flow<EventItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(item: EventItem)

    @Update
    suspend fun updateEvent(item: EventItem)

    @Delete
    suspend fun deleteEvent(item: EventItem)

    @Query("DELETE FROM event_items WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("DELETE FROM event_items WHERE recurrenceRuleId = :ruleId")
    suspend fun deleteEventsByRecurrenceRuleId(ruleId: String)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    // --- RECURRENCE RULES ---
    @Query("SELECT * FROM recurrence_rules")
    fun getAllRecurrenceRules(): Flow<List<RecurrenceRule>>

    @Query("SELECT * FROM recurrence_rules WHERE id = :id")
    suspend fun getRecurrenceRuleById(id: String): RecurrenceRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrenceRule(rule: RecurrenceRule)

    @Update
    suspend fun updateRecurrenceRule(rule: RecurrenceRule)

    @Query("DELETE FROM recurrence_rules WHERE id = :id")
    suspend fun deleteRecurrenceRuleById(id: String)

    // --- RECURRENCE EXCEPTIONS ---
    @Query("SELECT * FROM recurrence_exceptions")
    fun getAllRecurrenceExceptions(): Flow<List<RecurrenceException>>

    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceRuleId = :ruleId")
    suspend fun getExceptionsByRuleId(ruleId: String): List<RecurrenceException>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurrenceException(exception: RecurrenceException)

    @Query("DELETE FROM recurrence_exceptions WHERE recurrenceRuleId = :ruleId")
    suspend fun deleteExceptionsByRuleId(ruleId: String)
}
