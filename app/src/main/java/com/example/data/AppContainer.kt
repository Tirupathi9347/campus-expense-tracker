package com.example.data

import android.content.Context

/**
 * Dependency Injection container at the application level.
 * Provides application-wide repository dependencies.
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
