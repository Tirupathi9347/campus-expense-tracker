package com.example.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.ui.dashboard.CategoryCapImpact
import com.example.ui.dashboard.ExpenseCalculators
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * UI State representation for the Add / Edit Expense screen.
 * Follows Android Basics with Compose Unit 2 (State) and Unit 6 (Room).
 */
data class ExpenseDetails(
    val id: Long = 0L,
    val title: String = "",
    val amount: String = "",
    val category: String = ExpenseCategory.FOOD.name,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class ExpenseUiState(
    val expenseDetails: ExpenseDetails = ExpenseDetails(),
    val isEntryValid: Boolean = false,
    val validationError: String? = null,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val isLoaded: Boolean = false,
    val currencySymbol: String = "₹",
    val categoryCapImpact: CategoryCapImpact? = null
)

/**
 * Validation rules for expense input.
 */
object ExpenseValidator {
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validate(details: ExpenseDetails): ValidationResult {
        if (details.title.trim().isBlank()) {
            return ValidationResult.Invalid("Please enter a title for this expense.")
        }
        val parsedAmount = details.amount.trim().toDoubleOrNull()
        if (parsedAmount == null) {
            return ValidationResult.Invalid("Please enter a valid numeric amount.")
        }
        if (parsedAmount <= 0.0) {
            return ValidationResult.Invalid("Expense amount must be greater than 0.")
        }
        if (parsedAmount > 1000000.0) {
            return ValidationResult.Invalid("Amount exceeds reasonable limit.")
        }
        if (details.dateMillis <= 0L) {
            return ValidationResult.Invalid("Please select a valid date.")
        }
        return ValidationResult.Valid
    }
}

class ExpenseEntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: com.example.data.UserPreferencesRepository
) : ViewModel() {

    private val expenseId: Long = savedStateHandle.get<Long>("expenseId") ?: 0L
    private var initialExpense: Expense? = null
    private var budgetsMap: Map<String, Double> = emptyMap()
    private var monthCategorySpendMap: Map<String, Double> = emptyMap()

    var uiState by mutableStateOf(
        ExpenseUiState(
            isEditing = expenseId > 0L,
            isLoaded = expenseId == 0L
        )
    )
        private set

    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.userPreferencesFlow,
                expenseRepository.getAllCategoryBudgetsStream(),
                expenseRepository.getAllExpensesStream()
            ) { prefs, budgets, expenses ->
                Triple(prefs, budgets, expenses)
            }.collect { (prefs, budgets, expenses) ->
                budgetsMap = budgets.associate { it.categoryName to it.budgetLimit }
                val currentMonthExpenses = ExpenseCalculators.filterExpensesForCurrentMonth(expenses)
                monthCategorySpendMap = currentMonthExpenses
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                uiState = uiState.copy(
                    currencySymbol = prefs.currencySymbol,
                    categoryCapImpact = evaluateCapImpact(uiState.expenseDetails, prefs.currencySymbol)
                )
            }
        }
        if (expenseId > 0L) {
            viewModelScope.launch {
                val expense = expenseRepository.getExpenseById(expenseId)
                if (expense != null) {
                    initialExpense = expense
                    val details = expense.toExpenseDetails()
                    uiState = uiState.copy(
                        expenseDetails = details,
                        isEntryValid = true,
                        isEditing = true,
                        isLoaded = true,
                        categoryCapImpact = evaluateCapImpact(details, uiState.currencySymbol)
                    )
                } else {
                    uiState = uiState.copy(isLoaded = true)
                }
            }
        }
    }

    private fun evaluateCapImpact(details: ExpenseDetails, symbol: String): CategoryCapImpact {
        val catEnum = ExpenseCategory.fromName(details.category)
        val limit = budgetsMap[catEnum.name]
        val totalSpent = monthCategorySpendMap[catEnum.name] ?: 0.0
        val adjustedSpent = if (expenseId > 0L && initialExpense?.category.equals(catEnum.name, ignoreCase = true)) {
            (totalSpent - (initialExpense?.amount ?: 0.0)).coerceAtLeast(0.0)
        } else {
            totalSpent
        }
        val entered = details.amount.toDoubleOrNull() ?: 0.0
        return ExpenseCalculators.calculateCategoryCapImpact(
            category = catEnum,
            enteredAmount = entered,
            currentSpent = adjustedSpent,
            budgetLimit = limit,
            currencySymbol = symbol
        )
    }

    fun updateUiState(newDetails: ExpenseDetails) {
        val validation = ExpenseValidator.validate(newDetails)
        uiState = uiState.copy(
            expenseDetails = newDetails,
            isEntryValid = validation is ExpenseValidator.ValidationResult.Valid,
            validationError = when (validation) {
                is ExpenseValidator.ValidationResult.Invalid -> validation.message
                ExpenseValidator.ValidationResult.Valid -> null
            },
            categoryCapImpact = evaluateCapImpact(newDetails, uiState.currencySymbol)
        )
    }

    suspend fun saveExpense(): Boolean {
        val validation = ExpenseValidator.validate(uiState.expenseDetails)
        if (validation !is ExpenseValidator.ValidationResult.Valid) {
            uiState = uiState.copy(
                isEntryValid = false,
                validationError = (validation as ExpenseValidator.ValidationResult.Invalid).message
            )
            return false
        }

        uiState = uiState.copy(isSaving = true)
        return try {
            val expense = uiState.expenseDetails.toExpense()
            if (uiState.isEditing && expense.id > 0L) {
                expenseRepository.updateExpense(expense)
            } else {
                expenseRepository.insertExpense(expense)
            }
            uiState = uiState.copy(isSaving = false)
            true
        } catch (e: Exception) {
            uiState = uiState.copy(isSaving = false, validationError = e.message ?: "Failed to save")
            false
        }
    }

    suspend fun deleteExpense(): Boolean {
        if (!uiState.isEditing || uiState.expenseDetails.id <= 0L) return false
        return try {
            expenseRepository.deleteExpenseById(uiState.expenseDetails.id)
            true
        } catch (e: Exception) {
            false
        }
    }
}

fun Expense.toExpenseDetails(): ExpenseDetails = ExpenseDetails(
    id = id,
    title = title,
    amount = amount.toString(),
    category = category,
    dateMillis = dateMillis,
    note = note
)

fun ExpenseDetails.toExpense(): Expense = Expense(
    id = id,
    title = title.trim(),
    amount = amount.toDoubleOrNull() ?: 0.0,
    category = category,
    dateMillis = dateMillis,
    note = note.trim()
)
