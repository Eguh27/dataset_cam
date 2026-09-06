package com.example.ui.components.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.theme.WarmPlumSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RechartsDonutChart(
    items: List<AspectRatioSliceItem>,
    totalSamples: Int,
    onRatioSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedSlice by remember { mutableStateOf<AspectRatioSliceItem?>(null) }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_animation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(WarmPlumSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("recharts_donut_chart_card")
    ) {
        // Header with Recharts badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Distribusi Rasio Aspek",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Proporsi rasio dimensi untuk model vision AI",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Recharts Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33C084FC))
                    .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Recharts • Donut",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonPurple,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty() || totalSamples == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x18FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada data rasio aspek dalam dataset",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            // Main Donut Area + Center Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Interactive Donut Canvas
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidthPx = 24.dp.toPx()
                        val selectedStrokeWidthPx = 30.dp.toPx()
                        val diameter = minOf(size.width, size.height) - selectedStrokeWidthPx
                        val arcSize = Size(diameter, diameter)
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        var startAngle = -90f
                        val gapAngle = if (items.size > 1) 3f else 0f

                        items.forEach { slice ->
                            val isSelected = selectedSlice?.ratioKey == slice.ratioKey
                            val rawSweep = (slice.percentage / 100f) * 360f
                            val sweep = (rawSweep - gapAngle).coerceAtLeast(1f) * animProgress

                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) selectedStrokeWidthPx else strokeWidthPx,
                                    cap = StrokeCap.Round
                                )
                            )

                            if (isSelected) {
                                // Outer subtle glow highlight
                                drawArc(
                                    color = slice.color.copy(alpha = 0.35f),
                                    startAngle = startAngle - 1f,
                                    sweepAngle = sweep + 2f,
                                    useCenter = false,
                                    topLeft = topLeft - Offset(4.dp.toPx(), 4.dp.toPx()),
                                    size = Size(diameter + 8.dp.toPx(), diameter + 8.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            startAngle += rawSweep
                        }
                    }

                    // Center Cutout Text: Total Count or Selected Slice
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(WarmPlumSurfaceElevated.copy(alpha = 0.85f))
                            .clickable { selectedSlice = null }
                    ) {
                        if (selectedSlice != null) {
                            Text(
                                text = selectedSlice?.ratioKey ?: "",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = selectedSlice?.color ?: Color.White
                            )
                            Text(
                                text = "${selectedSlice?.count} foto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = String.format("%.1f%%", selectedSlice?.percentage ?: 0f),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        } else {
                            Text(
                                text = "$totalSamples",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Total Sampel",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Interactive Mini Ratio Aspect Box Preview
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { slice ->
                        val isSelected = selectedSlice?.ratioKey == slice.ratioKey

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) slice.color.copy(alpha = 0.2f)
                                    else Color(0x1AFFFFFF)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) slice.color else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedSlice = if (selectedSlice?.ratioKey == slice.ratioKey) null else slice
                                    onRatioSelected?.invoke(slice.ratioKey)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("ratio_legend_${slice.ratioKey}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Wireframe preview icon for ratio
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = when (slice.ratioKey) {
                                                "16:9" -> 18.dp
                                                "4:3" -> 16.dp
                                                else -> 14.dp
                                            },
                                            height = when (slice.ratioKey) {
                                                "9:16" -> 18.dp
                                                "3:4" -> 16.dp
                                                else -> 14.dp
                                            }
                                        )
                                        .border(1.dp, slice.color, RoundedCornerShape(2.dp))
                                        .background(slice.color.copy(alpha = 0.25f))
                                )

                                Text(
                                    text = slice.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${slice.count}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "(${String.format("%.0f%%", slice.percentage)})",
                                    fontSize = 10.sp,
                                    color = slice.color,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Tooltip Card for Aspect Ratio Details
            AnimatedVisibility(
                visible = selectedSlice != null,
                enter = fadeIn() + androidx.compose.animation.expandVertically(),
                exit = fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                selectedSlice?.let { slice ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WarmPlumSurfaceElevated)
                            .border(1.dp, slice.color.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .testTag("recharts_donut_tooltip")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Text(
                                        text = "Rasio ${slice.label}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${slice.count} foto (${String.format("%.1f%%", slice.percentage)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slice.color
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rekomendasi Arsitektur ML: ${slice.recommendedModels}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            IconButton(
                                onClick = { selectedSlice = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Tooltip",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
