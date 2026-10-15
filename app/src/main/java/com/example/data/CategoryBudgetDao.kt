package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for per-category budget caps.
 */
@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets")
    fun getAllCategoryBudgetsStream(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE categoryName = :categoryName LIMIT 1")
    fun getCategoryBudgetStream(categoryName: String): Flow<CategoryBudget?>

    @Query("SELECT * FROM category_budgets WHERE categoryName = :categoryName LIMIT 1")
    suspend fun getCategoryBudget(categoryName: String): CategoryBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCategoryBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE categoryName = :categoryName")
    suspend fun deleteCategoryBudget(categoryName: String)

    @Query("DELETE FROM category_budgets")
    suspend fun clearAll()
}
