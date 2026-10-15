package com.example.data

import android.content.Context

/**
 * Dependency Injection container at the application level.
 * Direct implementation of the manual DI pattern taught in
 * Android Basics with Compose Unit 5 & 6.
 */
interface AppContainer {
    val expenseRepository: ExpenseRepository
    val userPreferencesRepository: UserPreferencesRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: ExpenseDatabase by lazy {
        ExpenseDatabase.getDatabase(context)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        OfflineExpenseRepository(
            expenseDao = database.expenseDao(),
            categoryBudgetDao = database.categoryBudgetDao()
        )
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }
}
