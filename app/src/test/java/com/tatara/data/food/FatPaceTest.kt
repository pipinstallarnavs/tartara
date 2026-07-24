package com.tatara.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The §3.6 table, including its boundaries. */
class FatPaceTest {

    @Test
    fun statesFollowRemainingDensity() {
        assertEquals(FatPaceState.FAT_LIGHT, FatPace.state(50f, 1000f))   // 0.050
        assertEquals(FatPaceState.ON_PACE, FatPace.state(30f, 1000f))     // 0.030
        assertEquals(FatPaceState.FAT_LOADED, FatPace.state(15f, 1000f))  // 0.015
        assertEquals(FatPaceState.SPENT, FatPace.state(11f, 1000f))       // 0.011
        assertEquals(FatPaceState.OVER, FatPace.state(-1f, 1000f))
    }

    @Test
    fun boundaries() {
        assertEquals(FatPaceState.ON_PACE, FatPace.state(45f, 1000f))     // 0.045 is not >
        assertEquals(FatPaceState.ON_PACE, FatPace.state(20f, 1000f))     // 0.020 inclusive
        assertEquals(FatPaceState.FAT_LOADED, FatPace.state(12f, 1000f))  // 0.012 inclusive
    }

    @Test
    fun noCaloriesLeftButFatRemaining_isSpent() {
        assertEquals(FatPaceState.SPENT, FatPace.state(10f, 0f))
        assertEquals(FatPaceState.SPENT, FatPace.state(10f, -50f))
    }

    @Test
    fun overOnFatWinsRegardlessOfCalories() {
        assertEquals(FatPaceState.OVER, FatPace.state(-5f, -100f))
    }

    @Test
    fun messagesMatchSpec() {
        assertEquals("Room for fat — add oil or nuts", FatPaceState.FAT_LIGHT.message)
        assertNull(FatPaceState.ON_PACE.message)
        assertEquals("Lean choices from here", FatPaceState.FAT_LOADED.message)
        assertEquals("Chicken and rice territory", FatPaceState.SPENT.message)
        assertEquals("Over on fat — tomorrow's problem", FatPaceState.OVER.message)
    }

    @Test
    fun proteinMirrorFlagsWhenBehind() {
        // Target density 150/3000 = 0.05; flag above 1.5× that.
        assertTrue(FatPace.proteinBehind(80f, 1000f, 150f, 3000f))   // 0.080
        assertFalse(FatPace.proteinBehind(60f, 1000f, 150f, 3000f))  // 0.060
        assertTrue(FatPace.proteinBehind(10f, 0f, 150f, 3000f))      // kcal gone, protein left
        assertFalse(FatPace.proteinBehind(0f, 500f, 150f, 3000f))    // protein done
    }
}
