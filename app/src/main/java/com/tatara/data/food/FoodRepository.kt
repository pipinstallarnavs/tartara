package com.tatara.data.food

import com.tatara.data.EditWindow
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.dao.EntryWithFood
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.UnitType
import java.time.Instant
import java.time.LocalDate

sealed interface LogResult {
    data class Logged(val entry: FoodEntry, val food: Food) : LogResult
    /** §3.1 — ambiguity is resolved by an inline chip row, never a dialog. */
    data class Ambiguous(val candidates: List<Food>, val quantity: Float?) : LogResult
    /** §3.1 — offers "Create <query>?" opening the custom food form pre-filled. */
    data class NoMatch(val query: String) : LogResult
    data object OutsideEditWindow : LogResult
    data object EmptyInput : LogResult
}

class FoodRepository(
    private val db: TataraDatabase,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Instant = { Instant.now() },
) {

    suspend fun log(text: String, date: LocalDate = today()): LogResult {
        if (text.isBlank()) return LogResult.EmptyInput
        if (!EditWindow.isEditable(date, today())) return LogResult.OutsideEditWindow

        val parsed = FoodInputParser.parse(text)
        return when (val match = FoodMatcher(db.foodDao().getAll()).match(parsed)) {
            is MatchResult.Resolved -> LogResult.Logged(
                addEntry(match.food, match.quantity ?: defaultQuantity(match.food), date),
                match.food,
            )
            is MatchResult.Ambiguous -> LogResult.Ambiguous(match.candidates, match.quantity)
            is MatchResult.NoMatch -> LogResult.NoMatch(match.query)
        }
    }

    /** Direct add — used by disambiguation chips, Recent row, and repeat features. */
    suspend fun addEntry(food: Food, quantity: Float, date: LocalDate = today()): FoodEntry {
        require(EditWindow.isEditable(date, today())) { "outside the D-2 edit window" }
        val entry = FoodEntry(date = date, foodId = food.id, quantity = quantity, loggedAt = now())
        val id = db.foodDao().insertEntry(entry)
        db.foodDao().bumpUsage(food.id, now())
        return entry.copy(id = id)
    }

    suspend fun deleteEntry(entry: FoodEntry) {
        require(EditWindow.isEditable(entry.date, today())) { "outside the D-2 edit window" }
        db.foodDao().deleteEntry(entry.id)
    }

    /** §3.2 — custom foods are flat: name + four macros + unit type. */
    suspend fun createCustomFood(
        name: String,
        unitType: UnitType,
        portionName: String?,
        kcal: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        fatSource: com.tatara.data.db.entity.FatSource,
        isEstimated: Boolean = false,
    ): Food {
        val food = Food(
            name = name.trim(), isCustom = true, isEstimated = isEstimated,
            unitType = unitType, portionName = portionName,
            kcal = kcal, protein = protein, carbs = carbs, fat = fat, fatSource = fatSource,
        )
        return food.copy(id = db.foodDao().insert(food))
    }

    suspend fun entriesOn(date: LocalDate = today()): List<EntryWithFood> =
        db.foodDao().entriesWithFoodOn(date)

    suspend fun totalsOn(date: LocalDate = today()): MacroTotals =
        entriesOn(date).fold(MacroTotals()) { acc, e -> acc + MacroMath.macrosFor(e.food, e.entry.quantity) }

    /** §3.1 — the Recent row: the 15 most-logged foods. */
    suspend fun recentFoods(limit: Int = 15): List<Food> = db.foodDao().mostUsed(limit)

    /** Targets currently in effect — null until the first Sunday adjustment lands. */
    suspend fun latestTargets(): com.tatara.data.db.entity.TargetAdjustment? =
        db.bodyDao().latestAdjustment()

    /** §3.1 — "Repeat yesterday": clones the previous day's log wholesale. */
    suspend fun repeatDay(from: LocalDate, to: LocalDate = today()): Int {
        require(EditWindow.isEditable(to, today())) { "outside the D-2 edit window" }
        val entries = db.foodDao().entriesOn(from)
        db.foodDao().insertEntries(
            entries.map { FoodEntry(date = to, foodId = it.foodId, quantity = it.quantity, loggedAt = now()) }
        )
        return entries.size
    }

    /** Last-used quantity for one-tap re-add; sensible defaults otherwise. */
    private suspend fun defaultQuantity(food: Food): Float =
        db.foodDao().lastEntryFor(food.id)?.quantity
            ?: if (food.unitType == UnitType.PORTION) 1f else 100f
}
