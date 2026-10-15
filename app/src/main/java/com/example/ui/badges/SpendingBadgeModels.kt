package com.example.ui.badges

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.Expense
import com.example.ui.dashboard.BurnRateForecast
import com.example.ui.dashboard.BurnRateStatus
import com.example.ui.dashboard.CategoryBudgetItem
import com.example.ui.dashboard.CategoryBudgetStatus
import com.example.ui.dashboard.ExpenseCalculators
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar

/**
 * Identifiers for all rule-based student spending badges.
 */
enum class BadgeId {
    THRIFTY_WEEK,
    UNDER_BUDGET,
    BUDGET_CHAMPION,
    CAP_CRUSADER,
    CONSISTENT_TRACKER
}

/**
 * Model representing a smart spending achievement badge.
 */
data class SpendingBadge(
    val id: BadgeId,
    val title: String,
    val description: String,
    val isEarned: Boolean,
    val earnedReason: String,
    val criteriaExplanation: String,
    val progressRatio: Float,
    val progressText: String,
    val icon: ImageVector,
    val badgeColor: Color
)

/**
 * Pure calculation logic for evaluating student achievement badges.
 * Ensures transparent, testable, deterministic rules without duplicate awards.
 */
object BadgeCalculators {

    /**
     * Evaluates Thrifty Week achievement:
     * - Requires at least 7 days elapsed in the month (incomplete week handling).
     * - Requires at least 1 expense logged in the 7-day period (empty transaction handling).
     * - 7-day spending must be <= weekly budget (monthlyBudget * 7 / totalDaysInMonth).
     */
    fun evaluateThriftyWeek(
        currentMonthExpenses: List<Expense>,
        monthlyBudget: Double,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        currencySymbol: String = "₹",
        referenceMillis: Long = System.currentTimeMillis()
    ): SpendingBadge {
        val safeTotalDays = totalDaysInMonth.coerceAtLeast(28)
        val weeklyBudget = (monthlyBudget * 7.0) / safeTotalDays
        val formattedWeeklyBudget = CurrencyFormatter.format(weeklyBudget, currencySymbol)

        val windowStart = DateUtils.getSevenDayWindowStartMillis(referenceMillis)
        val pastWeekExpenses = currentMonthExpenses.filter { it.dateMillis in windowStart..referenceMillis }
        val pastWeekSpent = ExpenseCalculators.calculateTotal(pastWeekExpenses)
        val formattedWeekSpent = CurrencyFormatter.format(pastWeekSpent, currencySymbol)

        val criteria = "Spend $formattedWeeklyBudget or less across any complete 7-day window with active tracking."

        if (daysElapsed < 7) {
            val ratio = (daysElapsed / 7f).coerceIn(0f, 1f)
            return SpendingBadge(
                id = BadgeId.THRIFTY_WEEK,
                title = "Thrifty Week",
                description = "Kept weekly spending within prorated budget",
                isEarned = false,
                earnedReason = "Incomplete week: $daysElapsed of 7 days elapsed in this cycle. Complete a full 7-day period under $formattedWeeklyBudget to qualify.",
                criteriaExplanation = criteria,
                progressRatio = ratio,
                progressText = "$daysElapsed / 7 days elapsed",
                icon = Icons.Default.DateRange,
                badgeColor = Color(0xFF0D9488)
            )
        }

        if (pastWeekExpenses.isEmpty()) {
            return SpendingBadge(
                id = BadgeId.THRIFTY_WEEK,
                title = "Thrifty Week",
                description = "Kept weekly spending within prorated budget",
                isEarned = false,
                earnedReason = "No expenses logged in the past 7 days. Active expense tracking is required.",
                criteriaExplanation = criteria,
                progressRatio = 0f,
                progressText = "0 txns in past 7 days",
                icon = Icons.Default.DateRange,
                badgeColor = Color(0xFF0D9488)
            )
        }

        val isUnderWeekly = pastWeekSpent <= weeklyBudget
        val progressRatio = if (weeklyBudget > 0) (pastWeekSpent / weeklyBudget).toFloat().coerceIn(0f, 1f) else 0f

        return SpendingBadge(
            id = BadgeId.THRIFTY_WEEK,
            title = "Thrifty Week",
            description = "Kept weekly spending within prorated budget",
            isEarned = isUnderWeekly,
            earnedReason = if (isUnderWeekly) {
                "Awarded: Spent $formattedWeekSpent in the past 7 days, staying under your weekly cap of $formattedWeeklyBudget."
            } else {
                "Past 7-day spending ($formattedWeekSpent) exceeded weekly budget threshold of $formattedWeeklyBudget."
            },
            criteriaExplanation = criteria,
            progressRatio = if (isUnderWeekly) 1f else progressRatio,
            progressText = "$formattedWeekSpent / $formattedWeeklyBudget",
            icon = Icons.Default.DateRange,
            badgeColor = Color(0xFF0D9488)
        )
    }

    /**
     * Evaluates Under Budget achievement:
     * - Requires at least 3 expenses recorded in the month.
     * - Monthly spent <= monthly budget, with budget > 0.
     */
    fun evaluateUnderBudget(
        monthlySpent: Double,
        monthlyBudget: Double,
        transactionCount: Int,
        currencySymbol: String = "₹"
    ): SpendingBadge {
        val formattedBudget = CurrencyFormatter.format(monthlyBudget, currencySymbol)
        val formattedSpent = CurrencyFormatter.format(monthlySpent, currencySymbol)
        val criteria = "Maintain monthly spending within $formattedBudget with at least 3 recorded expenses."

        if (transactionCount < 3 || monthlySpent <= 0.0) {
            val ratio = (transactionCount / 3f).coerceIn(0f, 1f)
            return SpendingBadge(
                id = BadgeId.UNDER_BUDGET,
                title = "Under Budget",
                description = "Kept total monthly expenses within budget",
                isEarned = false,
                earnedReason = "Log at least 3 campus expenses to demonstrate active tracking under your $formattedBudget budget.",
                criteriaExplanation = criteria,
                progressRatio = ratio,
                progressText = "$transactionCount / 3 transactions",
                icon = Icons.Default.AccountBalanceWallet,
                badgeColor = Color(0xFF2563EB)
            )
        }

        val isUnder = monthlyBudget > 0.0 && monthlySpent <= monthlyBudget
        val ratio = if (monthlyBudget > 0) (monthlySpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 1f

        return SpendingBadge(
            id = BadgeId.UNDER_BUDGET,
            title = "Under Budget",
            description = "Kept total monthly expenses within budget",
            isEarned = isUnder,
            earnedReason = if (isUnder) {
                val remaining = monthlyBudget - monthlySpent
                val formattedRemaining = CurrencyFormatter.format(remaining, currencySymbol)
                "Awarded: Current monthly total is $formattedSpent, leaving $formattedRemaining under your $formattedBudget budget."
            } else {
                val over = monthlySpent - monthlyBudget
                val formattedOver = CurrencyFormatter.format(over, currencySymbol)
                "Monthly budget exceeded by $formattedOver ($formattedSpent spent vs $formattedBudget budget)."
            },
            criteriaExplanation = criteria,
            progressRatio = if (isUnder) (1f - ratio).coerceIn(0f, 1f) else 0f,
            progressText = "$formattedSpent / $formattedBudget",
            icon = Icons.Default.AccountBalanceWallet,
            badgeColor = Color(0xFF2563EB)
        )
    }

    /**
     * Evaluates Budget Champion achievement:
     * - High milestone achievement:
     * - Minimum 5 transactions and >= 5 days elapsed.
     * - Sustainable burn rate forecast.
     * - Monthly spending <= 75% of budget.
     * - No category budget cap exceeded.
     */
    fun evaluateBudgetChampion(
        monthlySpent: Double,
        monthlyBudget: Double,
        transactionCount: Int,
        daysElapsed: Int,
        burnRateForecast: BurnRateForecast,
        categoryBudgetItems: List<CategoryBudgetItem>,
        currencySymbol: String = "₹"
    ): SpendingBadge {
        val milestoneLimit = monthlyBudget * 0.75
        val formattedMilestone = CurrencyFormatter.format(milestoneLimit, currencySymbol)
        val criteria = "Maintain a Sustainable pace with <= 75% of budget spent ($formattedMilestone), at least 5 transactions across 5+ days, and 0 exceeded category caps."

        val hasEnoughHistory = transactionCount >= 5 && daysElapsed >= 5
        val isUnderMilestone = monthlyBudget > 0.0 && monthlySpent <= milestoneLimit
        val isSustainablePace = burnRateForecast.status == BurnRateStatus.SUSTAINABLE
        val hasNoExceededCaps = categoryBudgetItems.none { it.status == CategoryBudgetStatus.EXCEEDED }

        val isEarned = hasEnoughHistory && isUnderMilestone && isSustainablePace && hasNoExceededCaps

        val earnedReason = when {
            isEarned -> {
                "Awarded: Outstanding financial management! Spending is well within 75% of budget, forecast pace is Sustainable, and all category caps are respected."
            }
            !hasEnoughHistory -> {
                "Milestone in progress: Logged $transactionCount/5 expenses over $daysElapsed/5 elapsed days."
            }
            !isUnderMilestone -> {
                "Spending (${CurrencyFormatter.format(monthlySpent, currencySymbol)}) exceeds the 75% milestone threshold ($formattedMilestone)."
            }
            !isSustainablePace -> {
                "Spending pace is currently '${burnRateForecast.statusHeadline}'. A Sustainable pace is required."
            }
            !hasNoExceededCaps -> {
                "One or more category budget caps have been exceeded."
            }
            else -> "Criteria not fully met."
        }

        val historyRatio = ((transactionCount / 5f) * 0.5f) + ((daysElapsed.coerceAtMost(5) / 5f) * 0.5f)

        return SpendingBadge(
            id = BadgeId.BUDGET_CHAMPION,
            title = "Budget Champion",
            description = "Sustainable pace under 75% budget with clean category caps",
            isEarned = isEarned,
            earnedReason = earnedReason,
            criteriaExplanation = criteria,
            progressRatio = if (isEarned) 1f else historyRatio.coerceIn(0f, 1f),
            progressText = if (isEarned) "Milestone Achieved" else "$transactionCount/5 txns, $daysElapsed/5 days",
            icon = Icons.Default.EmojiEvents,
            badgeColor = Color(0xFFF59E0B)
        )
    }

    /**
     * Evaluates Cap Crusader achievement:
     * - Requires at least one configured category budget cap.
     * - Requires at least one transaction in a capped category.
     * - All capped categories are within limit (none EXCEEDED).
     */
    fun evaluateCapCrusader(
        categoryBudgetItems: List<CategoryBudgetItem>
    ): SpendingBadge {
        val cappedItems = categoryBudgetItems.filter { it.budgetLimit != null && it.budgetLimit > 0.0 }
        val criteria = "Configure category budget caps and keep all capped categories within their limits."

        if (cappedItems.isEmpty()) {
            return SpendingBadge(
                id = BadgeId.CAP_CRUSADER,
                title = "Cap Crusader",
                description = "Kept all category budget caps under limit",
                isEarned = false,
                earnedReason = "No category caps configured yet. Set a spending cap for any category in Analytics to qualify.",
                criteriaExplanation = criteria,
                progressRatio = 0f,
                progressText = "0 active caps",
                icon = Icons.Default.Security,
                badgeColor = Color(0xFF8B5CF6)
            )
        }

        val cappedWithActivity = cappedItems.filter { it.transactionCount > 0 }
        if (cappedWithActivity.isEmpty()) {
            return SpendingBadge(
                id = BadgeId.CAP_CRUSADER,
                title = "Cap Crusader",
                description = "Kept all category budget caps under limit",
                isEarned = false,
                earnedReason = "Caps configured for ${cappedItems.size} categories, but no transactions recorded in those categories yet.",
                criteriaExplanation = criteria,
                progressRatio = 0.5f,
                progressText = "${cappedItems.size} caps configured",
                icon = Icons.Default.Security,
                badgeColor = Color(0xFF8B5CF6)
            )
        }

        val hasExceeded = cappedItems.any { it.status == CategoryBudgetStatus.EXCEEDED }
        val isEarned = !hasExceeded

        return SpendingBadge(
            id = BadgeId.CAP_CRUSADER,
            title = "Cap Crusader",
            description = "Kept all category budget caps under limit",
            isEarned = isEarned,
            earnedReason = if (isEarned) {
                "Awarded: Successfully respected all ${cappedItems.size} configured category budget caps with active spending."
            } else {
                val exceededNames = cappedItems.filter { it.status == CategoryBudgetStatus.EXCEEDED }.joinToString { it.category.displayName }
                "One or more category caps exceeded ($exceededNames)."
            },
            criteriaExplanation = criteria,
            progressRatio = if (isEarned) 1f else 0.5f,
            progressText = if (isEarned) "${cappedItems.size} caps respected" else "Cap exceeded",
            icon = Icons.Default.Security,
            badgeColor = Color(0xFF8B5CF6)
        )
    }

    /**
     * Evaluates Consistent Tracker achievement:
     * - Requires at least 5 transactions across at least 3 distinct calendar days.
     */
    fun evaluateConsistentTracker(
        expenses: List<Expense>
    ): SpendingBadge {
        val criteria = "Log at least 5 transactions across 3 or more distinct calendar days."
        val count = expenses.size

        val distinctDays = expenses.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.toSet().size

        val isEarned = count >= 5 && distinctDays >= 3
        val ratio = (((count.coerceAtMost(5) / 5f) * 0.5f) + ((distinctDays.coerceAtMost(3) / 3f) * 0.5f)).coerceIn(0f, 1f)

        return SpendingBadge(
            id = BadgeId.CONSISTENT_TRACKER,
            title = "Consistent Tracker",
            description = "Built a steady habit of logging student expenses",
            isEarned = isEarned,
            earnedReason = if (isEarned) {
                "Awarded: Logged $count expenses across $distinctDays distinct days, demonstrating consistent financial tracking."
            } else {
                "Logged $count/5 expenses across $distinctDays/3 distinct days. Keep tracking to earn this badge."
            },
            criteriaExplanation = criteria,
            progressRatio = if (isEarned) 1f else ratio,
            progressText = "$count/5 txns, $distinctDays/3 days",
            icon = Icons.Default.Timeline,
            badgeColor = Color(0xFF10B981)
        )
    }

    /**
     * Master evaluation function returning the deterministic list of all badges.
     * Prevents duplicate awards because each badge has a unique ID and is calculated in real-time.
     */
    fun evaluateBadges(
        allExpenses: List<Expense>,
        currentMonthExpenses: List<Expense>,
        monthlyBudget: Double,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        burnRateForecast: BurnRateForecast,
        categoryBudgetItems: List<CategoryBudgetItem>,
        currencySymbol: String = "₹",
        referenceMillis: Long = System.currentTimeMillis()
    ): List<SpendingBadge> {
        val monthlySpent = ExpenseCalculators.calculateTotal(currentMonthExpenses)

        return listOf(
            evaluateThriftyWeek(
                currentMonthExpenses = currentMonthExpenses,
                monthlyBudget = monthlyBudget,
                daysElapsed = daysElapsed,
                totalDaysInMonth = totalDaysInMonth,
                currencySymbol = currencySymbol,
                referenceMillis = referenceMillis
            ),
            evaluateUnderBudget(
                monthlySpent = monthlySpent,
                monthlyBudget = monthlyBudget,
                transactionCount = currentMonthExpenses.size,
                currencySymbol = currencySymbol
            ),
            evaluateBudgetChampion(
                monthlySpent = monthlySpent,
                monthlyBudget = monthlyBudget,
                transactionCount = currentMonthExpenses.size,
                daysElapsed = daysElapsed,
                burnRateForecast = burnRateForecast,
                categoryBudgetItems = categoryBudgetItems,
                currencySymbol = currencySymbol
            ),
            evaluateCapCrusader(
                categoryBudgetItems = categoryBudgetItems
            ),
            evaluateConsistentTracker(
                expenses = allExpenses
            )
        )
    }
}
