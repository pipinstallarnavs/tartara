package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.Stack
import java.time.LocalDate

@Dao
interface HabitDao {
    @Insert suspend fun insertStack(stack: Stack): Long
    @Insert suspend fun insertStacks(stacks: List<Stack>)
    @Query("SELECT * FROM stack ORDER BY id") suspend fun getAllStacks(): List<Stack>
    @Query("DELETE FROM stack") suspend fun deleteAllStacks()

    @Insert suspend fun insertHabit(habit: Habit): Long
    @Insert suspend fun insertHabits(habits: List<Habit>)
    @Update suspend fun updateHabit(habit: Habit)
    @Query("SELECT * FROM habit ORDER BY id") suspend fun getAllHabits(): List<Habit>
    @Query("DELETE FROM habit WHERE id = :id") suspend fun deleteHabit(id: Long)
    @Query("DELETE FROM habit") suspend fun deleteAllHabits()

    @Insert suspend fun insertLog(log: HabitLog): Long
    @Insert suspend fun insertLogs(logs: List<HabitLog>)
    @Query("SELECT * FROM habit_log WHERE date = :date") suspend fun logsOn(date: LocalDate): List<HabitLog>
    @Query("SELECT * FROM habit_log WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun logFor(habitId: Long, date: LocalDate): HabitLog?
    @Query("DELETE FROM habit_log WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLogFor(habitId: Long, date: LocalDate)
    @Query("SELECT COUNT(*) FROM habit_log WHERE status = 'FROZEN' AND date BETWEEN :from AND :to")
    suspend fun frozenCountBetween(from: LocalDate, to: LocalDate): Int
    @Query("SELECT * FROM habit_log ORDER BY id") suspend fun getAllLogs(): List<HabitLog>
    @Query("DELETE FROM habit_log") suspend fun deleteAllLogs()
}
