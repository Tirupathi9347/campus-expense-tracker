package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    AMOUNT_HIGHEST("Highest Amount"),
    AMOUNT_LOWEST("Lowest Amount")
}

data class TransactionHistoryUiState(
    val expenses: List<Expense> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val sortOrder: SortOrder = SortOrder.DATE_NEWEST,
    val totalFilteredAmount: Double = 0.0,
    val currencySymbol: String = "$",
    val isLoading: Boolean = false
)

class TransactionHistoryViewModel(
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<ExpenseCategory?>(null)
    private val sortOrderFlow = MutableStateFlow(SortOrder.DATE_NEWEST)

    val uiState: StateFlow<TransactionHistoryUiState> = combine(
        expenseRepository.getAllExpensesStream(),
        userPreferencesRepository.userPreferencesFlow,
        searchQueryFlow,
        selectedCategoryFlow,
        sortOrderFlow
    ) { allExpenses, preferences, search, category, sortOrder ->

        val filtered = allExpenses.filter { expense ->
            val matchesCategory = category == null || expense.categoryEnum == category
            val matchesSearch = search.isBlank() ||
                    expense.title.contains(search, ignoreCase = true) ||
                    expense.note.contains(search, ignoreCase = true) ||
                    expense.category.contains(search, ignoreCase = true)
            matchesCategory && matchesSearch
        }

        val sorted = when (sortOrder) {
            SortOrder.DATE_NEWEST -> filtered.sortedWith(compareByDescending<Expense> { it.dateMillis }.thenByDescending { it.id })
            SortOrder.DATE_OLDEST -> filtered.sortedWith(compareBy<Expense> { it.dateMillis }.thenBy { it.id })
            SortOrder.AMOUNT_HIGHEST -> filtered.sortedByDescending { it.amount }
            SortOrder.AMOUNT_LOWEST -> filtered.sortedBy { it.amount }
        }

        TransactionHistoryUiState(
            expenses = sorted,
            searchQuery = search,
            selectedCategory = category,
            sortOrder = sortOrder,
            totalFilteredAmount = sorted.sumOf { it.amount },
            currencySymbol = preferences.currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionHistoryUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun onCategorySelected(category: ExpenseCategory?) {
        selectedCategoryFlow.value = category
    }

    fun onSortOrderChanged(sortOrder: SortOrder) {
        sortOrderFlow.value = sortOrder
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }
}
