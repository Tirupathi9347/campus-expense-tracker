package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing an expense entry.
 * Implements Android Basics with Compose Unit 6: Data Persistence.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val category: String,
    val dateMillis: Long,
    val note: String = ""
) {
    /**
     * Resolves the associated category enum safely.
     */
    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromName(category)
}
