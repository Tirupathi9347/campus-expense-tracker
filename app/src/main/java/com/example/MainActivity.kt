package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.CampusExpenseNavHost
import com.example.ui.theme.CampusExpenseTheme

/**
 * Single-Activity host for the Campus Expense Tracker app.
 * Follows Android Basics with Compose single-activity architecture with Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusExpenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CampusExpenseNavHost()
                }
            }
        }
    }
}
