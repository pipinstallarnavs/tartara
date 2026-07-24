package com.tatara.data.sleep

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepMathTest {

    @Test
    fun durationCrossesMidnight() {
        // §6.3 — duration = wake − bed − timeToFallAsleep.
        assertEquals(465, SleepMath.durationMinutes(LocalTime.of(23, 0), LocalTime.of(7, 0), 15))
        assertEquals(420, SleepMath.durationMinutes(LocalTime.of(1, 0), LocalTime.of(8, 0), 0))
        assertEquals(0, SleepMath.durationMinutes(LocalTime.of(23, 0), LocalTime.of(23, 0), 10))
    }

    @Test
    fun shiftedScalePutsEarlyHoursAfterMidnightHours() {
        // 23:50 and 00:10 are 20 minutes apart, not 23 hours.
        assertEquals(
            20,
            SleepMath.shiftedMinutes(LocalTime.of(0, 10)) - SleepMath.shiftedMinutes(LocalTime.of(23, 50)),
        )
    }

    @Test
    fun midpointOnShiftedScale() {
        // Bed 23:00, 480 min asleep → midpoint 03:00 = 1620 shifted minutes.
        assertEquals(1620f, SleepMath.midpointMinutes(LocalTime.of(23, 0), 480), 0.01f)
    }

    @Test
    fun regularityFollowsSpecFormula() {
        // §6.3 — 100 − sd × 1.5. Two midpoints ±30 around the mean → sd 30 → 55.
        assertEquals(55f, SleepMath.regularity(listOf(1500f, 1560f))!!, 0.01f)
        // Identical nights → 100.
        assertEquals(100f, SleepMath.regularity(listOf(1530f, 1530f, 1530f))!!, 0.01f)
        // Wild spread clamps at 0, never negative.
        assertEquals(0f, SleepMath.regularity(listOf(1200f, 1500f))!!, 0.01f)
        // One night is not a pattern.
        assertNull(SleepMath.regularity(listOf(1500f)))
    }

    @Test
    fun curfewComparisonWrapsMidnight() {
        val start = LocalTime.of(21, 30)
        assertTrue(SleepMath.heldCurfew(LocalTime.of(21, 10), start))
        assertTrue(SleepMath.heldCurfew(LocalTime.of(21, 30), start))
        assertFalse(SleepMath.heldCurfew(LocalTime.of(22, 15), start))
        // 00:30 is after a 21:30 curfew, not before it.
        assertFalse(SleepMath.heldCurfew(LocalTime.of(0, 30), start))
    }

    @Test
    fun comparisonMeansAndSuppression() {
        val pairs = List(6) { true to 4 } + List(5) { false to 3 }
        val comp = SleepMath.compare("caffeine", pairs)!!
        assertEquals(4f, comp.yesMean, 0.01f)
        assertEquals(6, comp.yesN)
        assertEquals(3f, comp.noMean, 0.01f)
        assertEquals(5, comp.noN)

        // §6.5 — under 5 observations on either side, say nothing.
        assertNull(SleepMath.compare("x", List(4) { true to 4 } + List(10) { false to 3 }))
        assertNull(SleepMath.compare("x", List(10) { true to 4 } + List(4) { false to 3 }))
    }
}
