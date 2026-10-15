package com.example

import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.ui.badges.BadgeCalculators
import com.example.ui.badges.BadgeId
import com.example.ui.dashboard.BurnRateForecast
import com.example.ui.dashboard.BurnRateStatus
import com.example.ui.dashboard.CategoryBudgetItem
import com.example.ui.dashboard.CategoryBudgetStatus
import com.example.ui.dashboard.DailyAllowanceStatus
import com.example.ui.dashboard.ExpenseCalculators
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests for expense calculation accuracy, monthly filtering, budget tracking,
 * and dynamic daily spending allowance.
 * Maps to Unit 4 & Unit 7 of Android Basics with Compose.
 */
class ExpenseCalculationTest {

    @Test
    fun calculateTotal_emptyList_returnsZero() {
        val total = ExpenseCalculators.calculateTotal(emptyList())
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun calculateTotal_sumsCorrectly() {
        val expenses = listOf(
            Expense(1, "Textbooks", 120.50, ExpenseCategory.BOOKS.name, System.currentTimeMillis()),
            Expense(2, "Lunch", 15.25, ExpenseCategory.FOOD.name, System.currentTimeMillis()),
            Expense(3, "Bus Pass", 45.00, ExpenseCategory.TRANSPORT.name, System.currentTimeMillis())
        )
        val total = ExpenseCalculators.calculateTotal(expenses)
        assertEquals(180.75, total, 0.001)
    }

    @Test
    fun calculateRemainingBudget_withinBudget() {
        val remaining = ExpenseCalculators.calculateRemainingBudget(600.0, 180.0)
        assertEquals(420.0, remaining, 0.001)
    }

    @Test
    fun calculateRemainingBudget_overBudget_returnsNegative() {
        val remaining = ExpenseCalculators.calculateRemainingBudget(500.0, 650.0)
        assertEquals(-150.0, remaining, 0.001)
    }

    @Test
    fun calculateBudgetProgress_clampsBetweenZeroAndOne() {
        assertEquals(0.5f, ExpenseCalculators.calculateBudgetProgress(500.0, 250.0), 0.001f)
        assertEquals(1.0f, ExpenseCalculators.calculateBudgetProgress(500.0, 600.0), 0.001f)
        assertEquals(0.0f, ExpenseCalculators.calculateBudgetProgress(0.0, 100.0), 0.001f)
    }

    @Test
    fun filterExpensesForCurrentMonth_excludesOtherMonths() {
        val cal = Calendar.getInstance()
        val currentMonthMillis = cal.timeInMillis

        // Two months ago
        cal.add(Calendar.MONTH, -2)
        val oldMillis = cal.timeInMillis

        val expenses = listOf(
            Expense(1, "Current Month Textbook", 80.0, ExpenseCategory.BOOKS.name, currentMonthMillis),
            Expense(2, "Old Expense", 50.0, ExpenseCategory.FOOD.name, oldMillis)
        )

        val currentMonthExpenses = ExpenseCalculators.filterExpensesForCurrentMonth(expenses)
        assertEquals(1, currentMonthExpenses.size)
        assertEquals("Current Month Textbook", currentMonthExpenses[0].title)
    }

    @Test
    fun buildCategoryBreakdown_groupsAndCalculatesPercentages() {
        val expenses = listOf(
            Expense(1, "Meal 1", 30.0, ExpenseCategory.FOOD.name, System.currentTimeMillis()),
            Expense(2, "Meal 2", 20.0, ExpenseCategory.FOOD.name, System.currentTimeMillis()),
            Expense(3, "Book", 50.0, ExpenseCategory.BOOKS.name, System.currentTimeMillis())
        )

        val breakdown = ExpenseCalculators.buildCategoryBreakdown(expenses)
        assertEquals(2, breakdown.size)

        val bookCategory = breakdown.first { it.category == ExpenseCategory.BOOKS }
        val foodCategory = breakdown.first { it.category == ExpenseCategory.FOOD }

        assertEquals(50.0, bookCategory.amount, 0.001)
        assertEquals(0.5f, bookCategory.percentageOfTotal, 0.001f)
        assertEquals(1, bookCategory.transactionCount)

        assertEquals(50.0, foodCategory.amount, 0.001)
        assertEquals(0.5f, foodCategory.percentageOfTotal, 0.001f)
        assertEquals(2, foodCategory.transactionCount)
    }

    @Test
    fun calculateDailyAllowance_normalBudget_returnsCorrectAmount() {
        val allowance = ExpenseCalculators.calculateDailyAllowance(remainingBudget = 300.0, daysRemaining = 15)
        assertEquals(20.0, allowance, 0.001)

        val info = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 600.0,
            totalSpent = 300.0,
            daysRemaining = 15
        )
        assertEquals(20.0, info.dailyAllowance, 0.001)
        assertEquals(300.0, info.remainingBudget, 0.001)
        assertEquals(15, info.daysRemaining)
        assertEquals(DailyAllowanceStatus.ON_TRACK, info.status)
    }

    @Test
    fun calculateDailyAllowance_emptyData_returnsFullPacedAllowance() {
        // When student has 0 expenses logged, remaining is full monthly budget
        val info = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 600.0,
            totalSpent = 0.0,
            daysRemaining = 20
        )
        assertEquals(30.0, info.dailyAllowance, 0.001)
        assertEquals(600.0, info.remainingBudget, 0.001)
        assertEquals(DailyAllowanceStatus.ON_TRACK, info.status)
    }

    @Test
    fun calculateDailyAllowance_exhaustedBudget_returnsZero() {
        val allowance = ExpenseCalculators.calculateDailyAllowance(remainingBudget = 0.0, daysRemaining = 10)
        assertEquals(0.0, allowance, 0.001)

        val info = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 500.0,
            totalSpent = 500.0,
            daysRemaining = 10
        )
        assertEquals(0.0, info.dailyAllowance, 0.001)
        assertEquals(0.0, info.remainingBudget, 0.001)
        assertEquals(DailyAllowanceStatus.EXHAUSTED, info.status)
    }

    @Test
    fun calculateDailyAllowance_overspent_returnsZeroAndOverspentStatus() {
        val allowance = ExpenseCalculators.calculateDailyAllowance(remainingBudget = -85.0, daysRemaining = 12)
        assertEquals(0.0, allowance, 0.001)

        val info = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 500.0,
            totalSpent = 585.0,
            daysRemaining = 12
        )
        assertEquals(0.0, info.dailyAllowance, 0.001)
        assertEquals(-85.0, info.remainingBudget, 0.001)
        assertEquals(DailyAllowanceStatus.OVERSPENT, info.status)
    }

    @Test
    fun calculateDailyAllowance_zeroOrNegativeDays_handlesGracefully() {
        val allowanceZeroDays = ExpenseCalculators.calculateDailyAllowance(remainingBudget = 200.0, daysRemaining = 0)
        assertEquals(0.0, allowanceZeroDays, 0.001)

        val allowanceNegDays = ExpenseCalculators.calculateDailyAllowance(remainingBudget = 200.0, daysRemaining = -3)
        assertEquals(0.0, allowanceNegDays, 0.001)

        val infoZeroDays = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 600.0,
            totalSpent = 200.0,
            daysRemaining = 0
        )
        assertEquals(0.0, infoZeroDays.dailyAllowance, 0.001)
    }

    @Test
    fun calculateDailyAllowance_tightBudget_returnsTightStatus() {
        // Remaining budget is $30 with 10 days left -> $3.00/day (< $5/day threshold)
        val info = ExpenseCalculators.calculateDailyAllowanceInfo(
            monthlyBudget = 600.0,
            totalSpent = 570.0,
            daysRemaining = 10
        )
        assertEquals(3.0, info.dailyAllowance, 0.001)
        assertEquals(DailyAllowanceStatus.TIGHT, info.status)
    }

    @Test
    fun getDaysRemainingInMonth_returnsSensibleDayCount() {
        val days = DateUtils.getDaysRemainingInMonth()
        assertTrue("Days remaining in current month should be between 1 and 31", days in 1..31)
    }

    // --- Feature 1: Per-Category Budget Caps Tests ---

    @Test
    fun evaluateCategoryBudgetStatus_nullOrZeroLimit_returnsNoBudget() {
        assertEquals(CategoryBudgetStatus.NO_BUDGET, ExpenseCalculators.evaluateCategoryBudgetStatus(100.0, null))
        assertEquals(CategoryBudgetStatus.NO_BUDGET, ExpenseCalculators.evaluateCategoryBudgetStatus(100.0, 0.0))
        assertEquals(CategoryBudgetStatus.NO_BUDGET, ExpenseCalculators.evaluateCategoryBudgetStatus(100.0, -10.0))
    }

    @Test
    fun evaluateCategoryBudgetStatus_under80Percent_returnsOnTrack() {
        // Spent 75 on 100 limit -> 75% -> ON_TRACK
        val status = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 75.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.ON_TRACK, status)
    }

    @Test
    fun evaluateCategoryBudgetStatus_at80Percent_returnsWarning() {
        // Spent 80 on 100 limit -> 80% -> WARNING
        val statusExact = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 80.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.WARNING, statusExact)

        // Spent 95 on 100 limit -> 95% -> WARNING
        val statusHigh = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 95.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.WARNING, statusHigh)
    }

    @Test
    fun evaluateCategoryBudgetStatus_at100Percent_returnsWarning() {
        // Spent exactly 100 on 100 limit -> 100% -> WARNING (ratio <= 1.0)
        val statusExact = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 100.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.WARNING, statusExact)
    }

    @Test
    fun evaluateCategoryBudgetStatus_over100Percent_returnsExceeded() {
        // Spent 101 on 100 limit -> >100% -> EXCEEDED
        val status = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 101.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.EXCEEDED, status)

        val statusDouble = ExpenseCalculators.evaluateCategoryBudgetStatus(spent = 250.0, limit = 100.0)
        assertEquals(CategoryBudgetStatus.EXCEEDED, statusDouble)
    }

    @Test
    fun buildCategoryBudgetItems_evaluatesAndSortsByStatusPriority() {
        val expenses = listOf(
            Expense(1, "Dining lunch", 120.0, ExpenseCategory.FOOD.name, System.currentTimeMillis()),
            Expense(2, "Textbook", 85.0, ExpenseCategory.BOOKS.name, System.currentTimeMillis()),
            Expense(3, "Subway ticket", 30.0, ExpenseCategory.TRANSPORT.name, System.currentTimeMillis())
        )
        val budgetMap = mapOf(
            ExpenseCategory.FOOD.name to 100.0,      // 120 spent / 100 limit -> EXCEEDED
            ExpenseCategory.BOOKS.name to 100.0,     // 85 spent / 100 limit -> WARNING (>=80%)
            ExpenseCategory.TRANSPORT.name to 100.0  // 30 spent / 100 limit -> ON_TRACK
        )

        val items = ExpenseCalculators.buildCategoryBudgetItems(expenses, budgetMap)
        assertEquals(3, items.size)

        // EXCEEDED should be sorted first, followed by WARNING, then ON_TRACK
        assertEquals(ExpenseCategory.FOOD, items[0].category)
        assertEquals(CategoryBudgetStatus.EXCEEDED, items[0].status)
        assertEquals(120.0, items[0].amountSpent, 0.001)
        assertEquals(1.0f, items[0].usageRatio, 0.001f) // Clamped to 1.0 for progress indicator

        assertEquals(ExpenseCategory.BOOKS, items[1].category)
        assertEquals(CategoryBudgetStatus.WARNING, items[1].status)
        assertEquals(85.0, items[1].amountSpent, 0.001)

        assertEquals(ExpenseCategory.TRANSPORT, items[2].category)
        assertEquals(CategoryBudgetStatus.ON_TRACK, items[2].status)
        assertEquals(30.0, items[2].amountSpent, 0.001)
    }

    @Test
    fun buildCategoryBudgetItems_withIncludeAllCategories_containsAllCategories() {
        val expenses = listOf(
            Expense(1, "Dining lunch", 50.0, ExpenseCategory.FOOD.name, System.currentTimeMillis())
        )
        val budgetMap = mapOf(
            ExpenseCategory.FOOD.name to 200.0
        )

        // Without includeAllCategories, only FOOD is returned
        val filteredItems = ExpenseCalculators.buildCategoryBudgetItems(expenses, budgetMap, includeAllCategories = false)
        assertEquals(1, filteredItems.size)

        // With includeAllCategories, all ExpenseCategory entries are returned
        val allItems = ExpenseCalculators.buildCategoryBudgetItems(expenses, budgetMap, includeAllCategories = true)
        assertEquals(ExpenseCategory.entries.size, allItems.size)
    }

    @Test
    fun calculateCategoryCapImpact_nullLimit_returnsNoBudget() {
        val impact = ExpenseCalculators.calculateCategoryCapImpact(
            category = ExpenseCategory.BOOKS,
            enteredAmount = 45.0,
            currentSpent = 20.0,
            budgetLimit = null,
            currencySymbol = "₹"
        )
        assertEquals(CategoryBudgetStatus.NO_BUDGET, impact.status)
        assertEquals(null, impact.budgetLimit)
        assertEquals(65.0, impact.projectedSpent, 0.001)
        assertTrue(impact.message.contains("No monthly budget cap set"))
    }

    @Test
    fun calculateCategoryCapImpact_withinBudget_returnsOnTrack() {
        val impact = ExpenseCalculators.calculateCategoryCapImpact(
            category = ExpenseCategory.FOOD,
            enteredAmount = 50.0,
            currentSpent = 100.0,
            budgetLimit = 300.0,
            currencySymbol = "₹"
        )
        assertEquals(CategoryBudgetStatus.ON_TRACK, impact.status)
        assertEquals(150.0, impact.projectedSpent, 0.001)
        assertTrue(impact.message.contains("On track"))
        assertTrue(impact.message.contains("₹150.00 will remain"))
    }

    @Test
    fun calculateCategoryCapImpact_atOrAbove80Percent_returnsWarning() {
        val impact = ExpenseCalculators.calculateCategoryCapImpact(
            category = ExpenseCategory.FOOD,
            enteredAmount = 150.0,
            currentSpent = 100.0,
            budgetLimit = 300.0,
            currencySymbol = "₹"
        )
        // 250 / 300 -> 83%
        assertEquals(CategoryBudgetStatus.WARNING, impact.status)
        assertEquals(250.0, impact.projectedSpent, 0.001)
        assertTrue(impact.message.contains("Warning"))
        assertTrue(impact.message.contains("83%"))
    }

    @Test
    fun calculateCategoryCapImpact_overLimit_returnsExceeded() {
        val impact = ExpenseCalculators.calculateCategoryCapImpact(
            category = ExpenseCategory.FOOD,
            enteredAmount = 250.0,
            currentSpent = 100.0,
            budgetLimit = 300.0,
            currencySymbol = "₹"
        )
        // 350 / 300 -> exceeded by 50
        assertEquals(CategoryBudgetStatus.EXCEEDED, impact.status)
        assertEquals(350.0, impact.projectedSpent, 0.001)
        assertTrue(impact.message.contains("exceed"))
        assertTrue(impact.message.contains("₹50.00"))
    }

    // ==========================================
    // Feature 2: Budget Burn Rate & Runout Tests
    // ==========================================

    @Test
    fun calculateDailyBurnRate_normalValues_calculatesCorrectAverage() {
        val rate = ExpenseCalculators.calculateDailyBurnRate(totalSpent = 1500.0, daysElapsed = 10)
        assertEquals(150.0, rate, 0.001)
    }

    @Test
    fun calculateDailyBurnRate_zeroSpending_returnsZero() {
        val rate = ExpenseCalculators.calculateDailyBurnRate(totalSpent = 0.0, daysElapsed = 12)
        assertEquals(0.0, rate, 0.001)
    }

    @Test
    fun calculateDailyBurnRate_zeroOrNegativeDays_returnsZero() {
        val zeroDaysRate = ExpenseCalculators.calculateDailyBurnRate(totalSpent = 500.0, daysElapsed = 0)
        assertEquals(0.0, zeroDaysRate, 0.001)

        val negativeDaysRate = ExpenseCalculators.calculateDailyBurnRate(totalSpent = 500.0, daysElapsed = -5)
        assertEquals(0.0, negativeDaysRate, 0.001)
    }

    @Test
    fun calculateDailyBurnRate_negativeSpent_returnsZero() {
        val rate = ExpenseCalculators.calculateDailyBurnRate(totalSpent = -100.0, daysElapsed = 5)
        assertEquals(0.0, rate, 0.001)
    }

    @Test
    fun calculateHistoricalAverageDailySpending_emptyList_returnsZero() {
        val avg = ExpenseCalculators.calculateHistoricalAverageDailySpending(emptyList())
        assertEquals(0.0, avg, 0.001)
    }

    @Test
    fun calculateHistoricalAverageDailySpending_singleExpense_returnsAmount() {
        val expenses = listOf(
            Expense(1, "Textbook", 120.0, ExpenseCategory.BOOKS.name, 1000000L)
        )
        val avg = ExpenseCalculators.calculateHistoricalAverageDailySpending(expenses)
        assertEquals(120.0, avg, 0.001)
    }

    @Test
    fun calculateHistoricalAverageDailySpending_multiDayExpenses_calculatesCorrectRate() {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val startMillis = 10000000L
        val expenses = listOf(
            Expense(1, "Groceries", 200.0, ExpenseCategory.FOOD.name, startMillis),
            Expense(2, "Transit", 100.0, ExpenseCategory.TRANSPORT.name, startMillis + (4 * oneDayMillis))
        )
        // Span = 4 days difference + 1 = 5 days span. Total spent = 300.0. Rate = 60.0 / day.
        val avg = ExpenseCalculators.calculateHistoricalAverageDailySpending(expenses)
        assertEquals(60.0, avg, 0.001)
    }

    @Test
    fun calculateBudgetRunoutForecast_zeroSpending_returnsInsufficientData() {
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 5000.0,
            monthlySpent = 0.0,
            daysElapsed = 10,
            calendarDaysRemaining = 20,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.INSUFFICIENT_DATA, forecast.status)
        assertEquals(0.0, forecast.dailyBurnRate, 0.001)
        assertEquals(0, forecast.estimatedDaysRemaining)
        assertNull(forecast.runoutDateMillis)
        assertTrue(forecast.statusHeadline.contains("Insufficient"))
        assertTrue(forecast.statusMessage.contains("No expenses recorded"))
    }

    @Test
    fun calculateBudgetRunoutForecast_earlyRunout_detectsEarlyDepletion() {
        // Budget 3000, spent 2000 in 10 days (rate 200/day).
        // Remaining budget = 1000.
        // Estimated days remaining = 1000 / 200 = 5 days.
        // Calendar days left = 20 days.
        // 5 < 20 -> EARLY_RUNOUT.
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 3000.0,
            monthlySpent = 2000.0,
            daysElapsed = 10,
            calendarDaysRemaining = 20,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.EARLY_RUNOUT, forecast.status)
        assertEquals(200.0, forecast.dailyBurnRate, 0.001)
        assertEquals(5, forecast.estimatedDaysRemaining)
        assertEquals(20, forecast.calendarDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Early Runout"))
        assertTrue(forecast.statusMessage.contains("run out in ~5 days"))
        assertTrue(forecast.statusMessage.contains("15 days before month end"))
    }

    @Test
    fun calculateBudgetRunoutForecast_onTrack_balancedPace() {
        // Budget 3000, spent 1000 in 10 days (rate 100/day).
        // Remaining budget = 2000.
        // Estimated days remaining = 2000 / 100 = 20 days.
        // Calendar days left = 20 days.
        // 20 == 20 -> ON_TRACK.
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 3000.0,
            monthlySpent = 1000.0,
            daysElapsed = 10,
            calendarDaysRemaining = 20,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.ON_TRACK, forecast.status)
        assertEquals(100.0, forecast.dailyBurnRate, 0.001)
        assertEquals(20, forecast.estimatedDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Balanced"))
        assertTrue(forecast.statusMessage.contains("safely to month end"))
    }

    @Test
    fun calculateBudgetRunoutForecast_sustainable_projectedSurplus() {
        // Budget 5000, spent 500 in 10 days (rate 50/day).
        // Remaining budget = 4500.
        // Estimated days = 4500 / 50 = 90 days.
        // Calendar days left = 20 days.
        // 90 > 22 -> SUSTAINABLE.
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 5000.0,
            monthlySpent = 500.0,
            daysElapsed = 10,
            calendarDaysRemaining = 20,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.SUSTAINABLE, forecast.status)
        assertEquals(50.0, forecast.dailyBurnRate, 0.001)
        assertEquals(90, forecast.estimatedDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Sustainable"))
        // Projected surplus = 4500 - (50 * 20) = 3500.
        assertTrue(forecast.statusMessage.contains("₹3500.00"))
    }

    @Test
    fun calculateBudgetRunoutForecast_exhaustedBudget_returnsExhausted() {
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 2000.0,
            monthlySpent = 2000.0,
            daysElapsed = 15,
            calendarDaysRemaining = 15,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.EXHAUSTED, forecast.status)
        assertEquals(0, forecast.estimatedDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Exhausted"))
        assertTrue(forecast.statusMessage.contains("100% used"))
    }

    @Test
    fun calculateBudgetRunoutForecast_overBudget_returnsOverBudget() {
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 2000.0,
            monthlySpent = 2250.0,
            daysElapsed = 15,
            calendarDaysRemaining = 15,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.OVER_BUDGET, forecast.status)
        assertEquals(0, forecast.estimatedDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Exceeded"))
        assertTrue(forecast.statusMessage.contains("exceeded by ₹250.00"))
    }

    @Test
    fun calculateBudgetRunoutForecast_monthBoundary_dayOneOfThirty() {
        // Day 1 of month, spent 200 out of 2000.
        // Burn rate = 200 / 1 = 200/day.
        // Remaining = 1800.
        // Estimated days = 1800 / 200 = 9 days.
        // Calendar days left = 30 days.
        // 9 < 30 -> EARLY_RUNOUT.
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 2000.0,
            monthlySpent = 200.0,
            daysElapsed = 1,
            calendarDaysRemaining = 30,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.EARLY_RUNOUT, forecast.status)
        assertEquals(200.0, forecast.dailyBurnRate, 0.001)
        assertEquals(9, forecast.estimatedDaysRemaining)
        assertTrue(forecast.statusMessage.contains("21 days before month end"))
    }

    @Test
    fun calculateBudgetRunoutForecast_monthBoundary_monthEnded() {
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 2000.0,
            monthlySpent = 1500.0,
            daysElapsed = 31,
            calendarDaysRemaining = 0,
            currencySymbol = "₹"
        )
        assertEquals(BurnRateStatus.SUSTAINABLE, forecast.status)
        assertEquals(0, forecast.estimatedDaysRemaining)
        assertEquals(0, forecast.calendarDaysRemaining)
        assertTrue(forecast.statusHeadline.contains("Month Concluded"))
    }

    @Test
    fun calculateBudgetRunoutForecast_includesEstimateDisclaimerNotice() {
        val forecast = ExpenseCalculators.calculateBudgetRunoutForecast(
            monthlyBudget = 3000.0,
            monthlySpent = 1000.0,
            daysElapsed = 10,
            calendarDaysRemaining = 20,
            currencySymbol = "₹"
        )
        assertTrue(forecast.isEstimateNotice.contains("estimate"))
        assertTrue(forecast.isEstimateNotice.contains("not a guaranteed prediction"))
    }

    // ==========================================
    // Feature 3: Smart Spending Badges Tests
    // ==========================================

    @Test
    fun evaluateThriftyWeek_incompleteWeek_fewerThanSevenDays_notEarned() {
        val expenses = listOf(
            Expense(1, "Lunch", 20.0, ExpenseCategory.FOOD.name, System.currentTimeMillis())
        )
        // Day 4 of month (< 7 days elapsed)
        val badge = BadgeCalculators.evaluateThriftyWeek(
            currentMonthExpenses = expenses,
            monthlyBudget = 2800.0,
            daysElapsed = 4,
            totalDaysInMonth = 28,
            currencySymbol = "₹"
        )
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("Incomplete week"))
        assertTrue(badge.earnedReason.contains("4 of 7 days"))
    }

    @Test
    fun evaluateThriftyWeek_noExpensesInWindow_notEarned() {
        // Day 10 of month, but no expenses logged in past 7 days
        val badge = BadgeCalculators.evaluateThriftyWeek(
            currentMonthExpenses = emptyList(),
            monthlyBudget = 2800.0,
            daysElapsed = 10,
            totalDaysInMonth = 28,
            currencySymbol = "₹"
        )
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("No expenses logged"))
    }

    @Test
    fun evaluateThriftyWeek_underWeeklyThreshold_earned() {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            Expense(1, "Groceries", 250.0, ExpenseCategory.FOOD.name, now - 10000000L),
            Expense(2, "Stationery", 150.0, ExpenseCategory.BOOKS.name, now - 20000000L)
        )
        // Budget 2800 over 28 days = 700 weekly cap. Spent 400 <= 700.
        val badge = BadgeCalculators.evaluateThriftyWeek(
            currentMonthExpenses = expenses,
            monthlyBudget = 2800.0,
            daysElapsed = 10,
            totalDaysInMonth = 28,
            currencySymbol = "₹",
            referenceMillis = now
        )
        assertTrue(badge.isEarned)
        assertTrue(badge.earnedReason.contains("Awarded"))
        assertTrue(badge.earnedReason.contains("₹400.00"))
    }

    @Test
    fun evaluateThriftyWeek_exceededWeeklyThreshold_notEarned() {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            Expense(1, "Gadget", 900.0, ExpenseCategory.OTHER.name, now - 5000000L)
        )
        // Budget 2800 over 28 days = 700 weekly cap. Spent 900 > 700.
        val badge = BadgeCalculators.evaluateThriftyWeek(
            currentMonthExpenses = expenses,
            monthlyBudget = 2800.0,
            daysElapsed = 10,
            totalDaysInMonth = 28,
            currencySymbol = "₹",
            referenceMillis = now
        )
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("exceeded weekly budget"))
    }

    @Test
    fun evaluateUnderBudget_insufficientTransactionsOrZeroSpend_notEarned() {
        // 0 transactions
        val badgeZero = BadgeCalculators.evaluateUnderBudget(
            monthlySpent = 0.0,
            monthlyBudget = 3000.0,
            transactionCount = 0,
            currencySymbol = "₹"
        )
        assertFalse(badgeZero.isEarned)

        // Only 2 transactions
        val badgeTwo = BadgeCalculators.evaluateUnderBudget(
            monthlySpent = 150.0,
            monthlyBudget = 3000.0,
            transactionCount = 2,
            currencySymbol = "₹"
        )
        assertFalse(badgeTwo.isEarned)
        assertTrue(badgeTwo.earnedReason.contains("at least 3 campus expenses"))
    }

    @Test
    fun evaluateUnderBudget_withinBudget_earned() {
        val badge = BadgeCalculators.evaluateUnderBudget(
            monthlySpent = 1200.0,
            monthlyBudget = 3000.0,
            transactionCount = 5,
            currencySymbol = "₹"
        )
        assertTrue(badge.isEarned)
        assertTrue(badge.earnedReason.contains("Awarded"))
        assertTrue(badge.earnedReason.contains("₹1800.00 under"))
    }

    @Test
    fun evaluateUnderBudget_overBudget_notEarned() {
        val badge = BadgeCalculators.evaluateUnderBudget(
            monthlySpent = 3500.0,
            monthlyBudget = 3000.0,
            transactionCount = 6,
            currencySymbol = "₹"
        )
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("exceeded by ₹500.00"))
    }

    @Test
    fun evaluateBudgetChampion_allCriteriaMet_earned() {
        val forecast = BurnRateForecast(
            status = BurnRateStatus.SUSTAINABLE,
            statusHeadline = "Sustainable Pace"
        )
        val cappedItems = listOf(
            CategoryBudgetItem(ExpenseCategory.FOOD, 400.0, 600.0, 0.67f, CategoryBudgetStatus.ON_TRACK, 3)
        )
        // 1000 spent <= 75% of 2000 (1500), 5 txns, 8 days, Sustainable, 0 caps exceeded
        val badge = BadgeCalculators.evaluateBudgetChampion(
            monthlySpent = 1000.0,
            monthlyBudget = 2000.0,
            transactionCount = 5,
            daysElapsed = 8,
            burnRateForecast = forecast,
            categoryBudgetItems = cappedItems,
            currencySymbol = "₹"
        )
        assertTrue(badge.isEarned)
        assertTrue(badge.earnedReason.contains("Awarded"))
        assertTrue(badge.earnedReason.contains("Outstanding financial management"))
    }

    @Test
    fun evaluateBudgetChampion_exceededCap_notEarned() {
        val forecast = BurnRateForecast(
            status = BurnRateStatus.SUSTAINABLE,
            statusHeadline = "Sustainable Pace"
        )
        val cappedItems = listOf(
            CategoryBudgetItem(ExpenseCategory.FOOD, 700.0, 600.0, 1.0f, CategoryBudgetStatus.EXCEEDED, 3)
        )
        val badge = BadgeCalculators.evaluateBudgetChampion(
            monthlySpent = 1000.0,
            monthlyBudget = 2000.0,
            transactionCount = 5,
            daysElapsed = 8,
            burnRateForecast = forecast,
            categoryBudgetItems = cappedItems,
            currencySymbol = "₹"
        )
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("category budget caps have been exceeded"))
    }

    @Test
    fun evaluateCapCrusader_noCaps_notEarned() {
        val badge = BadgeCalculators.evaluateCapCrusader(emptyList())
        assertFalse(badge.isEarned)
        assertTrue(badge.earnedReason.contains("No category caps configured"))
    }

    @Test
    fun evaluateCapCrusader_capsRespectedWithExpenses_earned() {
        val cappedItems = listOf(
            CategoryBudgetItem(ExpenseCategory.FOOD, 150.0, 300.0, 0.5f, CategoryBudgetStatus.ON_TRACK, 2),
            CategoryBudgetItem(ExpenseCategory.BOOKS, 80.0, 200.0, 0.4f, CategoryBudgetStatus.ON_TRACK, 1)
        )
        val badge = BadgeCalculators.evaluateCapCrusader(cappedItems)
        assertTrue(badge.isEarned)
        assertTrue(badge.earnedReason.contains("Awarded"))
        assertTrue(badge.earnedReason.contains("2 configured category budget caps"))
    }

    @Test
    fun evaluateConsistentTracker_requiresFiveTxnsAcrossThreeDays() {
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val baseMillis = 100000000L

        // 5 txns all on same day -> not earned
        val sameDayExpenses = List(5) { i ->
            Expense(i.toLong(), "Item $i", 10.0, ExpenseCategory.FOOD.name, baseMillis)
        }
        val badgeSameDay = BadgeCalculators.evaluateConsistentTracker(sameDayExpenses)
        assertFalse(badgeSameDay.isEarned)

        // 5 txns across 3 different days -> earned
        val multiDayExpenses = listOf(
            Expense(1, "Item 1", 10.0, ExpenseCategory.FOOD.name, baseMillis),
            Expense(2, "Item 2", 10.0, ExpenseCategory.FOOD.name, baseMillis),
            Expense(3, "Item 3", 10.0, ExpenseCategory.FOOD.name, baseMillis + oneDayMillis),
            Expense(4, "Item 4", 10.0, ExpenseCategory.FOOD.name, baseMillis + (2 * oneDayMillis)),
            Expense(5, "Item 5", 10.0, ExpenseCategory.FOOD.name, baseMillis + (2 * oneDayMillis))
        )
        val badgeMultiDay = BadgeCalculators.evaluateConsistentTracker(multiDayExpenses)
        assertTrue(badgeMultiDay.isEarned)
        assertTrue(badgeMultiDay.earnedReason.contains("Awarded"))
    }

    @Test
    fun evaluateBadges_noDuplicateIds_returnsUniqueBadges() {
        val badges = BadgeCalculators.evaluateBadges(
            allExpenses = emptyList(),
            currentMonthExpenses = emptyList(),
            monthlyBudget = 3000.0,
            daysElapsed = 10,
            totalDaysInMonth = 30,
            burnRateForecast = BurnRateForecast(),
            categoryBudgetItems = emptyList(),
            currencySymbol = "₹"
        )
        assertEquals(5, badges.size)
        // Check uniqueness of BadgeIds
        val uniqueIds = badges.map { it.id }.toSet()
        assertEquals(badges.size, uniqueIds.size)
    }

    @Test
    fun evaluateBadges_expenseEditOrDeletion_updatesBadgeStateDeterministically() {
        val now = System.currentTimeMillis()
        val baseExpenses = mutableListOf(
            Expense(1, "Book", 100.0, ExpenseCategory.BOOKS.name, now - 10000L),
            Expense(2, "Food 1", 50.0, ExpenseCategory.FOOD.name, now - 20000L),
            Expense(3, "Food 2", 50.0, ExpenseCategory.FOOD.name, now - 30000L)
        )
        val budget = 500.0

        // Step 1: Baseline evaluation (Total = 200 <= 500) -> Under Budget earned
        val badgesBefore = BadgeCalculators.evaluateBadges(
            allExpenses = baseExpenses,
            currentMonthExpenses = baseExpenses,
            monthlyBudget = budget,
            daysElapsed = 10,
            totalDaysInMonth = 30,
            burnRateForecast = BurnRateForecast(status = BurnRateStatus.SUSTAINABLE),
            categoryBudgetItems = emptyList()
        )
        val underBudgetBefore = badgesBefore.first { it.id == BadgeId.UNDER_BUDGET }
        assertTrue(underBudgetBefore.isEarned)

        // Step 2: Edit/Add an expense that causes overspending (e.g. + ₹400 -> total 600 > 500)
        val editedExpenses = baseExpenses + Expense(4, "Big Expense", 400.0, ExpenseCategory.OTHER.name, now)
        val badgesAfterEdit = BadgeCalculators.evaluateBadges(
            allExpenses = editedExpenses,
            currentMonthExpenses = editedExpenses,
            monthlyBudget = budget,
            daysElapsed = 10,
            totalDaysInMonth = 30,
            burnRateForecast = BurnRateForecast(status = BurnRateStatus.OVER_BUDGET),
            categoryBudgetItems = emptyList()
        )
        val underBudgetAfterEdit = badgesAfterEdit.first { it.id == BadgeId.UNDER_BUDGET }
        assertFalse(underBudgetAfterEdit.isEarned) // Over budget immediately revokes badge

        // Step 3: Delete the big expense -> restored back to base expenses
        val badgesAfterDelete = BadgeCalculators.evaluateBadges(
            allExpenses = baseExpenses,
            currentMonthExpenses = baseExpenses,
            monthlyBudget = budget,
            daysElapsed = 10,
            totalDaysInMonth = 30,
            burnRateForecast = BurnRateForecast(status = BurnRateStatus.SUSTAINABLE),
            categoryBudgetItems = emptyList()
        )
        val underBudgetAfterDelete = badgesAfterDelete.first { it.id == BadgeId.UNDER_BUDGET }
        assertTrue(underBudgetAfterDelete.isEarned) // Deletion restores badge eligibility
    }
}
