@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class, InstantSerializer::class)

package com.tatara.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tatara.data.db.InstantSerializer
import com.tatara.data.db.LocalDateSerializer
import com.tatara.data.db.LocalTimeSerializer
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** §3.2 — kcal/protein/carbs/fat are per 100g if GRAM, per 1 portion if PORTION. */
@Serializable
@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aliases: String = "",
    val isCustom: Boolean = false,
    val isEstimated: Boolean = false,
    val unitType: UnitType,
    val portionName: String? = null,
    val kcal: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fatSource: FatSource,
    val lastUsedAt: Instant? = null,
    val useCount: Int = 0,
)

enum class UnitType { GRAM, PORTION }

/** §3.3 — saturated fat is never entered; it is derived as fat × satFraction. */
enum class FatSource(val satFraction: Float) {
    COCONUT_PALM(0.85f),
    DAIRY(0.65f),
    RED_MEAT(0.40f),
    EGG(0.32f),
    POULTRY(0.30f),
    FISH(0.25f),
    GRAIN_LEGUME(0.20f),
    SEED_OIL(0.15f),
    NUTS(0.12f),
    MIXED(0.35f),
}

/** One logged food. quantity is grams for GRAM foods, portion count for PORTION foods. */
@Serializable
@Entity(
    tableName = "food_entry",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("foodId"), Index("date")],
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val foodId: Long,
    val quantity: Float,
    val loggedAt: Instant,
)

/** §3.1 — a named set of entries ("post-gym"), logged as one item. */
@Serializable
@Entity(tableName = "saved_meal")
data class SavedMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Serializable
@Entity(
    tableName = "saved_meal_item",
    foreignKeys = [
        ForeignKey(
            entity = SavedMeal::class,
            parentColumns = ["id"],
            childColumns = ["savedMealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("savedMealId"), Index("foodId")],
)
data class SavedMealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedMealId: Long,
    val foodId: Long,
    val quantity: Float,
)
