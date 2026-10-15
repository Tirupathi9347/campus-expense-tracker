package com.example.ui.splitbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.UserPreferencesRepository
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SplitBillUiState(
    val totalAmountInput: String = "",
    val participantsInput: String = "2",
    val titleInput: String = "Shared Campus Bill",
    val selectedCategory: ExpenseCategory = ExpenseCategory.FOOD,
    val currencySymbol: String = "₹",
    val result: SplitBillResult? = null,
    val errorMessage: String? = null,
    val isExpenseRecorded: Boolean = false,
    val recordedExpenseMessage: String? = null
)

class SplitBillViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _internalState = MutableStateFlow(
        SplitBillUiState(
            totalAmountInput = "",
            participantsInput = "2"
        )
    )

    val uiState: StateFlow<SplitBillUiState> = combine(
        _internalState,
        userPreferencesRepository.userPreferencesFlow
    ) { state, preferences ->
        val currentSymbol = preferences.currencySymbol
        val updatedResult = if (state.totalAmountInput.isNotBlank() && state.participantsInput.isNotBlank()) {
            val validation = SplitBillCalculator.validateInputs(state.totalAmountInput, state.participantsInput)
            if (validation is SplitBillValidation.Valid) {
                val amount = state.totalAmountInput.toDouble()
                val count = state.participantsInput.toInt()
                SplitBillCalculator.calculateSplit(
                    totalAmount = amount,
                    participantCount = count,
                    currencySymbol = currentSymbol
                )
            } else {
                null
            }
        } else {
            null
        }

        state.copy(
            currencySymbol = currentSymbol,
            result = updatedResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplitBillUiState()
    )

    fun onAmountChange(newAmount: String) {
        _internalState.update { current ->
            val validation = if (newAmount.isNotBlank() && current.participantsInput.isNotBlank()) {
                SplitBillCalculator.validateInputs(newAmount, current.participantsInput)
            } else null

            val error = when (validation) {
                is SplitBillValidation.Error -> validation.message
                else -> null
            }

            val result = if (validation is SplitBillValidation.Valid) {
                SplitBillCalculator.calculateSplit(
                    totalAmount = newAmount.toDouble(),
                    participantCount = current.participantsInput.toInt(),
                    currencySymbol = current.currencySymbol
                )
            } else null

            current.copy(
                totalAmountInput = newAmount,
                errorMessage = error,
                result = result,
                isExpenseRecorded = false,
                recordedExpenseMessage = null
            )
        }
    }

    fun onParticipantsChange(newCount: String) {
        _internalState.update { current ->
            val validation = if (current.totalAmountInput.isNotBlank() && newCount.isNotBlank()) {
                SplitBillCalculator.validateInputs(current.totalAmountInput, newCount)
            } else null

            val error = when (validation) {
                is SplitBillValidation.Error -> validation.message
                else -> null
            }

            val result = if (validation is SplitBillValidation.Valid) {
                SplitBillCalculator.calculateSplit(
                    totalAmount = current.totalAmountInput.toDouble(),
                    participantCount = newCount.toInt(),
                    currencySymbol = current.currencySymbol
                )
            } else null

            current.copy(
                participantsInput = newCount,
                errorMessage = error,
                result = result,
                isExpenseRecorded = false,
                recordedExpenseMessage = null
            )
        }
    }

    fun incrementParticipants() {
        val currentCount = _internalState.value.participantsInput.toIntOrNull() ?: 2
        if (currentCount < SplitBillCalculator.MAX_PARTICIPANTS) {
            onParticipantsChange((currentCount + 1).toString())
        }
    }

    fun decrementParticipants() {
        val currentCount = _internalState.value.participantsInput.toIntOrNull() ?: 2
        if (currentCount > SplitBillCalculator.MIN_PARTICIPANTS) {
            onParticipantsChange((currentCount - 1).toString())
        }
    }

    fun onTitleChange(newTitle: String) {
        _internalState.update { it.copy(titleInput = newTitle) }
    }

    fun onCategoryChange(newCategory: ExpenseCategory) {
        _internalState.update { it.copy(selectedCategory = newCategory) }
    }

    /**
     * Explicitly records only the user's portion of the bill as a personal expense.
     * Never called automatically.
     */
    fun recordUserShareAsExpense() {
        val state = uiState.value
        val result = state.result ?: return
        val userShare = result.userShare

        viewModelScope.launch {
            val title = state.titleInput.ifBlank { "Shared Bill (${result.participantCount} ways)" }
            val expense = Expense(
                title = title,
                amount = userShare,
                category = state.selectedCategory.name,
                dateMillis = System.currentTimeMillis()
            )
            expenseRepository.insertExpense(expense)

            val formattedShare = CurrencyFormatter.format(userShare, state.currencySymbol)
            _internalState.update {
                it.copy(
                    isExpenseRecorded = true,
                    recordedExpenseMessage = "Successfully recorded your share ($formattedShare) as an expense!"
                )
            }
        }
    }

    fun clearResult() {
        _internalState.update {
            it.copy(
                totalAmountInput = "",
                participantsInput = "2",
                result = null,
                errorMessage = null,
                isExpenseRecorded = false,
                recordedExpenseMessage = null
            )
        }
    }
}
