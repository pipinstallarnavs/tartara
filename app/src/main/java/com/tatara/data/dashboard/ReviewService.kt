package com.tatara.data.dashboard

import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.db.entity.WeeklyReview
import com.tatara.data.db.entity.XpEvent
import com.tatara.data.db.entity.XpEventType
import com.tatara.data.food.MacroMath
import com.tatara.data.habit.Automaticity
import com.tatara.data.sleep.SleepMath
import com.tatara.data.tdee.TdeeCalculator
import com.tatara.data.tdee.WeightPoint
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Everything the §7.5 card shows, assembled from the tables on demand. */
data class ReviewContent(
    val weekStart: LocalDate,
    val ewmaStart: Float?,
    val ewmaEnd: Float?,
    val adjustment: TargetAdjustment?,
    val loggedDays: Int,
    val meanKcal: Float?,
    val proteinHitRate: Float?,
    val habits: List<Triple<String, Float, String>>,
    val meanSleepMin: Float?,
    val regularity: Float?,
    val sessionCount: Int,
    val setsByGroup: Map<String, Int>,
    val xpEarned: Long,
)

/**
 * §7.5 — reviews generate for each week whose Sunday 23:00 has passed, are archived
 * forever, and award XP exactly once on first open.
 */
class ReviewService(private val db: TataraDatabase) {

    suspend fun generateDueReviews(now: ZonedDateTime) {
        val firstLog = listOfNotNull(
            db.foodDao().firstEntryDate(),
            db.bodyDao().firstWeightDate(),
        ).minOrNull() ?: return

        var weekStart = firstLog.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        while (true) {
            val sundayClose = ZonedDateTime.of(weekStart.plusDays(6), LocalTime.of(23, 0), now.zone)
            if (sundayClose.isAfter(now)) break
            if (db.dashboardDao().reviewByWeekStart(weekStart) == null) {
                db.dashboardDao().insertReview(
                    WeeklyReview(weekStart = weekStart, generatedAt = now.toInstant())
                )
            }
            weekStart = weekStart.plusWeeks(1)
        }
    }

    /** Opening awards XP once (§7.2/§7.5); reopening an archived review is free. */
    suspend fun openReview(id: Long, today: LocalDate, now: Instant = Instant.now()): Boolean {
        val review = db.dashboardDao().reviewById(id) ?: return false
        if (review.openedAt != null) return false
        db.dashboardDao().updateReview(review.copy(openedAt = now))
        db.dashboardDao().insertXpEvent(
            XpEvent(date = today, type = XpEventType.REVIEW_OPENED, amount = Levels.XP_REVIEW_OPENED)
        )
        return true
    }

    suspend fun contentFor(weekStart: LocalDate): ReviewContent {
        val weekEnd = weekStart.plusDays(6)

        val weights = db.bodyDao().getAllWeights()
            .filter { !it.date.isAfter(weekEnd) }
            .map { WeightPoint(it.date, it.weightKg) }
        val ewma = TdeeCalculator.ewmaSeries(weights)
        val ewmaStart = ewma.lastOrNull { !it.date.isAfter(weekStart) }?.kg
            ?: ewma.firstOrNull { it.date in weekStart..weekEnd }?.kg
        val ewmaEnd = ewma.lastOrNull { it.date in weekStart..weekEnd }?.kg

        // The adjustment computed FROM this week takes effect the following Monday.
        val adjustment = db.bodyDao().getAllAdjustments()
            .firstOrNull { it.effectiveFrom == weekEnd.plusDays(1) }
        val inEffect = db.bodyDao().adjustmentOn(weekEnd)

        val byDay = db.foodDao().entriesWithFoodBetween(weekStart, weekEnd)
            .groupBy { it.entry.date }
            .mapValues { (_, entries) ->
                entries.fold(com.tatara.data.food.MacroTotals()) { acc, e ->
                    acc + MacroMath.macrosFor(e.food, e.entry.quantity)
                }
            }
        val loggedDays = byDay.size
        val meanKcal = if (loggedDays == 0) null else byDay.values.map { it.kcal }.sum() / loggedDays
        val proteinHitRate = inEffect?.let { adj ->
            if (loggedDays == 0) null
            else byDay.values.count { it.protein >= 0.95f * adj.proteinG }.toFloat() / loggedDays
        }

        val habits = db.habitDao().getAllHabits().map {
            Triple(it.name, it.automaticity, Automaticity.stage(it.automaticity).label)
        }

        val sleepLogs = db.sleepDao().logsBetween(weekStart, weekEnd)
        val durations = sleepLogs.map { SleepMath.durationMinutes(it.bedTime, it.wakeTime, it.timeToFallAsleepMin) }
        val midpoints = sleepLogs.mapIndexed { i, log -> SleepMath.midpointMinutes(log.bedTime, durations[i]) }

        val sessions = db.trainDao().sessionsBetween(weekStart, weekEnd)
        val groupOf = db.trainDao().getAllExercises().associate { it.id to it.muscleGroup }
        val setsByGroup = mutableMapOf<String, Int>()
        for (session in sessions) {
            for (set in db.trainDao().setsForSession(session.id)) {
                if (set.isWarmup) continue
                val group = groupOf[set.exerciseId] ?: continue
                setsByGroup[group] = (setsByGroup[group] ?: 0) + 1
            }
        }

        return ReviewContent(
            weekStart = weekStart,
            ewmaStart = ewmaStart,
            ewmaEnd = ewmaEnd,
            adjustment = adjustment,
            loggedDays = loggedDays,
            meanKcal = meanKcal,
            proteinHitRate = proteinHitRate,
            habits = habits,
            meanSleepMin = if (durations.isEmpty()) null else durations.sum().toFloat() / durations.size,
            regularity = SleepMath.regularity(midpoints),
            sessionCount = sessions.size,
            setsByGroup = setsByGroup,
            xpEarned = db.dashboardDao().xpBetween(weekStart, weekEnd),
        )
    }
}
