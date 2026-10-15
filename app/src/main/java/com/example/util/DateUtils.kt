package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date formatting and month-range calculations.
 */
object DateUtils {

    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val dayMonthFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    fun formatDate(millis: Long): String {
        return fullDateFormat.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        return shortDateFormat.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return monthYearFormat.format(Date(millis))
    }

    fun formatDayMonth(millis: Long): String {
        return dayMonthFormat.format(Date(millis))
    }

    /**
     * Start of current month (00:00:00.000)
     */
    fun getCurrentMonthStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * End of current month (23:59:59.999)
     */
    fun getCurrentMonthEndMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Returns true if two epoch timestamps fall in the same calendar month and year.
     */
    fun isSameMonth(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    /**
     * Calculates the start and end millis for a specific month and year.
     */
    fun getMonthRange(year: Int, monthIndexZeroBased: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndexZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    /**
     * Calculates the number of days remaining in the current month, including today.
     * Guaranteed to return >= 1 for the current month.
     */
    fun getDaysRemainingInMonth(referenceMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDays - currentDay + 1).coerceAtLeast(1)
    }

    /**
     * Returns the total days in the month of the reference timestamp.
     */
    fun getTotalDaysInMonth(referenceMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Calculates the number of days elapsed in the current month, including today.
     * Guaranteed to return >= 1 for the current month.
     */
    fun getDaysElapsedInMonth(referenceMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        return cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }

    /**
     * Calculates the estimated date epoch millis by adding days to a reference date.
     */
    fun addDaysToTimestamp(days: Int, referenceMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    /**
     * Returns the start millis (00:00:00.000) for a 7-day window ending at the reference timestamp.
     */
    fun getSevenDayWindowStartMillis(referenceMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
