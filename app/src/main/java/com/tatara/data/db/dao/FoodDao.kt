package com.tatara.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tatara.data.db.entity.Food
import com.tatara.data.db.entity.FoodEntry
import com.tatara.data.db.entity.SavedMeal
import com.tatara.data.db.entity.SavedMealItem
import java.time.LocalDate

@Dao
interface FoodDao {
    @Insert suspend fun insert(food: Food): Long
    @Insert suspend fun insertAll(foods: List<Food>)
    @Update suspend fun update(food: Food)
    @Query("SELECT * FROM food ORDER BY id") suspend fun getAll(): List<Food>
    @Query("DELETE FROM food") suspend fun deleteAll()

    @Insert suspend fun insertEntry(entry: FoodEntry): Long
    @Insert suspend fun insertEntries(entries: List<FoodEntry>)
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY id")
    suspend fun entriesOn(date: LocalDate): List<FoodEntry>
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
