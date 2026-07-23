package com.tatara.data.food

import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.UnitType

/** satFat is derived (§3.3), displayed with a ~ prefix, never entered. */
data class MacroTotals(
    val kcal: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val satFat: Float = 0f,
) {
    operator fun plus(other: MacroTotals) = MacroTotals(
        kcal + other.kcal,
        protein + other.protein,
        carbs + other.carbs,
        fat + other.fat,
        satFat + other.satFat,
    )
}

object MacroMath {
    /** quantity is grams for GRAM foods (macros per 100g), portions for PORTION foods. */
    fun macrosFor(food: Food, quantity: Float): MacroTotals {
        val factor = when (food.unitType) {
            UnitType.GRAM -> quantity / 100f
            UnitType.PORTION -> quantity
        }
        val fat = food.fat * factor
        return MacroTotals(
            kcal = food.kcal * factor,
            protein = food.protein * factor,
            carbs = food.carbs * factor,
            fat = fat,
            satFat = fat * food.fatSource.satFraction,
        )
    }
}
