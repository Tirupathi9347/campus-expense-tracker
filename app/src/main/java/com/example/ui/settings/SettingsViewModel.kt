package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SettingsUiState(
    val userPreferences: UserPreferences = UserPreferences(),
    val budgetInput: String = "600",
    val statusMessage: String? = null,
    val isLoading: Boolean = false
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userPreferencesRepository.userPreferencesFlow
        .map { prefs ->
            SettingsUiState(
                userPreferences = prefs,
                budgetInput = prefs.monthlyBudget.toInt().toString(),
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(isLoading = true)
        )

    fun updateBudget(amountString: String) {
        val amount = amountString.toDoubleOrNull()
        if (amount != null && amount > 0) {
            viewModelScope.launch {
                userPreferencesRepository.saveMonthlyBudget(amount)
            }
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveCurrencySymbol(symbol)
        }
    }

    fun updateCampusName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveCampusName(name)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            expenseRepository.clearAllExpenses()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -2)
            val threeDaysAgo = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -4)
            val weekAgo = calendar.timeInMillis

            val sampleList = listOf(
                Expense(
                    title = "University Bookstore Textbooks",
                    amount = 135.00,
                    category = ExpenseCategory.BOOKS.name,
                    dateMillis = now,
                    note = "Physics & Calculus textbooks"
                ),
                Expense(
                    title = "Campus Dining Hall Meal",
                    amount = 16.50,
                    category = ExpenseCategory.FOOD.name,
                    dateMillis = now,
                    note = "Lunch with chemistry lab partner"
                ),
                Expense(
                    title = "Monthly Student Commuter Pass",
                    amount = 45.00,
                    category = ExpenseCategory.TRANSPORT.name,
                    dateMillis = yesterday,
                    note = "Subsidized regional bus pass"
                ),
                Expense(
                    title = "Dormitory Laundry Card Refill",
                    amount = 20.00,
                    category = ExpenseCategory.HOUSING.name,
                    dateMillis = threeDaysAgo,
                    note = "Campus residence laundry"
                ),
                Expense(
                    title = "Student Union Coffee & Bagel",
                    amount = 7.75,
                    category = ExpenseCategory.FOOD.name,
                    dateMillis = threeDaysAgo,
                    note = "Midterm morning breakfast"
                ),
                Expense(
                    title = "Campus Gym Fitness Pass",
                    amount = 30.00,
                    category = ExpenseCategory.HEALTH.name,
                    dateMillis = weekAgo,
                    note = "Semester student recreation center"
                ),
                Expense(
                    title = "Cloud Storage & IDE Student Subscription",
                    amount = 9.99,
                    category = ExpenseCategory.TECH.name,
                    dateMillis = weekAgo,
                    note = "Software engineering coursework tools"
                )
            )

            for (expense in sampleList) {
                expenseRepository.insertExpense(expense)
            }
        }
    }
}
