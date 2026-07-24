package com.tatara.data.habit

import androidx.room.withTransaction
import com.tatara.data.EditWindow
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * §5.4 — the automaticity engine, run as a day-close job on app open. A day is
 * closed only once it leaves the §2.1 edit window, so ticking and unticking an
 * editable day never needs automaticity reversal. Unlogged closed days get an
 * explicit MISSED log, which is what makes history unambiguous.
 */
class HabitCloseService(private val db: TataraDatabase) {

    suspend fun closeOpenDays(
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ) {
        db.withTransaction {
            val settings = db.bodyDao().getSettings() ?: Settings()
            val habits = db.habitDao().getAllHabits()
            if (habits.isEmpty()) return@withTransaction

            // Last day outside the editable [today-2, today] window.
            val closeThrough = today.minusDays(EditWindow.ENTRY_DAYS + 1)
            val start = settings.lastHabitDayClosed?.plusDays(1)
                ?: habits.minOf { it.createdAt.atZone(zone).toLocalDate() }
            if (start.isAfter(closeThrough)) return@withTransaction

            val state = habits.associateBy { it.id }.toMutableMap()
            var day = start
            while (!day.isAfter(closeThrough)) {
                for (id in state.keys) {
                    val habit = state.getValue(id)
                    if (habit.createdAt.atZone(zone).toLocalDate().isAfter(day)) continue

                    val log = db.habitDao().logFor(id, day)
                    when (log?.status) {
                        HabitLogStatus.COMPLETED -> {
                            var updated = habit.copy(
                                automaticity = Automaticity.afterCompletion(habit.automaticity),
                                consecutiveMisses = 0,
                            )
                            if (updated.automaticity >= Automaticity.GRADUATION &&
                                updated.list == HabitList.HABIT
                            ) {
                                updated = updated.copy(list = HabitList.HYGIENE, graduatedAt = now)
                            }
                            state[id] = updated
                        }
                        HabitLogStatus.FROZEN -> Unit
                        else -> {
                            val misses = habit.consecutiveMisses + 1
                            state[id] = habit.copy(
                                automaticity = Automaticity.afterMiss(habit.automaticity, misses),
                                consecutiveMisses = misses,
                            )
                            if (log == null) {
                                db.habitDao().insertLog(
                                    HabitLog(habitId = id, date = day, status = HabitLogStatus.MISSED)
                                )
                            }
                        }
                    }
                }
                day = day.plusDays(1)
            }

            state.values.forEach { db.habitDao().updateHabit(it) }
            db.bodyDao().upsertSettings(settings.copy(lastHabitDayClosed = closeThrough))
        }
    }
}
