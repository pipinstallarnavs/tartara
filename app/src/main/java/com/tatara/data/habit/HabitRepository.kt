package com.tatara.data.habit

import com.tatara.data.EditWindow
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Habit
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Stack
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class HabitRepository(
    private val db: TataraDatabase,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Instant = { Instant.now() },
) {

    companion object {
        /** §5.1 — hard cap on the HABITS list; hygiene is uncapped. */
        const val HABIT_CAP = 5

        /** §5.6 — freeze tokens per calendar month; unused tokens do not roll over. */
        const val TOKENS_PER_MONTH = 2
    }

    suspend fun stacks(): List<Stack> = db.habitDao().getAllStacks()

    suspend fun habits(): List<Habit> = db.habitDao().getAllHabits()

    suspend fun logsOn(date: LocalDate): Map<Long, HabitLog> =
        db.habitDao().logsOn(date).associateBy { it.habitId }

    suspend fun logsByHabit(): Map<Long, List<HabitLog>> =
        db.habitDao().getAllLogs().groupBy { it.habitId }

    /**
     * Tick or untick within the §2.1 window. completedAt is recorded only for
     * live (same-day) ticks — backdated ticks would pollute context stability.
     */
    suspend fun toggle(habitId: Long, date: LocalDate = today()): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        val existing = db.habitDao().logFor(habitId, date)
        when (existing?.status) {
            HabitLogStatus.COMPLETED -> db.habitDao().deleteLogFor(habitId, date)
            else -> {
                if (existing != null) db.habitDao().deleteLogFor(habitId, date)
                db.habitDao().insertLog(
                    HabitLog(
                        habitId = habitId,
                        date = date,
                        status = HabitLogStatus.COMPLETED,
                        completedAt = if (date == today()) now() else null,
                    )
                )
            }
        }
        return true
    }

    /** §5.6 — a frozen day is neutral: no penalty, no XP, streak preserved. */
    suspend fun freeze(habitId: Long, date: LocalDate = today()): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        val existing = db.habitDao().logFor(habitId, date)
        if (existing?.status == HabitLogStatus.COMPLETED) return false
        if (existing?.status == HabitLogStatus.FROZEN) return true
        if (tokensLeft(YearMonth.from(date)) <= 0) return false
        if (existing != null) db.habitDao().deleteLogFor(habitId, date)
        db.habitDao().insertLog(HabitLog(habitId = habitId, date = date, status = HabitLogStatus.FROZEN))
        return true
    }

    suspend fun unfreeze(habitId: Long, date: LocalDate): Boolean {
        if (!EditWindow.isEditable(date, today())) return false
        if (db.habitDao().logFor(habitId, date)?.status != HabitLogStatus.FROZEN) return false
        db.habitDao().deleteLogFor(habitId, date)
        return true
    }

    suspend fun tokensLeft(month: YearMonth = YearMonth.from(today())): Int =
        TOKENS_PER_MONTH - db.habitDao().frozenCountBetween(month.atDay(1), month.atEndOfMonth())

    /** Returns null when the §5.1 HABITS cap would be exceeded. */
    suspend fun createHabit(habit: Habit): Habit? {
        if (habit.list == HabitList.HABIT &&
            db.habitDao().getAllHabits().count { it.list == HabitList.HABIT } >= HABIT_CAP
        ) return null
        return habit.copy(id = db.habitDao().insertHabit(habit))
    }

    /** Returns false when moving this habit into HABIT would exceed the §5.1 cap. */
    suspend fun updateHabit(habit: Habit): Boolean {
        if (habit.list == HabitList.HABIT &&
            db.habitDao().getAllHabits().count { it.list == HabitList.HABIT && it.id != habit.id } >= HABIT_CAP
        ) return false
        db.habitDao().updateHabit(habit)
        return true
    }

    suspend fun deleteHabit(id: Long) = db.habitDao().deleteHabit(id)
}
