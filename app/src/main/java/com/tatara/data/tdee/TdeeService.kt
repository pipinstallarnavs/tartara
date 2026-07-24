package com.tatara.data.tdee

import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.TargetAdjustment
import com.tatara.data.food.MacroMath
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * §3.5 — the weekly job. The spec's "runs every Sunday at 23:00" is realised as
 * catch-up on app open: notifications and background scheduling are out of scope
 * (§0, §10), so every completed Sunday since the last examined week is processed,
 * oldest first, each week seeing the targets and ratchet its predecessors produced.
 * A week, once examined, is never recomputed — that is what makes the §2.1
 * backfill-exclusion rule hold.
 */
class TdeeService(private val db: TataraDatabase) {

    data class WeekResult(val weekEnd: LocalDate, val outcome: TdeeOutcome, val stallNote: String?)

    suspend fun catchUp(now: ZonedDateTime): List<WeekResult> {
        val firstLog = firstLogDate() ?: return emptyList()
        val settings = db.bodyDao().getSettings() ?: Settings()

        var sunday = settings.lastProcessedWeekEnd?.plusWeeks(1)
            ?: firstLog.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val results = mutableListOf<WeekResult>()
        while (!ZonedDateTime.of(sunday, LocalTime.of(23, 0), now.zone).isAfter(now)) {
            results.add(runWeek(sunday, now.toInstant()))
            sunday = sunday.plusWeeks(1)
        }
        return results
    }

    suspend fun runWeek(weekEnd: LocalDate, runAt: Instant): WeekResult {
        val settings = db.bodyDao().getSettings() ?: Settings()
        val weekStart = weekEnd.minusDays(6)

        val dailyKcal = db.foodDao().entriesWithFoodBetween(weekStart, weekEnd)
            .groupBy { it.entry.date }
            .mapValues { (_, entries) ->
                entries.fold(0f) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity).kcal }
            }

        // Weights are taken as known at the moment this week is examined; because a
        // week is examined exactly once, weights backfilled later can never rewrite it.
        val weights = db.bodyDao().getAllWeights()
            .filter { !it.loggedAt.isAfter(runAt) }
            .map { WeightPoint(it.date, it.weightKg) }
            .sortedBy { it.date }

        val outcome = TdeeCalculator.compute(
            TdeeInputs(
                weekEnd = weekEnd,
                dailyKcal = dailyKcal,
                weights = weights,
                currentTargetKcal = db.bodyDao().latestAdjustment()?.kcalTarget,
                settings = settings,
                firstLogDate = firstLogDate(),
            )
        )

        var stallNote: String? = null
        if (outcome is TdeeOutcome.Adjusted) {
            db.bodyDao().insertAdjustment(
                TargetAdjustment(
                    effectiveFrom = weekEnd.plusDays(1),
                    kcalTarget = outcome.kcalTarget,
                    proteinG = outcome.proteinG,
                    fatG = outcome.fatG,
                    carbsG = outcome.carbsG,
                    meanDailyKcal = outcome.meanDailyKcal,
                    ewmaStart = outcome.ewmaStart,
                    ewmaEnd = outcome.ewmaEnd,
                    impliedTdee = outcome.impliedTdee,
                    weeklyRatePercent = outcome.weeklyRatePercent,
                    computedFromWeightKg = outcome.computedFromWeightKg,
                )
            )
            db.bodyDao().upsertSettings(
                settings.copy(ratchetWeightKg = outcome.newRatchetKg, lastProcessedWeekEnd = weekEnd)
            )
            val deltas = db.bodyDao().getAllAdjustments()
                .mapNotNull { a -> a.ewmaEnd?.let { end -> a.ewmaStart?.let { end - it } } }
            stallNote = TdeeCalculator.stallNote(deltas, settings.goalRatePercent)
        } else {
            db.bodyDao().upsertSettings(settings.copy(lastProcessedWeekEnd = weekEnd))
        }
        return WeekResult(weekEnd, outcome, stallNote)
    }

    suspend fun shouldOfferMidweekRefresh(runAt: Instant = Instant.now()): Boolean {
        val latest = db.bodyDao().latestAdjustment() ?: return false
        val weights = db.bodyDao().getAllWeights()
            .filter { !it.loggedAt.isAfter(runAt) }
            .map { WeightPoint(it.date, it.weightKg) }
        val currentEwma = TdeeCalculator.ewmaSeries(weights).lastOrNull()?.kg
        return TdeeCalculator.shouldOfferMidweekRefresh(latest.computedFromWeightKg, currentEwma)
    }

    private suspend fun firstLogDate(): LocalDate? {
        val food = db.foodDao().firstEntryDate()
        val weight = db.bodyDao().firstWeightDate()
        return listOfNotNull(food, weight).minOrNull()
    }
}
