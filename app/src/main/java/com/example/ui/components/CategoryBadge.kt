package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseCategory

/**
 * Visual badge for expense categories with colored background and icon.
 */
@Composable
fun CategoryBadge(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val backgroundColor = category.color.copy(alpha = 0.15f)
    val contentColor = category.color

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

/**
 * Circular avatar icon for categories.
 */
@Composable
fun CategoryIconCircle(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
    sizeDp: Int = 40
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(category.color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = category.color,
            modifier = Modifier.size((sizeDp * 0.55).dp)
        )
    }
}
