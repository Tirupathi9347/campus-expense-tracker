package com.example.ui.splitbill

import com.example.util.CurrencyFormatter
import kotlin.math.roundToLong

data class ParticipantShare(
    val participantIndex: Int,
    val participantName: String,
    val shareAmount: Double,
    val isPrimaryUser: Boolean,
    val formattedShare: String,
    val hasRemainderPenny: Boolean = false
)

data class SplitBillResult(
    val totalAmount: Double,
    val participantCount: Int,
    val baseShare: Double,
    val remainderCents: Int,
    val participants: List<ParticipantShare>,
    val userShare: Double,
    val formattedTotal: String,
    val formattedUserShare: String,
    val isEvenSplit: Boolean
)

sealed class SplitBillValidation {
    object Valid : SplitBillValidation()
    data class Error(val message: String) : SplitBillValidation()
}

/**
 * Pure calculation logic for Split-Bill computations.
 * Implements exact cent/paisa rounding distribution and input validation.
 */
object SplitBillCalculator {
    const val MIN_PARTICIPANTS = 2
    const val MAX_PARTICIPANTS = 100
    const val MAX_BILL_AMOUNT = 10_000_000.0

    /**
     * Validates bill amount and participant count inputs.
     */
    fun validateInputs(
        amountStr: String,
        participantsStr: String
    ): SplitBillValidation {
        val trimmedAmount = amountStr.trim()
        val trimmedParticipants = participantsStr.trim()

        if (trimmedAmount.isBlank()) {
            return SplitBillValidation.Error("Please enter a total bill amount.")
        }
        val amount = trimmedAmount.toDoubleOrNull()
            ?: return SplitBillValidation.Error("Please enter a valid numeric amount.")
        if (amount <= 0.0) {
            return SplitBillValidation.Error("Bill amount must be greater than zero.")
        }
        if (amount > MAX_BILL_AMOUNT) {
            return SplitBillValidation.Error("Bill amount exceeds maximum allowable limit.")
        }

        if (trimmedParticipants.isBlank()) {
            return SplitBillValidation.Error("Please enter the number of participants.")
        }
        val participants = trimmedParticipants.toIntOrNull()
            ?: return SplitBillValidation.Error("Please enter a valid whole number for participants.")
        if (participants < MIN_PARTICIPANTS) {
            return SplitBillValidation.Error("A split bill requires at least $MIN_PARTICIPANTS participants.")
        }
        if (participants > MAX_PARTICIPANTS) {
            return SplitBillValidation.Error("Maximum allowed participant count is $MAX_PARTICIPANTS.")
        }

        return SplitBillValidation.Valid
    }

    /**
     * Splits total bill across participants with exact cent/paisa distribution.
     * Guarantees: sum of all participant shares == totalAmount.
     */
    fun calculateSplit(
        totalAmount: Double,
        participantCount: Int,
        currencySymbol: String = "₹",
        customNames: List<String> = emptyList()
    ): SplitBillResult {
        require(participantCount >= MIN_PARTICIPANTS) { "Participants must be at least $MIN_PARTICIPANTS" }
        require(totalAmount > 0.0) { "Total amount must be greater than 0" }

        val totalCents = (totalAmount * 100.0).roundToLong()
        val baseShareCents = totalCents / participantCount
        val remainderCents = (totalCents % participantCount).toInt()

        val participants = (0 until participantCount).map { i ->
            val hasExtraPenny = i < remainderCents
            val shareCents = baseShareCents + if (hasExtraPenny) 1 else 0
            val shareAmount = shareCents / 100.0
            val isUser = i == 0
            val customName = customNames.getOrNull(i)?.takeIf { it.isNotBlank() }
            val name = customName ?: if (isUser) "You (Your Share)" else "Person ${i + 1}"

            ParticipantShare(
                participantIndex = i,
                participantName = name,
                shareAmount = shareAmount,
                isPrimaryUser = isUser,
                formattedShare = CurrencyFormatter.format(shareAmount, currencySymbol),
                hasRemainderPenny = hasExtraPenny
            )
        }

        val userShare = participants.first().shareAmount

        return SplitBillResult(
            totalAmount = totalAmount,
            participantCount = participantCount,
            baseShare = baseShareCents / 100.0,
            remainderCents = remainderCents,
            participants = participants,
            userShare = userShare,
            formattedTotal = CurrencyFormatter.format(totalAmount, currencySymbol),
            formattedUserShare = CurrencyFormatter.format(userShare, currencySymbol),
            isEvenSplit = remainderCents == 0
        )
    }
}
