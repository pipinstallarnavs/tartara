package com.tatara.data.train

import com.tatara.data.db.entity.RoutineItem
import com.tatara.data.db.entity.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val item = RoutineItem(
        id = 1, routineId = 1, exerciseId = 1, targetSets = 3, sortOrder = 0,
        repRangeLow = 8, repRangeHigh = 10, incrementKg = 2.5f, currentWeightKg = 60f,
    )

    private fun set(reps: Int, weight: Float = 60f, warmup: Boolean = false) = SetEntry(
        sessionId = 1, exerciseId = 1, routineItemId = 1, setIndex = 0,
        weightKg = weight, reps = reps, isWarmup = warmup,
    )

    @Test
    fun epleyE1rm() {
        // 100 × (1 + 5/30) = 116.67
        assertEquals(116.67f, Progression.e1rm(100f, 5), 0.01f)
        assertEquals(100f, Progression.e1rm(100f, 0), 0.01f)
    }

    @Test
    fun incrementOnlyWhenAllWorkingSetsHitTheTop() {
        assertTrue(Progression.shouldIncrement(item, listOf(set(10), set(10), set(10))))
        assertTrue(Progression.shouldIncrement(item, listOf(set(10), set(11), set(12))))
        assertFalse(Progression.shouldIncrement(item, listOf(set(10), set(10), set(9))))
    }

    @Test
    fun incrementNeedsTheFullSetCount() {
        // Two brilliant sets of a 3-set slot are not a trigger.
        assertFalse(Progression.shouldIncrement(item, listOf(set(12), set(12))))
        assertFalse(Progression.shouldIncrement(item, emptyList()))
    }

    @Test
    fun stallNeedsThreeFlatSessions() {
        assertTrue(Progression.stalled(listOf(100f, 100f, 100f)))
        assertTrue(Progression.stalled(listOf(100f, 98f, 99f)))
        assertFalse(Progression.stalled(listOf(100f, 101f, 102f)))
        // Improvement inside the window is not a stall, even below an old PR.
        assertFalse(Progression.stalled(listOf(110f, 100f, 101f, 102f)))
        assertTrue(Progression.stalled(listOf(110f, 102f, 101f, 102f)))
        assertFalse(Progression.stalled(listOf(100f, 100f)))
    }

    @Test
    fun plateStackPerSide() {
        // 100kg on a 20kg bar → 40 per side → 25 + 15.
        assertEquals(listOf(25f, 15f), Progression.plateStack(100f))
        // 102.5 → 41.25 → 25 + 15 + 1.25.
        assertEquals(listOf(25f, 15f, 1.25f), Progression.plateStack(102.5f))
        // Bare bar.
        assertEquals(emptyList<Float>(), Progression.plateStack(20f))
        // 101 is not representable with standard plates; below the bar is nonsense.
        assertNull(Progression.plateStack(101f))
        assertNull(Progression.plateStack(15f))
    }
}
