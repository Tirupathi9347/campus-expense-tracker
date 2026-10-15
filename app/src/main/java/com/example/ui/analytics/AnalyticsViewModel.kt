package com.example.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferencesRepository
import com.example.ui.badges.BadgeCalculators
import com.example.ui.badges.SpendingBadge
import com.example.ui.dashboard.BurnRateForecast
import com.example.ui.dashboard.CategoryBudgetItem
import com.example.ui.dashboard.CategorySpendItem
import com.example.ui.dashboard.ExpenseCalculators
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val allTimeTotal: Double = 0.0,
    val currentMonthTotal: Double = 0.0,
    val averageTransaction: Double = 0.0,
    val highestExpense: Expense? = null,
    val totalTransactions: Int = 0,
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val categoryBudgetItems: List<CategoryBudgetItem> = emptyList(),
    val burnRateForecast: BurnRateForecast = BurnRateForecast(),
    val spendingBadges: List<SpendingBadge> = emptyList(),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = false
)

class AnalyticsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        expenseRepository.getAllExpensesStream(),
        expenseRepository.getAllCategoryBudgetsStream(),
        userPreferencesRepository.userPreferencesFlow
    ) { allExpenses, categoryBudgets, preferences ->
        val total = ExpenseCalculators.calculateTotal(allExpenses)
        val currentMonthExpenses = ExpenseCalculators.filterExpensesForCurrentMonth(allExpenses)
        val currentMonthTotal = ExpenseCalculators.calculateTotal(currentMonthExpenses)
        val avg = if (allExpenses.isNotEmpty()) total / allExpenses.size else 0.0
        val highest = allExpenses.maxByOrNull { it.amount }
        val categoryBreakdown = ExpenseCalculators.buildCategoryBreakdown(allExpenses)

        val budgetMap = categoryBudgets.associate { it.categoryName to it.budgetLimit }
        // Evaluate budget caps against current month spending, showing all categories so caps can be configured
        val categoryBudgetItems = ExpenseCalculators.buildCategoryBudgetItems(
            expenses = currentMonthExpenses,
            budgetMap = budgetMap,
            includeAllCategories = true
        )

        val daysElapsed = DateUtils.getDaysElapsedInMonth()
        val daysRemaining = DateUtils.getDaysRemainingInMonth()
        val totalDaysInMonth = DateUtils.getTotalDaysInMonth()
        val burnRateForecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = preferences.monthlyBudget,
            monthlySpent = currentMonthTotal,
            daysElapsed = daysElapsed,
            calendarDaysRemaining = daysRemaining,
            currencySymbol = preferences.currencySymbol
        )
        val spendingBadges = BadgeCalculators.evaluateBadges(
            allExpenses = allExpenses,
            currentMonthExpenses = currentMonthExpenses,
            monthlyBudget = preferences.monthlyBudget,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth,
            burnRateForecast = burnRateForecast,
            categoryBudgetItems = categoryBudgetItems,
            currencySymbol = preferences.currencySymbol
        )

        AnalyticsUiState(
            allTimeTotal = total,
            currentMonthTotal = currentMonthTotal,
            averageTransaction = avg,
            highestExpense = highest,
            totalTransactions = allExpenses.size,
            categoryBreakdown = categoryBreakdown,
            categoryBudgetItems = categoryBudgetItems,
            burnRateForecast = burnRateForecast,
            spendingBadges = spendingBadges,
            currencySymbol = preferences.currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun setCategoryBudget(category: ExpenseCategory, limit: Double) {
        viewModelScope.launch {
            if (limit > 0) {
                expenseRepository.setCategoryBudget(category.name, limit)
            } else {
                expenseRepository.deleteCategoryBudget(category.name)
            }
        }
    }

    fun deleteCategoryBudget(category: ExpenseCategory) {
        viewModelScope.launch {
            expenseRepository.deleteCategoryBudget(category.name)
        }
    }
}
