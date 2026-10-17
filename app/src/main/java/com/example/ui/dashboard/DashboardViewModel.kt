package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferencesRepository
import com.example.ui.badges.BadgeCalculators
import com.example.ui.badges.SpendingBadge
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val currentMonthTotal: Double = 0.0,
    val monthlyBudget: Double = 5000.0,
    val remainingBudget: Double = 5000.0,
    val budgetProgressRatio: Float = 0f,
    val isOverBudget: Boolean = false,
    val daysRemainingInMonth: Int = 1,
    val dailyAllowance: Double = 0.0,
    val dailyAllowanceStatus: DailyAllowanceStatus = DailyAllowanceStatus.ON_TRACK,
    val dailyAllowanceMessage: String = "",
    val monthlyTransactionCount: Int = 0,
    val allTimeTransactionCount: Int = 0,
    val recentExpenses: List<Expense> = emptyList(),
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val categoryBudgetItems: List<CategoryBudgetItem> = emptyList(),
    val topCategory: ExpenseCategory? = null,
    val currencySymbol: String = "₹",
    val campusName: String = "University Campus",
    val currentMonthName: String = "",
    val isLoading: Boolean = false,
    val burnRateForecast: BurnRateForecast = BurnRateForecast(),
    val spendingBadges: List<SpendingBadge> = emptyList()
)

class DashboardViewModel(
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        expenseRepository.getAllExpensesStream(),
        expenseRepository.getAllCategoryBudgetsStream(),
        userPreferencesRepository.userPreferencesFlow
    ) { allExpenses, categoryBudgets, preferences ->
        val currentMonthExpenses = ExpenseCalculators.filterExpensesForCurrentMonth(allExpenses)
        val monthlySpent = ExpenseCalculators.calculateTotal(currentMonthExpenses)
        val budget = preferences.monthlyBudget
        val remaining = ExpenseCalculators.calculateRemainingBudget(budget, monthlySpent)
        val progress = ExpenseCalculators.calculateBudgetProgress(budget, monthlySpent)
        val categoryBreakdown = ExpenseCalculators.buildCategoryBreakdown(currentMonthExpenses)
        val budgetMap = categoryBudgets.associate { it.categoryName to it.budgetLimit }
        val categoryBudgetItems = ExpenseCalculators.buildCategoryBudgetItems(currentMonthExpenses, budgetMap)

        val daysElapsed = DateUtils.getDaysElapsedInMonth()
        val daysRemaining = DateUtils.getDaysRemainingInMonth()
        val totalDaysInMonth = DateUtils.getTotalDaysInMonth()
        val allowanceInfo = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = budget,
            totalSpent = monthlySpent,
            daysRemaining = daysRemaining
        )
        val burnRateForecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = budget,
            monthlySpent = monthlySpent,
            daysElapsed = daysElapsed,
            calendarDaysRemaining = daysRemaining,
            currencySymbol = preferences.currencySymbol
        )
        val spendingBadges = BadgeCalculators.evaluateBadges(
            allExpenses = allExpenses,
            currentMonthExpenses = currentMonthExpenses,
            monthlyBudget = budget,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth,
            burnRateForecast = burnRateForecast,
            categoryBudgetItems = categoryBudgetItems,
            currencySymbol = preferences.currencySymbol
        )

        DashboardUiState(
            currentMonthTotal = monthlySpent,
            monthlyBudget = budget,
            remainingBudget = remaining,
            budgetProgressRatio = progress,
            isOverBudget = monthlySpent > budget,
            daysRemainingInMonth = allowanceInfo.daysRemaining,
            dailyAllowance = allowanceInfo.dailyAllowance,
            dailyAllowanceStatus = allowanceInfo.status,
            dailyAllowanceMessage = allowanceInfo.statusMessage,
            monthlyTransactionCount = currentMonthExpenses.size,
            allTimeTransactionCount = allExpenses.size,
            recentExpenses = allExpenses.take(5),
            categoryBreakdown = categoryBreakdown,
            categoryBudgetItems = categoryBudgetItems,
            topCategory = categoryBreakdown.firstOrNull()?.category,
            currencySymbol = preferences.currencySymbol,
            campusName = preferences.campusName,
            currentMonthName = DateUtils.formatMonthYear(System.currentTimeMillis()),
            isLoading = false,
            burnRateForecast = burnRateForecast,
            spendingBadges = spendingBadges
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(
            currentMonthName = DateUtils.formatMonthYear(System.currentTimeMillis()),
            daysRemainingInMonth = DateUtils.getDaysRemainingInMonth(),
            isLoading = true
        )
    )

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }
}
