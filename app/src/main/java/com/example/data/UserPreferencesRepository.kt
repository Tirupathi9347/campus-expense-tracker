package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "campus_user_preferences")

enum class AppTheme(val storageKey: String, val displayName: String) {
    SYSTEM("SYSTEM", "System Default"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark");

    companion object {
        fun fromStorageKey(key: String?): AppTheme {
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

data class UserPreferences(
    val monthlyBudget: Double = 5000.0,
    val currencySymbol: String = "₹",
    val campusName: String = "University Campus",
    val appTheme: AppTheme = AppTheme.SYSTEM
)

/**
 * Repository for managing user preferences via Jetpack Preferences DataStore.
 */
class UserPreferencesRepository(
    private val context: Context
) {
    companion object {
        val KEY_MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val KEY_CAMPUS_NAME = stringPreferencesKey("campus_name")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val budget = preferences[KEY_MONTHLY_BUDGET] ?: 5000.0
            val currency = preferences[KEY_CURRENCY_SYMBOL] ?: "₹"
            val campus = preferences[KEY_CAMPUS_NAME] ?: "University Campus"
            val themeStr = preferences[KEY_APP_THEME] ?: AppTheme.SYSTEM.storageKey
            UserPreferences(
                monthlyBudget = budget,
                currencySymbol = currency,
                campusName = campus,
                appTheme = AppTheme.fromStorageKey(themeStr)
            )
        }

    suspend fun saveMonthlyBudget(budget: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MONTHLY_BUDGET] = budget
        }
    }

    suspend fun saveCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun saveCampusName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CAMPUS_NAME] = name
        }
    }

    suspend fun saveAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme.storageKey
        }
    }
}
