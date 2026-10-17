package com.example
 
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.Crossfade
import com.example.ui.components.BrandedSplashScreen
import com.example.ui.navigation.CampusExpenseNavHost
import com.example.ui.theme.CampusExpenseTheme

/**
 * Single-Activity host for the Campus Expense Tracker app with Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusExpenseApplication
        val userPreferencesRepository = app.container.userPreferencesRepository
        setContent {
            val userPrefsOrNull by userPreferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = null
            )

            Crossfade(
                targetState = userPrefsOrNull,
                label = "AppStartupTransition"
            ) { prefs ->
                if (prefs == null) {
                    BrandedSplashScreen()
                } else {
                    CampusExpenseTheme(appTheme = prefs.appTheme) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            CampusExpenseNavHost()
                        }
                    }
                }
            }
        }
    }
}

