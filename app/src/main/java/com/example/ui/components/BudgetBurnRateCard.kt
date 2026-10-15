package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.BurnRateForecast
import com.example.ui.dashboard.BurnRateStatus
import com.example.util.CurrencyFormatter

/**
 * Material 3 Card displaying Budget Burn Rate and Runout Forecast.
 * Shows historical daily spending rate, estimated days remaining, spending pace status pill,
 * progress indicator, contextual advice, and clear estimate disclaimer.
 */
@Composable
fun BudgetBurnRateCard(
    forecast: BurnRateForecast,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (forecast.status) {
        BurnRateStatus.SUSTAINABLE -> Color(0xFF10B981) // Emerald Green
        BurnRateStatus.ON_TRACK -> MaterialTheme.colorScheme.primary // Campus Primary
        BurnRateStatus.EARLY_RUNOUT -> Color(0xFFF59E0B) // Amber Warning
        BurnRateStatus.EXHAUSTED -> Color(0xFFEA580C) // Deep Orange
        BurnRateStatus.OVER_BUDGET -> MaterialTheme.colorScheme.error // Error Red
        BurnRateStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outline
    }

    val statusIcon = when (forecast.status) {
        BurnRateStatus.SUSTAINABLE -> Icons.Default.CheckCircle
        BurnRateStatus.ON_TRACK -> Icons.Default.CheckCircle
        BurnRateStatus.EARLY_RUNOUT -> Icons.Default.Speed
        BurnRateStatus.EXHAUSTED -> Icons.Default.Warning
        BurnRateStatus.OVER_BUDGET -> Icons.Default.Error
        BurnRateStatus.INSUFFICIENT_DATA -> Icons.Default.Info
    }

    val statusPillText = when (forecast.status) {
        BurnRateStatus.SUSTAINABLE -> "Sustainable"
        BurnRateStatus.ON_TRACK -> "On Track"
        BurnRateStatus.EARLY_RUNOUT -> "Early Runout Risk"
        BurnRateStatus.EXHAUSTED -> "Exhausted"
        BurnRateStatus.OVER_BUDGET -> "Over Budget"
        BurnRateStatus.INSUFFICIENT_DATA -> "Need Data"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_burn_rate_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Icon + Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Burn Rate",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BURN RATE & RUNOUT FORECAST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = forecast.statusHeadline,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    modifier = Modifier.testTag("burn_rate_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusPillText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Metric 1: Average Daily Burn Rate
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Burn Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (forecast.status == BurnRateStatus.INSUFFICIENT_DATA) {
                            "—"
                        } else {
                            "${CurrencyFormatter.format(forecast.dailyBurnRate, currencySymbol)}/day"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("burn_rate_value")
                    )
                    Text(
                        text = "Over ${forecast.daysElapsed} day${if (forecast.daysElapsed != 1) "s" else ""} elapsed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metric 2: Estimated Runout
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Est. Budget Longevity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val longevityText = when (forecast.status) {
                        BurnRateStatus.INSUFFICIENT_DATA -> "—"
                        BurnRateStatus.OVER_BUDGET -> "0 days"
                        BurnRateStatus.EXHAUSTED -> "0 days"
                        else -> "~${forecast.estimatedDaysRemaining} days"
                    }
                    Text(
                        text = longevityText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor,
                        modifier = Modifier.testTag("burn_rate_longevity")
                    )
                    Text(
                        text = forecast.formattedRunoutDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("burn_rate_runout_date")
                    )
                }
            }

            // Visual Longevity Comparison Bar (for active spending)
            if (forecast.status in listOf(BurnRateStatus.EARLY_RUNOUT, BurnRateStatus.ON_TRACK, BurnRateStatus.SUSTAINABLE)) {
                Spacer(modifier = Modifier.height(14.dp))
                val safeCalendar = forecast.calendarDaysRemaining.coerceAtLeast(1)
                val paceRatio = (forecast.estimatedDaysRemaining.toFloat() / safeCalendar.toFloat()).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Longevity: ~${forecast.estimatedDaysRemaining} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                        Text(
                            text = "${forecast.calendarDaysRemaining} d remaining in month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { paceRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Status Explanation Message
            if (forecast.statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = forecast.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(10.dp)
                            .testTag("burn_rate_message")
                    )
                }
            }

            // Disclaimer Notice (Requirement 5: Clearly label forecasts as estimates, not guaranteed predictions)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("burn_rate_estimate_disclaimer"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Estimate Disclaimer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = forecast.isEstimateNotice,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}
