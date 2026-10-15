package com.example

import com.example.ui.splitbill.SplitBillCalculator
import com.example.ui.splitbill.SplitBillValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Unit tests for Feature 4: Split-Bill Calculator.
 * Tests equal splits, uneven splits, rounding remainders, invalid inputs,
 * participant limits, and supported currencies.
 */
class SplitBillCalculationTest {

    @Test
    fun calculateSplit_equalSplit_distributesExactEqualShares() {
        val result = SplitBillCalculator.calculateSplit(
            totalAmount = 60.0,
            participantCount = 3,
            currencySymbol = "₹"
        )
        assertEquals(3, result.participants.size)
        assertEquals(20.0, result.userShare, 0.001)
        assertEquals(0, result.remainderCents)
        assertTrue(result.isEvenSplit)

        // Sum of all shares must exactly equal total amount
        val sum = result.participants.sumOf { it.shareAmount }
        assertEquals(60.0, sum, 0.0001)
        result.participants.forEach {
            assertEquals(20.0, it.shareAmount, 0.001)
            assertEquals("₹20.00", it.formattedShare)
            assertFalse(it.hasRemainderPenny)
        }
    }

    @Test
    fun calculateSplit_unevenSplit_threeParticipants_distributesOneCentRemainder() {
        // ₹100 split 3 ways -> 1 person pays ₹33.34, 2 people pay ₹33.33
        val result = SplitBillCalculator.calculateSplit(
            totalAmount = 100.0,
            participantCount = 3,
            currencySymbol = "₹"
        )
        assertEquals(3, result.participants.size)
        assertEquals(1, result.remainderCents)
        assertFalse(result.isEvenSplit)

        val sum = result.participants.sumOf { it.shareAmount }
        assertEquals(100.0, sum, 0.0001)

        assertEquals(33.34, result.participants[0].shareAmount, 0.001)
        assertEquals("₹33.34", result.participants[0].formattedShare)
        assertTrue(result.participants[0].hasRemainderPenny)

        assertEquals(33.33, result.participants[1].shareAmount, 0.001)
        assertEquals("₹33.33", result.participants[1].formattedShare)
        assertFalse(result.participants[1].hasRemainderPenny)

        assertEquals(33.33, result.participants[2].shareAmount, 0.001)
        assertEquals("₹33.33", result.participants[2].formattedShare)
        assertFalse(result.participants[2].hasRemainderPenny)
    }

    @Test
    fun calculateSplit_unevenSplit_sevenParticipants_distributesMultipleCentRemainder() {
        // ₹100 split 7 ways: 10000 cents / 7 = 1428 cents (₹14.28), remainder = 4 cents
        // 4 participants pay ₹14.29, 3 participants pay ₹14.28
        val result = SplitBillCalculator.calculateSplit(
            totalAmount = 100.0,
            participantCount = 7,
            currencySymbol = "₹"
        )
        assertEquals(7, result.participants.size)
        assertEquals(4, result.remainderCents)

        val sum = result.participants.sumOf { it.shareAmount }
        assertEquals(100.0, sum, 0.0001)

        val extraPayers = result.participants.filter { it.hasRemainderPenny }
        val basePayers = result.participants.filter { !it.hasRemainderPenny }
        assertEquals(4, extraPayers.size)
        assertEquals(3, basePayers.size)

        extraPayers.forEach { assertEquals(14.29, it.shareAmount, 0.001) }
        basePayers.forEach { assertEquals(14.28, it.shareAmount, 0.001) }
    }

    @Test
    fun calculateSplit_smallAmount_distributesAccurately() {
        val result = SplitBillCalculator.calculateSplit(
            totalAmount = 0.05,
            participantCount = 2,
            currencySymbol = "₹"
        )
        assertEquals(2, result.participants.size)
        assertEquals(0.03, result.participants[0].shareAmount, 0.001)
        assertEquals(0.02, result.participants[1].shareAmount, 0.001)

        val sum = result.participants.sumOf { it.shareAmount }
        assertEquals(0.05, sum, 0.0001)
    }

    @Test
    fun calculateSplit_supportedCurrencies_inrAndUsd() {
        val inrResult = SplitBillCalculator.calculateSplit(
            totalAmount = 50.0,
            participantCount = 2,
            currencySymbol = "₹"
        )
        assertEquals("₹50.00", inrResult.formattedTotal)
        assertEquals("₹25.00", inrResult.formattedUserShare)

        val usdResult = SplitBillCalculator.calculateSplit(
            totalAmount = 50.0,
            participantCount = 2,
            currencySymbol = "$"
        )
        assertEquals("$50.00", usdResult.formattedTotal)
        assertEquals("$25.00", usdResult.formattedUserShare)
    }

    @Test
    fun calculateSplit_customParticipantNames_assignsNamesCorrectly() {
        val names = listOf("Alex", "Sam", "Taylor")
        val result = SplitBillCalculator.calculateSplit(
            totalAmount = 90.0,
            participantCount = 3,
            currencySymbol = "₹",
            customNames = names
        )
        assertEquals("Alex", result.participants[0].participantName)
        assertTrue(result.participants[0].isPrimaryUser)
        assertEquals("Sam", result.participants[1].participantName)
        assertFalse(result.participants[1].isPrimaryUser)
        assertEquals("Taylor", result.participants[2].participantName)
        assertFalse(result.participants[2].isPrimaryUser)
    }

    @Test
    fun validateInputs_validInputs_returnsValid() {
        val validation = SplitBillCalculator.validateInputs(
            amountStr = "1250.50",
            participantsStr = "4"
        )
        assertTrue(validation is SplitBillValidation.Valid)
    }

    @Test
    fun validateInputs_emptyInputs_returnsError() {
        val emptyAmount = SplitBillCalculator.validateInputs("", "4")
        assertTrue(emptyAmount is SplitBillValidation.Error)
        assertTrue((emptyAmount as SplitBillValidation.Error).message.contains("bill amount"))

        val emptyParticipants = SplitBillCalculator.validateInputs("100.0", "")
        assertTrue(emptyParticipants is SplitBillValidation.Error)
        assertTrue((emptyParticipants as SplitBillValidation.Error).message.contains("participants"))
    }

    @Test
    fun validateInputs_zeroOrNegativeAmount_returnsError() {
        val zeroAmount = SplitBillCalculator.validateInputs("0", "3")
        assertTrue(zeroAmount is SplitBillValidation.Error)
        assertTrue((zeroAmount as SplitBillValidation.Error).message.contains("greater than zero"))

        val negativeAmount = SplitBillCalculator.validateInputs("-25.50", "3")
        assertTrue(negativeAmount is SplitBillValidation.Error)
        assertTrue((negativeAmount as SplitBillValidation.Error).message.contains("greater than zero"))
    }

    @Test
    fun validateInputs_invalidParticipantLimits_returnsError() {
        val onePerson = SplitBillCalculator.validateInputs("100", "1")
        assertTrue(onePerson is SplitBillValidation.Error)
        assertTrue((onePerson as SplitBillValidation.Error).message.contains("at least 2"))

        val zeroPersons = SplitBillCalculator.validateInputs("100", "0")
        assertTrue(zeroPersons is SplitBillValidation.Error)

        val tooManyPersons = SplitBillCalculator.validateInputs("100", "101")
        assertTrue(tooManyPersons is SplitBillValidation.Error)
        assertTrue((tooManyPersons as SplitBillValidation.Error).message.contains("Maximum allowed"))
    }

    @Test
    fun validateInputs_excessivelyLargeAmount_returnsError() {
        val largeAmount = SplitBillCalculator.validateInputs("10000001", "2")
        assertTrue(largeAmount is SplitBillValidation.Error)
        assertTrue((largeAmount as SplitBillValidation.Error).message.contains("maximum allowable limit"))
    }

    @Test
    fun validateInputs_nonNumeric_returnsError() {
        val textAmount = SplitBillCalculator.validateInputs("not_a_number", "2")
        assertTrue(textAmount is SplitBillValidation.Error)

        val textParticipants = SplitBillCalculator.validateInputs("100", "three")
        assertTrue(textParticipants is SplitBillValidation.Error)
    }
}
