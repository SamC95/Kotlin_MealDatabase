package com.example.mealdatabase_w1854525

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MealDao {
    @Query("Select * from Meal")
    suspend fun getAll(): List<Meal>

    @Query("SELECT * FROM Meal WHERE meal LIKE :mealText OR ingredient1 LIKE :mealText OR " +
            "ingredient2 LIKE :mealText OR ingredient3 LIKE :mealText OR ingredient4 LIKE :mealText " +
            "OR ingredient5 LIKE :mealText OR ingredient6 LIKE :mealText OR ingredient7 LIKE :mealText " +
            "OR ingredient8 LIKE :mealText OR ingredient9 LIKE :mealText OR ingredient10 LIKE :mealText " +
            "OR ingredient11 LIKE :mealText OR ingredient12 LIKE :mealText OR ingredient13 LIKE :mealText " +
            "OR ingredient14 LIKE :mealText OR ingredient15 LIKE :mealText OR ingredient16 LIKE :mealText " +
            "OR ingredient17 LIKE :mealText OR ingredient18 LIKE :mealText OR ingredient19 LIKE :mealText " +
            "OR ingredient20 LIKE :mealText")
    fun findMealByNameOrIngredient(mealText: String): MutableList<Meal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(vararg meal: Meal)

    @Insert
    suspend fun insertAll(vararg meals: Meal)
}