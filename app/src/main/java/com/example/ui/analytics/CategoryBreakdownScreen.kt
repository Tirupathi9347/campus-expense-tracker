package com.example.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExpenseCategory
import com.example.ui.components.BudgetBurnRateCard
import com.example.ui.components.CategoryIconCircle
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricTile
import com.example.ui.components.SpendingBadgeDetailCard
import com.example.ui.dashboard.CategoryBudgetItem
import com.example.ui.dashboard.CategoryBudgetStatus
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingCategoryItem by remember { mutableStateOf<CategoryBudgetItem?>(null) }
    var editCapInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spending & Budget Caps",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        if (uiState.categoryBreakdown.isEmpty() && uiState.categoryBudgetItems.isEmpty()) {
            EmptyStateView(
                title = "No Analytics Data Yet",
                message = "Log campus transactions or set category budget limits to track your student spending.",
                actionButtonText = "Add Expense",
                onActionClick = onNavigateToAddExpense,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High-level Metric Tiles
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "All-Time Spend",
                            value = CurrencyFormatter.format(uiState.allTimeTotal, uiState.currencySymbol),
                            icon = Icons.Default.AttachMoney,
                            iconTint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "This Month",
                            value = CurrencyFormatter.format(uiState.currentMonthTotal, uiState.currencySymbol),
                            icon = Icons.Default.CalendarMonth,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "Avg / Expense",
                            value = CurrencyFormatter.format(uiState.averageTransaction, uiState.currencySymbol),
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            title = "Highest Single",
                            value = uiState.highestExpense?.let { CurrencyFormatter.format(it.amount, uiState.currencySymbol) } ?: "—",
                            icon = Icons.Default.Star,
                            iconTint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Budget Burn Rate & Runout Forecast (Feature 2)
                item {
                    BudgetBurnRateCard(
                        forecast = uiState.burnRateForecast,
                        currencySymbol = uiState.currencySymbol
                    )
                }

                // Section Header: Per-Category Budget Caps
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "Category Budget Caps & Limits",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Set monthly spending caps per category. Automatic warning alerts at 80% and 100%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.categoryBudgetItems, key = { it.category.name }) { item ->
                    CategoryBudgetCard(
                        item = item,
                        currencySymbol = uiState.currencySymbol,
                        onEditCapClick = {
                            editingCategoryItem = item
                            editCapInput = item.budgetLimit?.toInt()?.toString() ?: ""
                        }
                    )
                }

                // Smart Spending Badges Section (Feature 3)
                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Smart Spending Badges",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Rule-based achievements awarded for disciplined budget management.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(uiState.spendingBadges, key = { it.id.name }) { badge ->
                    SpendingBadgeDetailCard(badge = badge)
                }
            }
        }
    }

    // Edit Budget Cap Dialog
    if (editingCategoryItem != null) {
        val cat = editingCategoryItem!!.category
        AlertDialog(
            onDismissRequest = { editingCategoryItem = null },
            title = {
                Text("Set ${cat.displayName} Cap")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Set a monthly spending limit for ${cat.displayName}. You'll receive a warning when spending reaches 80% and an alert when exceeded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editCapInput,
                        onValueChange = { editCapInput = it },
                        label = { Text("Monthly Budget Cap") },
                        prefix = { Text("${uiState.currencySymbol} ") },
                        placeholder = { Text("e.g., 2000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_cap_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editCapInput.toDoubleOrNull() ?: 0.0
                        viewModel.setCategoryBudget(cat, parsed)
                        editingCategoryItem = null
                    },
                    modifier = Modifier.testTag("save_category_cap_button")
                ) {
                    Text("Save Cap")
                }
            },
            dismissButton = {
                Row {
                    if (editingCategoryItem?.budgetLimit != null) {
                        TextButton(
                            onClick = {
                                viewModel.deleteCategoryBudget(cat)
                                editingCategoryItem = null
                            }
                        ) {
                            Text("Remove Cap", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { editingCategoryItem = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

/**
 * Category Card displaying amount spent, budget cap, usage progress, and warning status.
 */
@Composable
fun CategoryBudgetCard(
    item: CategoryBudgetItem,
    currencySymbol: String,
    onEditCapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.usageRatio,
        label = "category_cap_progress_${item.category.name}"
    )

    val hasCap = item.budgetLimit != null && item.budgetLimit > 0.0
    val progressBarColor = when (item.status) {
        CategoryBudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
        CategoryBudgetStatus.WARNING -> Color(0xFFF59E0B) // Amber
        CategoryBudgetStatus.ON_TRACK -> item.category.color
        CategoryBudgetStatus.NO_BUDGET -> item.category.color.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditCapClick() }
            .testTag("category_budget_card_${item.category.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Category Icon, Name, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconCircle(
                    category = item.category,
                    sizeDp = 44
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.transactionCount} txn${if (item.transactionCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEditCapClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${item.category.displayName} Cap",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spending and Limit Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Spent this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(item.amountSpent, currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (item.status == CategoryBudgetStatus.EXCEEDED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hasCap) "Budget Cap" else "Cap Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasCap) {
                            CurrencyFormatter.format(item.budgetLimit!!, currencySymbol)
                        } else {
                            "No Cap Set"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (hasCap) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { if (hasCap) animatedProgress else 0.05f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressBarColor,
                trackColor = progressBarColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Warning / Confirmation Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.status) {
                    CategoryBudgetStatus.EXCEEDED -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Exceeded by ${CurrencyFormatter.format(item.amountSpent - (item.budgetLimit ?: 0.0), currencySymbol)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    CategoryBudgetStatus.WARNING -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "80%+ Warning (${CurrencyFormatter.format((item.budgetLimit ?: 0.0) - item.amountSpent, currencySymbol)} left)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                    CategoryBudgetStatus.ON_TRACK -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "On Track (${CurrencyFormatter.format((item.budgetLimit ?: 0.0) - item.amountSpent, currencySymbol)} left)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                    CategoryBudgetStatus.NO_BUDGET -> {
                        Text(
                            text = "Tap to set a monthly limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (hasCap) {
                    val pct = (item.amountSpent / item.budgetLimit!! * 100).toInt()
                    Text(
                        text = "$pct% used",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = progressBarColor
                    )
                }
            }
        }
    }
}
