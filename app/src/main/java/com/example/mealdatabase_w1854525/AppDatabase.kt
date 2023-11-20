package com.example.mealdatabase_w1854525

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Meal::class], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun mealDao(): MealDao
}