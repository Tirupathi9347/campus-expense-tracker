package com.example.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.CampusExpenseApplication
import com.example.ui.analytics.AnalyticsViewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.entry.ExpenseEntryViewModel
import com.example.ui.history.TransactionHistoryViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.splitbill.SplitBillViewModel

/**
 * Provides Factory to create instances of ViewModels for the entire Campus Expense Tracker.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                expenseRepository = campusExpenseApplication().container.expenseRepository,
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository
            )
        }
        initializer {
            ExpenseEntryViewModel(
                savedStateHandle = this.createSavedStateHandle(),
                expenseRepository = campusExpenseApplication().container.expenseRepository,
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository
            )
        }
        initializer {
            TransactionHistoryViewModel(
                expenseRepository = campusExpenseApplication().container.expenseRepository,
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository
            )
        }
        initializer {
            AnalyticsViewModel(
                expenseRepository = campusExpenseApplication().container.expenseRepository,
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository
            )
        }
        initializer {
            SettingsViewModel(
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository,
                expenseRepository = campusExpenseApplication().container.expenseRepository
            )
        }
        initializer {
            SplitBillViewModel(
                userPreferencesRepository = campusExpenseApplication().container.userPreferencesRepository,
                expenseRepository = campusExpenseApplication().container.expenseRepository
            )
        }
    }
}

/**
 * Extension function to queries for [CampusExpenseApplication] object and returns an instance of
 * [CampusExpenseApplication].
 */
fun CreationExtras.campusExpenseApplication(): CampusExpenseApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CampusExpenseApplication)
