package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DailyProductivity
import com.example.model.SubjectAnalytics

@Composable
fun SubjectMasteryChart(
    items: List<SubjectAnalytics>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { stat ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.subject.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (stat.attempts == 0) "Belum dicoba"
                        else "${stat.accuracyPct}% • ${stat.attempts} sesi (${stat.totalXp} XP)",
                        style = MaterialTheme.typography.labelMedium,
                        color = stat.subject.color
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = CornerRadius(12f, 12f),
                        size = size
                    )
                    val fraction = (stat.accuracyPct / 100f).coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(stat.subject.color.copy(alpha = 0.75f), stat.subject.color)
                            ),
                            cornerRadius = CornerRadius(12f, 12f),
                            size = Size(size.width * fraction, size.height)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyProductivityChart(
    days: List<DailyProductivity>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxVal = maxOf(15, days.maxOf { it.minutesStudied })
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val targetLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 4.dp)
        ) {
            val barCount = days.size
            val slotWidth = size.width / barCount
            val barWidth = slotWidth * 0.52f

            // Target 15 mins/day dashed line
            val targetRatio = (15f / maxVal.toFloat()).coerceIn(0f, 1f)
            val targetY = size.height * (1f - targetRatio)
            drawLine(
                color = targetLineColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            days.forEachIndexed { index, day ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.45f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                val ratio = (day.minutesStudied.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val h = size.height * ratio
                if (h > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(secondaryColor, primaryColor)),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${day.minutesStudied}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
