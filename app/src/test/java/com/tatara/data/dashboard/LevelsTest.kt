package com.tatara.data.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelsTest {

    @Test
    fun costAndTotalMatchSpec() {
        // §7.2 — cost(n) = 80 + 15n; total to level 100 = 83,750.
        assertEquals(95, Levels.cost(1))
        assertEquals(1580, Levels.cost(100))
        assertEquals(83750L, Levels.cumulative(100))
    }

    @Test
    fun levelBoundaries() {
        assertEquals(1, Levels.levelFor(0))
        assertEquals(1, Levels.levelFor(204))
        assertEquals(2, Levels.levelFor(205))       // 95 + 110
        assertEquals(100, Levels.levelFor(83750))
        assertEquals(100, Levels.levelFor(1_000_000))
    }

    @Test
    fun tierFloorsHoldTheLevel() {
        // §7.3 — reach Yaki-ire (band 3, floor 34) and the level can't display below 34.
        assertEquals(34, Levels.bandStartLevel(3))
        assertEquals(34, Levels.effectiveLevel(0, highestBandEntered = 3))
        val xpForL40 = Levels.cumulative(40)
        assertEquals(40, Levels.effectiveLevel(xpForL40, highestBandEntered = 3))
    }

    @Test
    fun nearFloorWarnsWithinTwoLevels() {
        assertTrue(Levels.nearFloor(34, 3))
        assertTrue(Levels.nearFloor(35, 3))
        assertFalse(Levels.nearFloor(36, 3))
        // The first band has no cliff below it.
        assertFalse(Levels.nearFloor(1, 0))
    }

    @Test
    fun xpToNextTier() {
        assertEquals(Levels.cumulative(12), Levels.xpToNextTier(0, 0))
        assertNull(Levels.xpToNextTier(83750, 8))
    }

    @Test
    fun workoutDiminishingReturns() {
        // §7.2 — dr(n) = 1/(1+0.35(n−1)): 30, 22, 18, 15.
        assertEquals(30, Levels.workoutXp(1))
        assertEquals(22, Levels.workoutXp(2))
        assertEquals(18, Levels.workoutXp(3))
        assertEquals(15, Levels.workoutXp(4))
    }
}
