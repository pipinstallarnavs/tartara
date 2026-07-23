package com.tatara.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AppThemeTest {

    @Test
    fun everyThemeHasNineTierNames() {
        AppTheme.entries.forEach { theme ->
            val names = (0..8).map { theme.tierName(it) }
            assertEquals(9, names.distinct().size)
        }
    }

    @Test
    fun bandBoundariesMatchSpec() {
        // §7.3 — 1–11, 12–22, …, 78–88, 89–100.
        assertEquals(0, bandIndexForLevel(1))
        assertEquals(0, bandIndexForLevel(11))
        assertEquals(1, bandIndexForLevel(12))
        assertEquals(3, bandIndexForLevel(34))
        assertEquals(4, bandIndexForLevel(45))
        assertEquals(7, bandIndexForLevel(88))
        assertEquals(8, bandIndexForLevel(89))
        assertEquals(8, bandIndexForLevel(100))
        assertThrows(IllegalArgumentException::class.java) { bandIndexForLevel(0) }
        assertThrows(IllegalArgumentException::class.java) { bandIndexForLevel(101) }
    }

    @Test
    fun tierNamesVaryOnlyByTheme() {
        assertEquals("Tamahagane", AppTheme.NIE.tierName(0))
        assertEquals("Kokuhō", AppTheme.NIE.tierName(8))
        assertEquals("Agneyastra", AppTheme.ASTRA.tierName(0))
        assertEquals("Narayanastra", AppTheme.ASTRA.tierName(8))
        assertEquals("Ārambha", AppTheme.SAMSKARA.tierName(0))
        assertEquals("Svabhāva", AppTheme.SAMSKARA.tierName(8))
    }
}
