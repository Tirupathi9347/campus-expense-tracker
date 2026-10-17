package com.example

import com.example.data.AppTheme
import com.example.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {

    @Test
    fun defaultUserPreferences_usesSystemTheme() {
        val prefs = UserPreferences()
        assertEquals(AppTheme.SYSTEM, prefs.appTheme)
        assertEquals("₹", prefs.currencySymbol)
    }

    @Test
    fun appTheme_storageKeyMapping_isCorrect() {
        assertEquals("SYSTEM", AppTheme.SYSTEM.storageKey)
        assertEquals("LIGHT", AppTheme.LIGHT.storageKey)
        assertEquals("DARK", AppTheme.DARK.storageKey)
    }

    @Test
    fun appTheme_fromStorageKey_returnsCorrectEnum() {
        assertEquals(AppTheme.LIGHT, AppTheme.fromStorageKey("LIGHT"))
        assertEquals(AppTheme.LIGHT, AppTheme.fromStorageKey("light"))
        assertEquals(AppTheme.DARK, AppTheme.fromStorageKey("DARK"))
        assertEquals(AppTheme.DARK, AppTheme.fromStorageKey("dark"))
        // Legacy or unknown keys gracefully fall back to SYSTEM
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStorageKey("OG_SHINE"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStorageKey("og_shine"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStorageKey("SYSTEM"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStorageKey("UNKNOWN"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStorageKey(null))
    }

    @Test
    fun themeSelection_isIndependentOfCurrency() {
        val prefs1 = UserPreferences(currencySymbol = "$", appTheme = AppTheme.LIGHT)
        val prefs2 = UserPreferences(currencySymbol = "$", appTheme = AppTheme.DARK)
        val prefs3 = UserPreferences(currencySymbol = "€", appTheme = AppTheme.SYSTEM)

        assertEquals("$", prefs1.currencySymbol)
        assertEquals(AppTheme.LIGHT, prefs1.appTheme)

        assertEquals("$", prefs2.currencySymbol)
        assertEquals(AppTheme.DARK, prefs2.appTheme)

        assertEquals("€", prefs3.currencySymbol)
        assertEquals(AppTheme.SYSTEM, prefs3.appTheme)
    }
}
