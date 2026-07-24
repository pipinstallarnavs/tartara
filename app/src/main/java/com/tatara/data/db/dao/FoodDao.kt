package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.SavedMeal
import com.tatara.data.db.entity.SavedMealItem
import java.time.Instant
import java.time.LocalDate

data class EntryWithFood(
    @Embedded val entry: FoodEntry,
    @Relation(parentColumn = "foodId", entityColumn = "id") val food: Food,
)

@Dao
interface FoodDao {
    @Insert suspend fun insert(food: Food): Long
    @Insert suspend fun insertAll(foods: List<Food>)
    @Update suspend fun update(food: Food)
    @Query("SELECT * FROM food ORDER BY id") suspend fun getAll(): List<Food>
    @Query("SELECT COUNT(*) FROM food") suspend fun count(): Int
    @Query("UPDATE food SET useCount = useCount + 1, lastUsedAt = :at WHERE id = :id")
    suspend fun bumpUsage(id: Long, at: Instant)
    @Query("SELECT * FROM food WHERE useCount > 0 ORDER BY useCount DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun mostUsed(limit: Int): List<Food>
    @Query("DELETE FROM food") suspend fun deleteAll()

    @Insert suspend fun insertEntry(entry: FoodEntry): Long
    @Insert suspend fun insertEntries(entries: List<FoodEntry>)
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY id")
    suspend fun entriesOn(date: LocalDate): List<FoodEntry>
    @Transaction
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY id")
    suspend fun entriesWithFoodOn(date: LocalDate): List<EntryWithFood>
    @Transaction
    @Query("SELECT * FROM food_entry WHERE date BETWEEN :from AND :to ORDER BY id")
    suspend fun entriesWithFoodBetween(from: LocalDate, to: LocalDate): List<EntryWithFood>
    @Query("SELECT MIN(date) FROM food_entry") suspend fun firstEntryDate(): LocalDate?
    @Query("SELECT * FROM food_entry WHERE foodId = :foodId ORDER BY id DESC LIMIT 1")
    suspend fun lastEntryFor(foodId: Long): FoodEntry?
    @Query("DELETE FROM food_entry WHERE id = :id") suspend fun deleteEntry(id: Long)
    @Query("SELECT * FROM food_entry ORDER BY id") suspend fun getAllEntries(): List<FoodEntry>
    @Query("DELETE FROM food_entry") suspend fun deleteAllEntries()

    @Insert suspend fun insertSavedMeal(meal: SavedMeal): Long
    @Insert suspend fun insertSavedMeals(meals: List<SavedMeal>)
    @Query("SELECT * FROM saved_meal ORDER BY id") suspend fun getAllSavedMeals(): List<SavedMeal>
    @Query("DELETE FROM saved_meal") suspend fun deleteAllSavedMeals()

    @Insert suspend fun insertSavedMealItems(items: List<SavedMealItem>)
    @Query("SELECT * FROM saved_meal_item ORDER BY id")
    suspend fun getAllSavedMealItems(): List<SavedMealItem>
    @Query("DELETE FROM saved_meal_item") suspend fun deleteAllSavedMealItems()
}
