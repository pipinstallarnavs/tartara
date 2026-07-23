package com.tatara.data.food

import com.tatara.data.db.entity.FatSource
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroMathTest {

    @Test fun gramFoodScalesPer100g() {
        val rice = Food(
            name = "rice", unitType = UnitType.GRAM, kcal = 130f, protein = 2.7f,
            carbs = 28.2f, fat = 0.3f, fatSource = FatSource.GRAIN_LEGUME,
        )
        val m = MacroMath.macrosFor(rice, 60f)
        assertEquals(78f, m.kcal, 0.01f)
        assertEquals(1.62f, m.protein, 0.01f)
        assertEquals(16.92f, m.carbs, 0.01f)
        assertEquals(0.18f, m.fat, 0.01f)
    }

    @Test fun portionFoodScalesPerPortion() {
        val daal = Food(
            name = "daal", unitType = UnitType.PORTION, portionName = "katori",
            kcal = 180f, protein = 9f, carbs = 22f, fat = 6f, fatSource = FatSource.GRAIN_LEGUME,
        )
        val m = MacroMath.macrosFor(daal, 2f)
        assertEquals(360f, m.kcal, 0.01f)
        assertEquals(18f, m.protein, 0.01f)
    }

    @Test fun satFatIsDerivedFromFatSource() {
        // §3.3 — DAIRY derives at 65% of total fat.
        val paneer = Food(
            name = "paneer", unitType = UnitType.GRAM, kcal = 296f, protein = 18f,
            carbs = 6f, fat = 22f, fatSource = FatSource.DAIRY,
        )
        val m = MacroMath.macrosFor(paneer, 100f)
        assertEquals(22f * 0.65f, m.satFat, 0.001f)
    }

    @Test fun totalsSum() {
        val a = MacroTotals(100f, 10f, 5f, 2f, 1f)
        val b = MacroTotals(50f, 5f, 20f, 8f, 2f)
        assertEquals(MacroTotals(150f, 15f, 25f, 10f, 3f), a + b)
    }
}
