package com.example

import com.example.data.ExpenseCategory
import com.example.ui.entry.ExpenseDetails
import com.example.ui.entry.ExpenseValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for input validation and clear user error messages.
 * Maps to Unit 2 (Building App UI) and Unit 4 (ViewModel Architecture).
 */
class ExpenseValidationTest {

    @Test
    fun validate_blankTitle_returnsInvalid() {
        val details = ExpenseDetails(
            title = "   ",
            amount = "25.00",
            category = ExpenseCategory.FOOD.name,
            dateMillis = System.currentTimeMillis()
        )
        val result = ExpenseValidator.validate(details)
        assertTrue(result is ExpenseValidator.ValidationResult.Invalid)
        assertEquals("Please enter a title for this expense.", (result as ExpenseValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun validate_invalidAmount_returnsInvalid() {
        val details = ExpenseDetails(
            title = "Coffee",
            amount = "abc",
            category = ExpenseCategory.FOOD.name,
            dateMillis = System.currentTimeMillis()
        )
        val result = ExpenseValidator.validate(details)
        assertTrue(result is ExpenseValidator.ValidationResult.Invalid)
        assertEquals("Please enter a valid numeric amount.", (result as ExpenseValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun validate_zeroOrNegativeAmount_returnsInvalid() {
        val zeroDetails = ExpenseDetails(
            title = "Dorm Laundry",
            amount = "0",
            category = ExpenseCategory.HOUSING.name,
            dateMillis = System.currentTimeMillis()
        )
        val zeroResult = ExpenseValidator.validate(zeroDetails)
        assertTrue(zeroResult is ExpenseValidator.ValidationResult.Invalid)
        assertEquals("Expense amount must be greater than 0.", (zeroResult as ExpenseValidator.ValidationResult.Invalid).message)

        val negativeDetails = ExpenseDetails(
            title = "Transit",
            amount = "-5.50",
            category = ExpenseCategory.TRANSPORT.name,
            dateMillis = System.currentTimeMillis()
        )
        val negResult = ExpenseValidator.validate(negativeDetails)
        assertTrue(negResult is ExpenseValidator.ValidationResult.Invalid)
        assertEquals("Expense amount must be greater than 0.", (negResult as ExpenseValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun validate_validInput_returnsValid() {
        val details = ExpenseDetails(
            title = "Chemistry Lab Manual",
            amount = "34.50",
            category = ExpenseCategory.BOOKS.name,
            dateMillis = System.currentTimeMillis(),
            note = "Required for CHEM 101"
        )
        val result = ExpenseValidator.validate(details)
        assertTrue(result is ExpenseValidator.ValidationResult.Valid)
    }
}
