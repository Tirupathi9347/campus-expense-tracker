package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a spending budget limit for a specific category.
 * Implements Feature 1: Per-Category Budget Caps.
 */
@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey
    val categoryName: String,
    val budgetLimit: Double
)
