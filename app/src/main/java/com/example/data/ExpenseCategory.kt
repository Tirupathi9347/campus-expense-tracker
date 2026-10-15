package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Standard campus expense categories tailored for student life.
 * Maps to Unit 3 (Display Lists and Material Design) and Unit 6 (Data Persistence).
 */
enum class ExpenseCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    FOOD(
        displayName = "Food & Dining",
        icon = Icons.Default.Restaurant,
        color = Color(0xFFEF4444) // Coral red
    ),
    BOOKS(
        displayName = "Books & Supplies",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        color = Color(0xFF3B82F6) // Blue
    ),
    HOUSING(
        displayName = "Housing & Dorm",
        icon = Icons.Default.Home,
        color = Color(0xFF8B5CF6) // Purple
    ),
    TUITION(
        displayName = "Tuition & Fees",
        icon = Icons.Default.School,
        color = Color(0xFF059669) // Emerald
    ),
    TRANSPORT(
        displayName = "Transit & Travel",
        icon = Icons.Default.DirectionsBus,
        color = Color(0xFFF59E0B) // Amber
    ),
    ENTERTAINMENT(
        displayName = "Social & Fun",
        icon = Icons.Default.SportsEsports,
        color = Color(0xFFEC4899) // Pink
    ),
    HEALTH(
        displayName = "Health & Gym",
        icon = Icons.Default.FitnessCenter,
        color = Color(0xFF10B981) // Teal
    ),
    TECH(
        displayName = "Tech & Tools",
        icon = Icons.Default.Laptop,
        color = Color(0xFF6366F1) // Indigo
    ),
    OTHER(
        displayName = "Other Campus",
        icon = Icons.Default.Category,
        color = Color(0xFF64748B) // Slate
    );

    companion object {
        fun fromName(name: String): ExpenseCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: OTHER
        }
    }
}
