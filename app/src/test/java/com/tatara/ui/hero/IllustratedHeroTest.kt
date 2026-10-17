package com.tatara.ui.hero

import com.tatara.data.dashboard.Levels
import org.junit.Assert.assertEquals
import org.junit.Test

class IllustratedHeroTest {
    @Test
    fun everyHeldLevelSelectsADistinctPermanentArtState() {
        val states = (1..100).map { level ->
            val tier = Levels.bandFor(level)
            tier to refinementStep(level, tier)
        }
        assertEquals(100, states.distinct().size)
        assertEquals(0 to 0, states.first())
        assertEquals(0 to 10, states[10])
        assertEquals(1 to 0, states[11])
        assertEquals(8 to 10, states[98])
        assertEquals(8 to 11, states.last())
    }
}
