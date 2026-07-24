package com.tatara.data.tdee

import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.Sex
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class WeightPoint(val date: LocalDate, val kg: Float)

data class TdeeInputs(
    /** The Sunday being closed; the week is [weekEnd-6, weekEnd]. */
    val weekEnd: LocalDate,
    /** kcal per day; a date is a "logged day" iff it has a key. */
    val dailyKcal: Map<LocalDate, Float>,
    /** Full weight history up to the run moment, chronological, one point per date. */
    val weights: List<WeightPoint>,
    val currentTargetKcal: Float?,
    val settings: Settings,
    /** Earliest food or weight log ever; null when nothing is logged. */
    val firstLogDate: LocalDate?,
)

enum class SkipReason(val message: String) {
    /** §3.5 — no adjustment in the first 14 days; the app needs a baseline. */
    TOO_EARLY("No adjustment in the first 14 days."),
    TOO_FEW_LOGGED_DAYS("Not enough data. Fewer than 5 logged days."),
    /** §3.5 — fewer than 4 weigh-ins is mostly water noise; adjusting on it is worse than not adjusting. */
    TOO_FEW_WEIGH_INS("Skipped. Fewer than 4 weigh-ins this week."),
}

sealed interface TdeeOutcome {
    data class Adjusted(
        val kcalTarget: Float,
        val proteinG: Float,
        val fatG: Float,
        val carbsG: Float,
        val meanDailyKcal: Float,
        val ewmaStart: Float,
        val ewmaEnd: Float,
        val impliedTdee: Float,
        val weeklyRatePercent: Float,
        val computedFromWeightKg: Float,
        val newRatchetKg: Float,
        val notes: List<String>,
    ) : TdeeOutcome

    data class Skipped(val reason: SkipReason) : TdeeOutcome
}

/**
 * §3.5 — the Sunday recalculation, as a pure function. Every intermediate is returned
 * so the weekly review can show the arithmetic; no black boxes.
 */
object TdeeCalculator {

    const val KCAL_PER_KG = 7700f
    private const val ALPHA = 0.25f

    fun compute(inputs: TdeeInputs): TdeeOutcome {
        val weekEnd = inputs.weekEnd
        val weekStart = weekEnd.minusDays(6)
        val settings = inputs.settings

        val firstLog = inputs.firstLogDate
            ?: return TdeeOutcome.Skipped(SkipReason.TOO_EARLY)
        if (ChronoUnit.DAYS.between(firstLog, weekEnd) < 13) {
            return TdeeOutcome.Skipped(SkipReason.TOO_EARLY)
        }

        val loggedDays = inputs.dailyKcal.keys.count { it in weekStart..weekEnd }
        if (loggedDays < 5) return TdeeOutcome.Skipped(SkipReason.TOO_FEW_LOGGED_DAYS)

        val weekWeights = inputs.weights.filter { it.date in weekStart..weekEnd }
        if (weekWeights.size < 4) return TdeeOutcome.Skipped(SkipReason.TOO_FEW_WEIGH_INS)

        // §3.5 step 1 — the EWMA is continuous over the whole weight history; it never
        // resets weekly, so a noisy Monday weigh-in cannot become the baseline.
        val ewmaByDate = ewmaSeries(inputs.weights.filter { !it.date.isAfter(weekEnd) })
        val startAnchor = ewmaByDate.lastOrNull { !it.date.isAfter(weekStart) }
            ?: ewmaByDate.first { it.date in weekStart..weekEnd }
        val endAnchor = ewmaByDate.last()
        val daysElapsed = max(1L, ChronoUnit.DAYS.between(startAnchor.date, endAnchor.date)).toFloat()

        val meanDailyKcal = inputs.dailyKcal
            .filterKeys { it in weekStart..weekEnd }
            .values.sum() / loggedDays

        // §3.5 step 2 — back-calculate maintenance.
        val deltaKg = endAnchor.kg - startAnchor.kg
        val tdee = meanDailyKcal - (deltaKg * KCAL_PER_KG / daysElapsed)

        // §3.5 step 3 — apply the goal rate (signed: negative = cut).
        val weeklyRateKcal = settings.goalRatePercent / 100f * endAnchor.kg * KCAL_PER_KG
        var target = tdee + weeklyRateKcal / 7f
        val notes = mutableListOf<String>()

        inputs.currentTargetKcal?.let { current ->
            val capped = target.coerceIn(current * 0.9f, current * 1.1f)
            if (capped != target) notes.add("Weekly change capped at 10%.")
            target = capped
        }

        bmr(settings, endAnchor.kg, weekEnd)?.let { bmr ->
            val clamped = target.coerceIn(bmr, 2.5f * bmr)
            if (clamped != target) notes.add("Calorie target held to BMR bounds.")
            target = clamped
        }

        // §3.4.1 — the protein ratchet never drops mid-block.
        val newRatchet = max(settings.ratchetWeightKg ?: endAnchor.kg, endAnchor.kg)
        val proteinG = settings.proteinPerKg * newRatchet

        // §3.4 — fat at 30% of calories, floored at max(20% kcal, 0.7 g/kg), ceiling 35%.
        val fatFloorG = max(0.20f * target / 9f, 0.7f * endAnchor.kg)
        val fatCeilG = 0.35f * target / 9f
        val fatG = (0.30f * target / 9f).coerceIn(min(fatFloorG, fatCeilG), max(fatFloorG, fatCeilG))

        val carbsG = max(0f, (target - proteinG * 4f - fatG * 9f) / 4f)

        return TdeeOutcome.Adjusted(
            kcalTarget = target,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            meanDailyKcal = meanDailyKcal,
            ewmaStart = startAnchor.kg,
            ewmaEnd = endAnchor.kg,
            impliedTdee = tdee,
            weeklyRatePercent = settings.goalRatePercent,
            computedFromWeightKg = endAnchor.kg,
            newRatchetKg = newRatchet,
            notes = notes,
        )
    }

    /** Smoothed value after each weigh-in: ewma[0] = w[0]; ewma[n] = α·w + (1−α)·prev. */
    fun ewmaSeries(weights: List<WeightPoint>): List<WeightPoint> {
        var ewma = 0f
        return weights.sortedBy { it.date }.mapIndexed { i, p ->
            ewma = if (i == 0) p.kg else ALPHA * p.kg + (1 - ALPHA) * ewma
            WeightPoint(p.date, ewma)
        }
    }

    /** Mifflin-St Jeor; null when the profile is incomplete — the bounds are then skipped. */
    fun bmr(settings: Settings, weightKg: Float, on: LocalDate): Float? {
        val height = settings.heightCm ?: return null
        val birthYear = settings.birthYear ?: return null
        val sex = settings.sex ?: return null
        val age = (on.year - birthYear).toFloat()
        val sexTerm = if (sex == Sex.MALE) 5f else -161f
        return 10f * weightKg + 6.25f * height - 5f * age + sexTerm
    }

    /**
     * §3.5 — three consecutive weeks whose smoothed delta contradicts the goal.
     * deltas are ewmaEnd − ewmaStart per adjusted week, oldest first, current last.
     */
    fun stallNote(deltas: List<Float>, goalRatePercent: Float): String? {
        if (goalRatePercent == 0f || deltas.size < 3) return null
        val contradicts = deltas.takeLast(3).all { d ->
            if (goalRatePercent < 0f) d >= -0.05f else d <= 0.05f
        }
        return if (contradicts) {
            "Three weeks without movement. Either intake is being under-logged or the rate is too slow — pick one."
        } else null
    }

    /** §3.4.1 — offer a mid-week refresh when smoothed weight moved >2% since the last recalculation. */
    fun shouldOfferMidweekRefresh(lastComputedFromKg: Float?, currentEwmaKg: Float?): Boolean {
        if (lastComputedFromKg == null || currentEwmaKg == null) return false
        return abs(currentEwmaKg - lastComputedFromKg) / lastComputedFromKg > 0.02f
    }
}
