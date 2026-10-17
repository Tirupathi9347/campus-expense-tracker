package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AppViewModelProvider
import com.example.ui.analytics.CategoryBreakdownScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.entry.ExpenseEntryScreen
import com.example.ui.history.TransactionHistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splitbill.SplitBillScreen

/**
 * Main Navigation host composable managing screen transitions and bottom navigation.
 */
@Composable
fun CampusExpenseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on top-level screens
    val isTopLevelScreen = bottomNavScreens.any { it.route == currentRoute }

    // Helper for switching between top-level bottom nav destinations
    val navigateToTopLevelDestination: (String) -> Unit = { targetRoute ->
        if (targetRoute != currentRoute) {
            if (targetRoute == Screen.Dashboard.route) {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = false
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            } else {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateToTopLevelDestination(screen.route)
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        letterSpacing = (-0.2).sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard Destination
            composable(route = Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.ExpenseEntry.createRoute(0L))
                    },
                    onNavigateToEditExpense = { expenseId ->
                        navController.navigate(Screen.ExpenseEntry.createRoute(expenseId))
                    },
                    onNavigateToHistory = {
                        navigateToTopLevelDestination(Screen.History.route)
                    },
                    onNavigateToAnalytics = {
                        navigateToTopLevelDestination(Screen.Analytics.route)
                    },
                    onNavigateToSplitBill = {
                        navigateToTopLevelDestination(Screen.SplitBill.route)
                    }
                )
            }

            // Split-Bill Calculator Destination
            composable(route = Screen.SplitBill.route) {
                SplitBillScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory)
                )
            }

            // Transaction History Destination
            composable(route = Screen.History.route) {
                TransactionHistoryScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.ExpenseEntry.createRoute(0L))
                    },
                    onNavigateToEditExpense = { expenseId ->
                        navController.navigate(Screen.ExpenseEntry.createRoute(expenseId))
                    }
                )
            }

            // Spending Analytics Destination
            composable(route = Screen.Analytics.route) {
                CategoryBreakdownScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onNavigateToAddExpense = {
                        navController.navigate(Screen.ExpenseEntry.createRoute(0L))
                    }
                )
            }

            // Settings Destination
            composable(route = Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory)
                )
            }

            // Expense Entry / Edit Destination
            composable(
                route = Screen.ExpenseEntry.route,
                arguments = listOf(
                    navArgument(Screen.ExpenseEntry.ARG_EXPENSE_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) {
                ExpenseEntryScreen(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
