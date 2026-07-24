package com.tatara.data.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticityTest {

    private fun afterNCompletions(n: Int): Float {
        var a = 0f
        repeat(n) { a = Automaticity.afterCompletion(a) }
        return a
    }

    @Test
    fun growthMatchesTheEvidenceCurve() {
        // §5.4 — ≈50% at 15 days, ≈80% at 35, ≈95% at 65; never reaches 100.
        assertTrue(afterNCompletions(15) in 49.5f..50.5f)
        assertTrue(afterNCompletions(35) in 79.5f..80.5f)
        assertTrue(afterNCompletions(65) in 94.5f..95.5f)
        assertTrue(afterNCompletions(1000) < 100f)
    }

    @Test
    fun earlyRepsBuyMoreThanLateOnes() {
        val early = Automaticity.afterCompletion(10f) - 10f
        val late = Automaticity.afterCompletion(80f) - 80f
        assertTrue(early > late)
    }

    @Test
    fun penaltyEscalatesWithConsecutiveMisses() {
        assertEquals(0f, Automaticity.penaltyFor(1), 0f)
        assertEquals(0.02f, Automaticity.penaltyFor(2), 0f)
        assertEquals(0.05f, Automaticity.penaltyFor(3), 0f)
        assertEquals(0.08f, Automaticity.penaltyFor(4), 0f)
        assertEquals(0.08f, Automaticity.penaltyFor(9), 0f)
    }

    @Test
    fun firstMissIsFree() {
        assertEquals(60f, Automaticity.afterMiss(60f, 1), 0.001f)
        assertEquals(60f * 0.98f, Automaticity.afterMiss(60f, 2), 0.001f)
        assertEquals(60f * 0.92f, Automaticity.afterMiss(60f, 5), 0.001f)
    }

    @Test
    fun stageBandsMatchSpec() {
        // §5.5 — 0–29 / 30–59 / 60–84 / 85–100.
        assertEquals(HabitStage.INITIATION, Automaticity.stage(0f))
        assertEquals(HabitStage.INITIATION, Automaticity.stage(29.9f))
        assertEquals(HabitStage.LEARNING, Automaticity.stage(30f))
        assertEquals(HabitStage.LEARNING, Automaticity.stage(59.9f))
        assertEquals(HabitStage.STABILISING, Automaticity.stage(60f))
        assertEquals(HabitStage.STABILISING, Automaticity.stage(84.9f))
        assertEquals(HabitStage.AUTOMATIC, Automaticity.stage(85f))
        assertEquals(HabitStage.AUTOMATIC, Automaticity.stage(100f))
    }
}
