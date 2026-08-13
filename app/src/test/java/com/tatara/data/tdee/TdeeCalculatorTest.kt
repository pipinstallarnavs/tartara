package com.tatara.data.tdee

import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.Sex
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TdeeCalculatorTest {

    // 2026-07-19 is a Sunday; the week is Mon 13th .. Sun 19th.
    private val weekEnd = LocalDate.of(2026, 7, 19)
    private val weekStart = weekEnd.minusDays(6)
    private val firstLog = LocalDate.of(2026, 7, 1)

    private fun week(kcal: Float, days: Int = 7): Map<LocalDate, Float> =
        (0 until days).associate { weekStart.plusDays(it.toLong()) to kcal }

    private fun flatWeights(kg: Float, days: Int = 7): List<WeightPoint> =
        (0 until days).map { WeightPoint(weekStart.plusDays(it.toLong()), kg) }

    private fun inputs(
        dailyKcal: Map<LocalDate, Float> = week(2800f),
        weights: List<WeightPoint> = flatWeights(80f),
        currentTarget: Float? = null,
        settings: Settings = Settings(),
        first: LocalDate? = firstLog,
    ) = TdeeInputs(weekEnd, dailyKcal, weights, currentTarget, settings, first)

    @Test
    fun happyPath_showsAllTheArithmetic() {
        // Flat 80kg, 2800 kcal/day, default 0.5% cut:
        // tdee = 2800 − 0 = 2800; rate = −0.005 × 80 × 7700 = −3080/wk → target 2360.
        val out = TdeeCalculator.compute(inputs()) as TdeeOutcome.Adjusted
        assertEquals(2800f, out.meanDailyKcal, 0.01f)
        assertEquals(80f, out.ewmaStart, 0.001f)
        assertEquals(80f, out.ewmaEnd, 0.001f)
        assertEquals(2800f, out.impliedTdee, 0.01f)
        assertEquals(2360f, out.kcalTarget, 0.5f)
        // protein 1.8 × 80; fat 30% of 2360 = 78.7g (floor max(52.4, 56) doesn't bind).
        assertEquals(144f, out.proteinG, 0.01f)
        assertEquals(0.30f * 2360f / 9f, out.fatG, 0.1f)
        assertEquals((2360f - 144f * 4f - out.fatG * 9f) / 4f, out.carbsG, 0.5f)
        assertEquals(80f, out.computedFromWeightKg, 0.001f)
        assertTrue(out.notes.isEmpty())
    }

    @Test
    fun ewmaSmoothsTheDelta() {
        // Mon 84, Wed 84, Fri 84, Sun 76 → ewma: 84, 84, 84, 0.25×76+0.75×84 = 82.
        // delta −2 over 6 days → tdee = 2500 + 2×7700/6 = 5066.67. Rate 0 → target = tdee.
        val weights = listOf(
            WeightPoint(weekStart, 84f),
            WeightPoint(weekStart.plusDays(2), 84f),
            WeightPoint(weekStart.plusDays(4), 84f),
            WeightPoint(weekEnd, 76f),
        )
        val out = TdeeCalculator.compute(
            inputs(dailyKcal = week(2500f), weights = weights, settings = Settings(goalRatePercent = 0f))
        ) as TdeeOutcome.Adjusted
        assertEquals(84f, out.ewmaStart, 0.001f)
        assertEquals(82f, out.ewmaEnd, 0.001f)
        assertEquals(5066.67f, out.impliedTdee, 0.5f)
        assertEquals(out.impliedTdee, out.kcalTarget, 0.001f)
    }

    @Test
    fun ewmaIsContinuousAcrossHistory() {
        // A weigh-in long before the week anchors the smoothing; the Monday spike
        // is damped instead of becoming the baseline.
        val weights = listOf(WeightPoint(weekStart.minusDays(1), 80f)) +
            flatWeights(84f, days = 7)
        val out = TdeeCalculator.compute(inputs(weights = weights)) as TdeeOutcome.Adjusted
        // Monday's anchor is already smoothed: 0.25×84 + 0.75×80 = 81, not the raw 84.
        assertEquals(81f, out.ewmaStart, 0.001f)
        // Seven 84s pull the ewma up toward 84 but never reach it in a week.
        assertTrue(out.ewmaEnd > 80f && out.ewmaEnd < 84f)
    }

    @Test
    fun skips_firstFourteenDays() {
        val out = TdeeCalculator.compute(inputs(first = weekEnd.minusDays(10)))
        assertEquals(SkipReason.TOO_EARLY, (out as TdeeOutcome.Skipped).reason)
        val none = TdeeCalculator.compute(inputs(first = null))
        assertEquals(SkipReason.TOO_EARLY, (none as TdeeOutcome.Skipped).reason)
    }

    @Test
    fun skips_fewerThanFiveLoggedDays() {
        val out = TdeeCalculator.compute(inputs(dailyKcal = week(2800f, days = 4)))
        assertEquals(SkipReason.TOO_FEW_LOGGED_DAYS, (out as TdeeOutcome.Skipped).reason)
    }

    @Test
    fun skips_fewerThanFourWeighIns() {
        val out = TdeeCalculator.compute(inputs(weights = flatWeights(80f, days = 3)))
        assertEquals(SkipReason.TOO_FEW_WEIGH_INS, (out as TdeeOutcome.Skipped).reason)
    }

    @Test
    fun weeklyChangeIsCappedAtTenPercent() {
        // Raw target 2360 against a current 2900 → floor 2610.
        val out = TdeeCalculator.compute(inputs(currentTarget = 2900f)) as TdeeOutcome.Adjusted
        assertEquals(2610f, out.kcalTarget, 0.5f)
        assertTrue(out.notes.any { "capped" in it })
    }

    @Test
    fun bmrBoundsHoldWhenProfileIsSet() {
        // Mifflin: 10×80 + 6.25×175 − 5×26 + 5 = 1768.75. Raw target 1200−440=760 → clamped.
        val settings = Settings(heightCm = 175f, birthYear = 2000, sex = Sex.MALE)
        val out = TdeeCalculator.compute(
            inputs(dailyKcal = week(1200f), settings = settings)
        ) as TdeeOutcome.Adjusted
        assertEquals(1768.75f, out.kcalTarget, 0.5f)
        assertTrue(out.notes.any { "BMR" in it })
    }

    @Test
    fun bmrBoundsSkippedWhenProfileIncomplete() {
        val out = TdeeCalculator.compute(inputs(dailyKcal = week(1200f))) as TdeeOutcome.Adjusted
        assertEquals(760f, out.kcalTarget, 0.5f)
    }

    @Test
    fun formulaMaintenanceCombinesBmrAndActivity() {
        // Mifflin: 10×80 + 6.25×175 − 5×26 + 5 = 1768.75; × 1.55 moderate = 2741.5625.
        val settings = Settings(
            heightCm = 175f, birthYear = 2000, sex = Sex.MALE,
            activityLevel = com.tatara.data.db.entity.ActivityLevel.MODERATE,
        )
        val estimate = TdeeCalculator.formulaMaintenance(settings, 80f, LocalDate.of(2026, 7, 23))
        assertEquals(2741.5625f, estimate!!, 0.5f)
    }

    @Test
    fun formulaMaintenanceNullWhenActivityLevelMissing() {
        val settings = Settings(heightCm = 175f, birthYear = 2000, sex = Sex.MALE)
        assertNull(TdeeCalculator.formulaMaintenance(settings, 80f, LocalDate.of(2026, 7, 23)))
    }

    @Test
    fun proteinRatchetNeverDropsMidBlock() {
        // §3.4.1 — ewma 80 but block max was 85: protein stays pegged to 85.
        val out = TdeeCalculator.compute(
            inputs(settings = Settings(ratchetWeightKg = 85f))
        ) as TdeeOutcome.Adjusted
        assertEquals(85f, out.newRatchetKg, 0.001f)
        assertEquals(1.8f * 85f, out.proteinG, 0.01f)
    }

    @Test
    fun ratchetRisesWithNewMaxEwma() {
        val out = TdeeCalculator.compute(
            inputs(settings = Settings(ratchetWeightKg = 78f))
        ) as TdeeOutcome.Adjusted
        assertEquals(80f, out.newRatchetKg, 0.001f)
    }

    @Test
    fun fatFloorBindsAtPointSevenPerKg() {
        // 100kg, 2000 kcal maintain: 30% = 66.7g but floor = max(44.4, 70) = 70g.
        val out = TdeeCalculator.compute(
            inputs(
                dailyKcal = week(2000f),
                weights = flatWeights(100f),
                settings = Settings(goalRatePercent = 0f),
            )
        ) as TdeeOutcome.Adjusted
        assertEquals(70f, out.fatG, 0.1f)
    }

    @Test
    fun stallNoteAfterThreeContradictingWeeks() {
        val note = TdeeCalculator.stallNote(listOf(-0.4f, 0.1f, 0.0f, -0.02f), -0.5f)
        assertNotNull(note)
        assertNull(TdeeCalculator.stallNote(listOf(0.1f, 0.0f, -0.4f), -0.5f))
        assertNull(TdeeCalculator.stallNote(listOf(0.1f, 0.0f), -0.5f))
        assertNull(TdeeCalculator.stallNote(listOf(0.1f, 0.2f, 0.3f), 0f))
        // Gain goal contradicted by flat/falling weight.
        assertNotNull(TdeeCalculator.stallNote(listOf(0.0f, -0.1f, 0.02f), 0.25f))
    }

    @Test
    fun midweekRefreshOffersOnTwoPercentMove() {
        assertTrue(TdeeCalculator.shouldOfferMidweekRefresh(80f, 82f))
        assertFalse(TdeeCalculator.shouldOfferMidweekRefresh(80f, 81f))
        assertFalse(TdeeCalculator.shouldOfferMidweekRefresh(null, 82f))
        assertFalse(TdeeCalculator.shouldOfferMidweekRefresh(80f, null))
    }

    @Test
    fun ewmaSeriesMatchesSpecFormula() {
        val series = TdeeCalculator.ewmaSeries(
            listOf(WeightPoint(weekStart, 80f), WeightPoint(weekStart.plusDays(1), 84f))
        )
        assertEquals(80f, series[0].kg, 0.001f)
        assertEquals(81f, series[1].kg, 0.001f)
    }
}
