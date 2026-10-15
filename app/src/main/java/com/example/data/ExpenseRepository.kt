package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data access operations for expenses and category budget caps.
 * Represents the repository pattern taught in Android Basics with Compose Unit 6.
 */
interface ExpenseRepository {
    fun getAllExpensesStream(): Flow<List<Expense>>
    fun getExpenseStream(id: Long): Flow<Expense?>
    suspend fun getExpenseById(id: Long): Expense?
    fun getExpensesBetweenDatesStream(startDateMillis: Long, endDateMillis: Long): Flow<List<Expense>>
    fun getExpensesByCategoryStream(category: String): Flow<List<Expense>>
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun deleteExpenseById(id: Long)
    suspend fun clearAllExpenses()

    // Per-Category Budget Caps operations
    fun getAllCategoryBudgetsStream(): Flow<List<CategoryBudget>>
    fun getCategoryBudgetStream(categoryName: String): Flow<CategoryBudget?>
    suspend fun setCategoryBudget(categoryName: String, limit: Double)
    suspend fun deleteCategoryBudget(categoryName: String)
}

/**
 * Concrete implementation of ExpenseRepository utilizing Room DAOs.
 */
class OfflineExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryBudgetDao: CategoryBudgetDao
) : ExpenseRepository {

    override fun getAllExpensesStream(): Flow<List<Expense>> =
        expenseDao.getAllExpenses()

    override fun getExpenseStream(id: Long): Flow<Expense?> =
        expenseDao.getExpenseStream(id)

    override suspend fun getExpenseById(id: Long): Expense? =
        expenseDao.getExpenseById(id)

    override fun getExpensesBetweenDatesStream(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<List<Expense>> =
        expenseDao.getExpensesBetweenDates(startDateMillis, endDateMillis)

    override fun getExpensesByCategoryStream(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    override suspend fun insertExpense(expense: Expense): Long =
        expenseDao.insert(expense)

    override suspend fun updateExpense(expense: Expense) =
        expenseDao.update(expense)

    override suspend fun deleteExpense(expense: Expense) =
        expenseDao.delete(expense)

    override suspend fun deleteExpenseById(id: Long) =
        expenseDao.deleteById(id)

    override suspend fun clearAllExpenses() =
        expenseDao.clearAll()

    // Per-Category Budget Caps implementations
    override fun getAllCategoryBudgetsStream(): Flow<List<CategoryBudget>> =
        categoryBudgetDao.getAllCategoryBudgetsStream()

    override fun getCategoryBudgetStream(categoryName: String): Flow<CategoryBudget?> =
        categoryBudgetDao.getCategoryBudgetStream(categoryName)

    override suspend fun setCategoryBudget(categoryName: String, limit: Double) =
        categoryBudgetDao.setCategoryBudget(CategoryBudget(categoryName = categoryName, budgetLimit = limit))

    override suspend fun deleteCategoryBudget(categoryName: String) =
        categoryBudgetDao.deleteCategoryBudget(categoryName)
}
