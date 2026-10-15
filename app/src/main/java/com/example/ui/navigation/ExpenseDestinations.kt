package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the Campus Expense Tracker.
 * Demonstrates Android Basics with Compose Unit 4: Navigation.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object SplitBill : Screen("split_bill", "Split Bill", Icons.AutoMirrored.Filled.CallSplit)
    object History : Screen("history", "History", Icons.Default.History)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object ExpenseEntry : Screen("expense_entry?expenseId={expenseId}", "Expense") {
        const val ARG_EXPENSE_ID = "expenseId"
        fun createRoute(expenseId: Long = 0L): String = "expense_entry?expenseId=$expenseId"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.SplitBill,
    Screen.History,
    Screen.Analytics,
    Screen.Settings
)
