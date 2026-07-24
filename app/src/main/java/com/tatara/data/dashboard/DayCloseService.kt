package com.tatara.data.dashboard

import androidx.room.withTransaction
import com.tatara.data.EditWindow
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.DailyRollup
import com.tatara.data.db.entity.HabitList
import com.tatara.data.db.entity.HabitLog
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.TierCrossing
import com.tatara.data.db.entity.XpEvent
import com.tatara.data.db.entity.XpEventType
import com.tatara.data.food.MacroMath
import com.tatara.data.food.MacroTotals
import com.tatara.data.habit.Automaticity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The day-close job: when a day leaves the §2.1 edit window it is judged exactly
 * once — habit automaticity moves (§5.4), XP is earned and lost (§7.2), the ring
 * is evaluated (§7.1), tier crossings are recorded (§7.3), and the daily_rollup
 * cache row is written (§2.4). Replaces HabitCloseService; same marker.
 */
class DayCloseService(private val db: TataraDatabase) {

    suspend fun closeOpenDays(
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ) {
        db.withTransaction {
            val settings = db.bodyDao().getSettings() ?: Settings()
            val habits = db.habitDao().getAllHabits()

            val firstActivity = listOfNotNull(
                habits.minOfOrNull { it.createdAt.atZone(zone).toLocalDate() },
                db.foodDao().firstEntryDate(),
                db.bodyDao().firstWeightDate(),
            ).minOrNull() ?: return@withTransaction

            val closeThrough = today.minusDays(EditWindow.ENTRY_DAYS + 1)
            val start = settings.lastHabitDayClosed?.plusDays(1) ?: firstActivity
            if (start.isAfter(closeThrough)) return@withTransaction

            val state = habits.associateBy { it.id }.toMutableMap()
            var runningXp = db.dashboardDao().totalXp()
            // Band 0 (Tamahagane) is where everyone starts — never a "crossing".
            var maxBand = db.dashboardDao().getAllTierCrossings()
                .maxOfOrNull { Levels.bandFor(it.level) } ?: 0

            var day = start
            while (!day.isAfter(closeThrough)) {
                var dayXp = 0L
                val events = mutableListOf<XpEvent>()
                fun record(type: XpEventType, amount: Int) {
                    if (amount == 0) return
                    events.add(XpEvent(date = day, type = type, amount = amount))
                    dayXp += amount
                }

                // ---- Habits (§5.4 automaticity; §7.2 XP on the HABIT list only) ----
                var habitTotal = 0
                var habitCompleted = 0
                var allHabitsSatisfied = true
                for (id in state.keys) {
                    val habit = state.getValue(id)
                    if (habit.createdAt.atZone(zone).toLocalDate().isAfter(day)) continue
                    val isHabitList = habit.list == HabitList.HABIT
                    if (isHabitList) habitTotal++

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
                            if (isHabitList) {
                                habitCompleted++
                                record(XpEventType.HABIT_COMPLETED, Levels.XP_HABIT_COMPLETED)
                            }
                        }
                        // §5.6 — frozen is neutral: no penalty, no XP, ring not broken.
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
                            if (isHabitList) {
                                allHabitsSatisfied = false
                                // §7.2 — same escalating scale as §5.4; first consecutive miss free.
                                val pct = Automaticity.penaltyFor(misses)
                                val level = Levels.effectiveLevel(runningXp + dayXp, maxBand)
                                val penalty = -(pct * Levels.cost(level)).roundToInt()
                                record(XpEventType.HABIT_MISS_PENALTY, penalty)
                            }
                        }
                    }
                }

                // ---- Food, sleep, weight, workouts ----
                val totals = db.foodDao().entriesWithFoodOn(day)
                    .fold(MacroTotals()) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity) }
                val target = db.bodyDao().adjustmentOn(day)?.kcalTarget
                val kcalInRange = target != null && abs(totals.kcal - target) <= 0.10f * target

                val sleepLogged = db.sleepDao().logOn(day) != null
                if (sleepLogged) record(XpEventType.SLEEP_LOGGED, Levels.XP_SLEEP_LOGGED)

                if (db.bodyDao().weightsBetween(day, day).isNotEmpty()) {
                    record(XpEventType.WEIGHT_LOGGED, Levels.XP_WEIGHT_LOGGED)
                }

                val weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val priorThisWeek =
                    if (day == weekStart) 0
                    else db.trainDao().sessionsBetween(weekStart, day.minusDays(1)).size
                db.trainDao().sessionsBetween(day, day).forEachIndexed { i, _ ->
                    record(XpEventType.WORKOUT_LOGGED, Levels.workoutXp(priorThisWeek + i + 1))
                }

                // §7.1 — ring closed: kcal ±10%, all HABITS satisfied, sleep logged.
                val ringClosed = kcalInRange && allHabitsSatisfied && sleepLogged
                if (ringClosed) record(XpEventType.RING_CLOSED, Levels.XP_RING_CLOSED)

                events.forEach { db.dashboardDao().insertXpEvent(it) }
                runningXp += dayXp

                // §7.3 — crossings are permanent and recorded, one row per band entered.
                val band = Levels.bandFor(Levels.effectiveLevel(runningXp, maxBand))
                while (band > maxBand) {
                    maxBand++
                    db.dashboardDao().insertTierCrossing(
                        TierCrossing(
                            tier = Levels.TIER_NAMES[maxBand],
                            level = Levels.bandStartLevel(maxBand),
                            date = day,
                        )
                    )
                }

                // §2.4 — the rollup cache row for this day.
                db.dashboardDao().upsertRollup(
                    DailyRollup(
                        date = day,
                        kcal = totals.kcal, proteinG = totals.protein, carbsG = totals.carbs,
                        fatG = totals.fat, satFatG = totals.satFat,
                        habitsCompleted = habitCompleted, habitsTotal = habitTotal,
                        ringClosed = ringClosed,
                    )
                )

                day = day.plusDays(1)
            }

            state.values.forEach { db.habitDao().updateHabit(it) }
            db.bodyDao().upsertSettings(
                (db.bodyDao().getSettings() ?: settings).copy(lastHabitDayClosed = closeThrough)
            )
        }
    }
}
