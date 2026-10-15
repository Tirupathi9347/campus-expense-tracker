package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryBudget
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferencesRepository
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.ui.badges.BadgeCalculators
import com.example.ui.badges.SpendingBadge
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategorySpendItem(
    val category: ExpenseCategory,
    val amount: Double,
    val percentageOfTotal: Float,
    val transactionCount: Int
)

enum class CategoryBudgetStatus {
    NO_BUDGET,
    ON_TRACK,
    WARNING,  // >= 80% and <= 100%
    EXCEEDED  // > 100%
}

data class CategoryBudgetItem(
    val category: ExpenseCategory,
    val amountSpent: Double,
    val budgetLimit: Double?,
    val usageRatio: Float,
    val status: CategoryBudgetStatus,
    val transactionCount: Int
)

data class CategoryCapImpact(
    val category: ExpenseCategory,
    val budgetLimit: Double?,
    val currentSpent: Double,
    val projectedSpent: Double,
    val status: CategoryBudgetStatus,
    val message: String
)

enum class DailyAllowanceStatus {
    ON_TRACK,
    TIGHT,
    EXHAUSTED,
    OVERSPENT
}

data class DailyAllowanceInfo(
    val dailyAllowance: Double,
    val daysRemaining: Int,
    val remainingBudget: Double,
    val status: DailyAllowanceStatus,
    val statusMessage: String
)

enum class BurnRateStatus {
    INSUFFICIENT_DATA,
    SUSTAINABLE,
    ON_TRACK,
    EARLY_RUNOUT,
    EXHAUSTED,
    OVER_BUDGET
}

data class BurnRateForecast(
    val dailyBurnRate: Double = 0.0,
    val estimatedDaysRemaining: Int = 0,
    val calendarDaysRemaining: Int = 0,
    val daysElapsed: Int = 0,
    val runoutDateMillis: Long? = null,
    val formattedRunoutDate: String = "",
    val status: BurnRateStatus = BurnRateStatus.INSUFFICIENT_DATA,
    val statusHeadline: String = "No Spending Yet",
    val statusMessage: String = "",
    val isEstimateNotice: String = "Forecast is an estimate based on current spending pace, not a guaranteed prediction."
)

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

/**
 * Calculation helper for student expense summaries, dynamic daily allowance,
 * and per-category budget caps.
 * Extracted so it can be thoroughly unit tested independently.
 */
object ExpenseCalculators {

    fun calculateTotal(expenses: List<Expense>): Double {
        return expenses.sumOf { it.amount }
    }

    fun calculateRemainingBudget(budget: Double, totalSpent: Double): Double {
        return budget - totalSpent
    }

    fun calculateBudgetProgress(budget: Double, totalSpent: Double): Float {
        if (budget <= 0.0) return 0f
        return (totalSpent / budget).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Evaluates budget cap status showing warning at 80% and exceeded at 100%.
     */
    fun evaluateCategoryBudgetStatus(spent: Double, limit: Double?): CategoryBudgetStatus {
        if (limit == null || limit <= 0.0) return CategoryBudgetStatus.NO_BUDGET
        val ratio = spent / limit
        return when {
            ratio > 1.0 -> CategoryBudgetStatus.EXCEEDED
            ratio >= 0.80 -> CategoryBudgetStatus.WARNING
            else -> CategoryBudgetStatus.ON_TRACK
        }
    }

    /**
     * Builds list of category budget items comparing actual spent vs caps.
     * When includeAllCategories is true, includes all categories so caps can be set in advance.
     */
    fun buildCategoryBudgetItems(
        expenses: List<Expense>,
        budgetMap: Map<String, Double>,
        includeAllCategories: Boolean = false
    ): List<CategoryBudgetItem> {
        val expenseMap = expenses.groupBy { it.categoryEnum }

        return ExpenseCategory.entries.mapNotNull { cat ->
            val list = expenseMap[cat] ?: emptyList()
            val spent = list.sumOf { it.amount }
            val limit = budgetMap[cat.name]

            if (!includeAllCategories && spent == 0.0 && (limit == null || limit <= 0.0)) {
                null
            } else {
                val ratio = if (limit != null && limit > 0.0) {
                    (spent / limit).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                CategoryBudgetItem(
                    category = cat,
                    amountSpent = spent,
                    budgetLimit = limit,
                    usageRatio = ratio,
                    status = evaluateCategoryBudgetStatus(spent, limit),
                    transactionCount = list.size
                )
            }
        }.sortedWith(
            compareByDescending<CategoryBudgetItem> { it.status == CategoryBudgetStatus.EXCEEDED }
                .thenByDescending { it.status == CategoryBudgetStatus.WARNING }
                .thenByDescending { it.budgetLimit != null && it.budgetLimit > 0.0 }
                .thenByDescending { it.amountSpent }
        )
    }

    /**
     * Calculates the projected impact of a pending/entered expense on the category's budget cap.
     */
    fun calculateCategoryCapImpact(
        category: ExpenseCategory,
        enteredAmount: Double,
        currentSpent: Double,
        budgetLimit: Double?,
        currencySymbol: String = "₹"
    ): CategoryCapImpact {
        if (budgetLimit == null || budgetLimit <= 0.0) {
            return CategoryCapImpact(
                category = category,
                budgetLimit = null,
                currentSpent = currentSpent,
                projectedSpent = currentSpent + enteredAmount,
                status = CategoryBudgetStatus.NO_BUDGET,
                message = "No monthly budget cap set for ${category.displayName}."
            )
        }

        val projected = currentSpent + enteredAmount
        val status = evaluateCategoryBudgetStatus(projected, budgetLimit)
        val formattedLimit = CurrencyFormatter.format(budgetLimit, currencySymbol)

        val message = when (status) {
            CategoryBudgetStatus.EXCEEDED -> {
                val over = projected - budgetLimit
                val formattedOver = CurrencyFormatter.format(over, currencySymbol)
                "This will exceed your ${category.displayName} cap by $formattedOver (Cap: $formattedLimit)."
            }
            CategoryBudgetStatus.WARNING -> {
                val left = (budgetLimit - projected).coerceAtLeast(0.0)
                val formattedLeft = CurrencyFormatter.format(left, currencySymbol)
                val pct = ((projected / budgetLimit) * 100).toInt()
                "Warning: This will reach $pct% of your ${category.displayName} cap ($formattedLeft remaining)."
            }
            CategoryBudgetStatus.ON_TRACK -> {
                val left = (budgetLimit - projected).coerceAtLeast(0.0)
                val formattedLeft = CurrencyFormatter.format(left, currencySymbol)
                "On track: $formattedLeft will remain under your ${category.displayName} cap."
            }
            CategoryBudgetStatus.NO_BUDGET -> ""
        }

        return CategoryCapImpact(
            category = category,
            budgetLimit = budgetLimit,
            currentSpent = currentSpent,
            projectedSpent = projected,
            status = status,
            message = message
        )
    }

    /**
     * Calculates the daily spending allowance from remaining budget and days left.
     * Handles zero days, non-positive remaining budget, and negative inputs safely.
     */
    fun calculateDailyAllowance(remainingBudget: Double, daysRemaining: Int): Double {
        if (remainingBudget <= 0.0 || daysRemaining <= 0) {
            return 0.0
        }
        return remainingBudget / daysRemaining
    }

    /**
     * Evaluates comprehensive daily allowance status and user guidance message.
     * Gracefully handles empty data (0 spent), exhausted budget, overspending, and zero days.
     */
    fun calculateDailyAllowanceInfo(
        monthlyBudget: Double,
        totalSpent: Double,
        daysRemaining: Int
    ): DailyAllowanceInfo {
        val remaining = calculateRemainingBudget(monthlyBudget, totalSpent)
        val safeDays = daysRemaining.coerceAtLeast(0)

        if (safeDays == 0) {
            return DailyAllowanceInfo(
                dailyAllowance = 0.0,
                daysRemaining = 0,
                remainingBudget = remaining,
                status = if (remaining < 0.0) DailyAllowanceStatus.OVERSPENT else DailyAllowanceStatus.EXHAUSTED,
                statusMessage = "Current month has ended."
            )
        }

        if (remaining < 0.0) {
            return DailyAllowanceInfo(
                dailyAllowance = 0.0,
                daysRemaining = safeDays,
                remainingBudget = remaining,
                status = DailyAllowanceStatus.OVERSPENT,
                statusMessage = "Over budget. Suggested daily allowance suspended."
            )
        }

        if (remaining == 0.0 || monthlyBudget <= 0.0) {
            return DailyAllowanceInfo(
                dailyAllowance = 0.0,
                daysRemaining = safeDays,
                remainingBudget = 0.0,
                status = DailyAllowanceStatus.EXHAUSTED,
                statusMessage = "Monthly budget completely exhausted for this month."
            )
        }

        val allowance = remaining / safeDays
        val isTight = allowance < 5.0 || (monthlyBudget > 0 && (remaining / monthlyBudget) < 0.15)

        return DailyAllowanceInfo(
            dailyAllowance = allowance,
            daysRemaining = safeDays,
            remainingBudget = remaining,
            status = if (isTight) DailyAllowanceStatus.TIGHT else DailyAllowanceStatus.ON_TRACK,
            statusMessage = if (isTight) {
                "Budget running low. Suggested strict daily spending limit."
            } else {
                "Safe daily spending allowance to finish the month within budget."
            }
        )
    }

    fun filterExpensesForCurrentMonth(expenses: List<Expense>, referenceMillis: Long = System.currentTimeMillis()): List<Expense> {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val targetYear = cal.get(Calendar.YEAR)
        val targetMonth = cal.get(Calendar.MONTH)

        return expenses.filter { expense ->
            val expCal = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
            expCal.get(Calendar.YEAR) == targetYear && expCal.get(Calendar.MONTH) == targetMonth
        }
    }

    fun buildCategoryBreakdown(expenses: List<Expense>): List<CategorySpendItem> {
        val total = calculateTotal(expenses)
        if (expenses.isEmpty() || total <= 0.0) return emptyList()

        return expenses
            .groupBy { it.categoryEnum }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySpendItem(
                    category = cat,
                    amount = sum,
                    percentageOfTotal = (sum / total).toFloat(),
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.amount }
    }

    /**
     * Calculates the average daily spending rate (burn rate) from historical spending over elapsed days.
     * Gracefully handles zero spending, non-positive elapsed days, or negative amounts.
     */
    fun calculateDailyBurnRate(totalSpent: Double, daysElapsed: Int): Double {
        if (totalSpent <= 0.0 || daysElapsed <= 0) return 0.0
        return totalSpent / daysElapsed
    }

    /**
     * Calculates the overall average daily spending from a collection of historical expenses
     * across their full calendar span.
     */
    fun calculateHistoricalAverageDailySpending(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val total = calculateTotal(expenses)
        if (total <= 0.0) return 0.0
        val minDate = expenses.minOf { it.dateMillis }
        val maxDate = expenses.maxOf { it.dateMillis }
        val diffMillis = (maxDate - minDate).coerceAtLeast(0L)
        val spanDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt() + 1
        return total / spanDays.coerceAtLeast(1)
    }

    /**
     * Estimates how many days the remaining budget will last at the current daily burn rate,
     * comparing the pace against the remaining calendar days in the month.
     *
     * Handles zero spending, insufficient history, exhausted budget, overspending,
     * and month boundaries safely.
     */
    fun calculateBudgetRunoutForecast(
        monthlyBudget: Double,
        monthlySpent: Double,
        daysElapsed: Int,
        calendarDaysRemaining: Int,
        currencySymbol: String = "₹",
        referenceMillis: Long = System.currentTimeMillis()
    ): BurnRateForecast {
        val remainingBudget = monthlyBudget - monthlySpent
        val safeDaysElapsed = daysElapsed.coerceAtLeast(1)
        val safeCalendarDays = calendarDaysRemaining.coerceAtLeast(0)

        // Case 1: Insufficient history or zero spending
        if (monthlySpent <= 0.0) {
            return BurnRateForecast(
                dailyBurnRate = 0.0,
                estimatedDaysRemaining = 0,
                calendarDaysRemaining = safeCalendarDays,
                daysElapsed = safeDaysElapsed,
                runoutDateMillis = null,
                formattedRunoutDate = "No forecast yet",
                status = BurnRateStatus.INSUFFICIENT_DATA,
                statusHeadline = "Insufficient Spending History",
                statusMessage = "No expenses recorded this month yet. Spending history is required to estimate budget runout."
            )
        }

        val dailyBurnRate = calculateDailyBurnRate(monthlySpent, safeDaysElapsed)

        // Case 2: Over budget (remaining < 0)
        if (remainingBudget < 0.0) {
            val overAmount = -remainingBudget
            val formattedOver = CurrencyFormatter.format(overAmount, currencySymbol)
            return BurnRateForecast(
                dailyBurnRate = dailyBurnRate,
                estimatedDaysRemaining = 0,
                calendarDaysRemaining = safeCalendarDays,
                daysElapsed = safeDaysElapsed,
                runoutDateMillis = referenceMillis,
                formattedRunoutDate = "Already Exceeded",
                status = BurnRateStatus.OVER_BUDGET,
                statusHeadline = "Budget Exceeded",
                statusMessage = "Monthly budget has been exceeded by $formattedOver. Runout has already occurred."
            )
        }

        // Case 3: Exhausted budget (remaining == 0 or monthlyBudget <= 0)
        if (remainingBudget == 0.0 || monthlyBudget <= 0.0) {
            return BurnRateForecast(
                dailyBurnRate = dailyBurnRate,
                estimatedDaysRemaining = 0,
                calendarDaysRemaining = safeCalendarDays,
                daysElapsed = safeDaysElapsed,
                runoutDateMillis = referenceMillis,
                formattedRunoutDate = "Budget Exhausted",
                status = BurnRateStatus.EXHAUSTED,
                statusHeadline = "Budget Exhausted",
                statusMessage = "Monthly budget is 100% used. Any additional expenses will result in overspending."
            )
        }

        // Case 4: Month concluded
        if (safeCalendarDays == 0) {
            return BurnRateForecast(
                dailyBurnRate = dailyBurnRate,
                estimatedDaysRemaining = 0,
                calendarDaysRemaining = 0,
                daysElapsed = safeDaysElapsed,
                runoutDateMillis = referenceMillis,
                formattedRunoutDate = "Month Ended",
                status = BurnRateStatus.SUSTAINABLE,
                statusHeadline = "Month Concluded",
                statusMessage = "The billing month has concluded within your allocated budget."
            )
        }

        // Case 5: Active spending with remaining budget
        val estimatedDays = (remainingBudget / dailyBurnRate).toInt()
        val runoutMillis = DateUtils.addDaysToTimestamp(estimatedDays, referenceMillis)
        val formattedRunout = DateUtils.formatDayMonth(runoutMillis)
        val formattedRate = CurrencyFormatter.format(dailyBurnRate, currencySymbol)
        val formattedRemaining = CurrencyFormatter.format(remainingBudget, currencySymbol)

        return when {
            // Runs out before month ends
            estimatedDays < safeCalendarDays -> {
                val daysShort = safeCalendarDays - estimatedDays
                BurnRateForecast(
                    dailyBurnRate = dailyBurnRate,
                    estimatedDaysRemaining = estimatedDays,
                    calendarDaysRemaining = safeCalendarDays,
                    daysElapsed = safeDaysElapsed,
                    runoutDateMillis = runoutMillis,
                    formattedRunoutDate = "Est. ~$formattedRunout (~$estimatedDays d)",
                    status = BurnRateStatus.EARLY_RUNOUT,
                    statusHeadline = "Early Runout Risk",
                    statusMessage = "At your current spending rate of $formattedRate/day, your remaining $formattedRemaining is estimated to run out in ~$estimatedDays day${if (estimatedDays != 1) "s" else ""} (around $formattedRunout), which is $daysShort day${if (daysShort != 1) "s" else ""} before month end."
                )
            }
            // Runs out roughly at the same time as month end (within 2 days)
            estimatedDays <= safeCalendarDays + 2 -> {
                BurnRateForecast(
                    dailyBurnRate = dailyBurnRate,
                    estimatedDaysRemaining = estimatedDays,
                    calendarDaysRemaining = safeCalendarDays,
                    daysElapsed = safeDaysElapsed,
                    runoutDateMillis = runoutMillis,
                    formattedRunoutDate = "Month End (~$formattedRunout)",
                    status = BurnRateStatus.ON_TRACK,
                    statusHeadline = "Balanced Spending Pace",
                    statusMessage = "At your current spending rate of $formattedRate/day, your remaining budget is projected to last ~$estimatedDays days, carrying you safely to month end."
                )
            }
            // Runs out past month end with a cushion
            else -> {
                val projectedSurplus = (remainingBudget - (dailyBurnRate * safeCalendarDays)).coerceAtLeast(0.0)
                val formattedSurplus = CurrencyFormatter.format(projectedSurplus, currencySymbol)
                BurnRateForecast(
                    dailyBurnRate = dailyBurnRate,
                    estimatedDaysRemaining = estimatedDays,
                    calendarDaysRemaining = safeCalendarDays,
                    daysElapsed = safeDaysElapsed,
                    runoutDateMillis = runoutMillis,
                    formattedRunoutDate = "Surplus to Month End",
                    status = BurnRateStatus.SUSTAINABLE,
                    statusHeadline = "Sustainable Pace",
                    statusMessage = "Your current spending rate of $formattedRate/day is well within budget. You are on track to finish the month with an estimated surplus of $formattedSurplus."
                )
            }
        }
    }
}

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
