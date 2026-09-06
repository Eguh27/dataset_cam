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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.theme.WarmPlumSurfaceElevated

@Composable
fun RechartsBarChart(
    items: List<CategoryBarItem>,
    totalSamples: Int,
    onCategoryClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPercentageMode by remember { mutableStateOf(false) }
    var sortByCountDesc by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<CategoryBarItem?>(null) }

    val sortedItems = remember(items, sortByCountDesc) {
        if (sortByCountDesc) {
            items.sortedByDescending { it.count }
        } else {
            items.sortedBy { it.name.lowercase() }
        }
    }

    val maxCount = remember(items) {
        val max = items.maxOfOrNull { it.count } ?: 0
        if (max == 0) 1 else max
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(WarmPlumSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("recharts_bar_chart_card")
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
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Jumlah Gambar per Kategori",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Distribusi volume sampel per kelas target",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Recharts Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3338BDF8))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Recharts • Bar",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controls bar: Toggle Unit (Count / %) and Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unit Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isPercentageMode) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { isPercentageMode = false }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sampel",
                        fontSize = 11.sp,
                        fontWeight = if (!isPercentageMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isPercentageMode) NeonCyan else TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isPercentageMode) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { isPercentageMode = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "% Porsi",
                        fontSize = 11.sp,
                        fontWeight = if (isPercentageMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPercentageMode) NeonCyan else TextSecondary
                    )
                }
            }

            // Sort button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { sortByCountDesc = !sortByCountDesc }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Urutkan",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (sortByCountDesc) "Jumlah ↓" else "Nama A-Z",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sortedItems.isEmpty() || totalSamples == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x18FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Belum ada sampel foto dalam kategori",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Ambil foto atau gunakan generator sampel",
                        fontSize = 11.sp,
                        color = NeonCyan.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // Interactive Cartesian Coordinate Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Background Cartesian Grid lines & Y-Axis Scale
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height - 30.dp.toPx() // Leave space for X-axis labels
                    val steps = 4
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    for (i in 0..steps) {
                        val y = h * (1f - i.toFloat() / steps)
                        drawLine(
                            color = Color(0x20FFFFFF),
                            start = Offset(32.dp.toPx(), y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )
                    }
                }

                // Y-Axis Ticks
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 30.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val step1 = if (isPercentageMode) "100%" else "$maxCount"
                    val step2 = if (isPercentageMode) "75%" else "${(maxCount * 3) / 4}"
                    val step3 = if (isPercentageMode) "50%" else "${maxCount / 2}"
                    val step4 = if (isPercentageMode) "25%" else "${maxCount / 4}"
                    val step5 = if (isPercentageMode) "0%" else "0"

                    listOf(step1, step2, step3, step4, step5).forEach { label ->
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(30.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Scrollable/Flexible Bars Column Container
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 36.dp, bottom = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    sortedItems.forEach { item ->
                        val isSelected = selectedItem?.classId == item.classId
                        val fraction = if (maxCount > 0) (item.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f) else 0f
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                            label = "bar_height_${item.name}"
                        )

                        val displayVal = if (isPercentageMode) {
                            String.format("%.1f%%", item.percentage)
                        } else {
                            "${item.count}"
                        }

                        Column(
                            modifier = Modifier
                                .width(if (sortedItems.size <= 4) 64.dp else 52.dp)
                                .fillMaxHeight()
                                .clickable {
                                    selectedItem = if (selectedItem?.classId == item.classId) null else item
                                }
                                .testTag("bar_item_${item.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Value text floating over bar
                            Text(
                                text = displayVal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Bar Graphic Container
                            Box(
                                modifier = Modifier
                                    .width(if (sortedItems.size <= 4) 38.dp else 30.dp)
                                    .fillMaxHeight(0.68f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                // Background subtle bar slot
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x15FFFFFF))
                                )

                                // Filled animated Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(animatedFraction.coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    item.color,
                                                    item.color.copy(alpha = 0.35f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else item.color.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-Axis Category Name with colored dot
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Recharts Floating Tooltip Card
            AnimatedVisibility(
                visible = selectedItem != null,
                enter = fadeIn() + androidx.compose.animation.expandVertically(),
                exit = fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                selectedItem?.let { activeItem ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(WarmPlumSurfaceElevated)
                            .border(1.dp, activeItem.color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .testTag("recharts_bar_tooltip")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                            .background(activeItem.color)
                                    )
                                    Text(
                                        text = activeItem.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${activeItem.count} gambar (${String.format("%.1f%%", activeItem.percentage)})",
                                        fontSize = 12.sp,
                                        color = activeItem.color,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (activeItem.aspectRatioBreakdown.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Rasio: " + activeItem.aspectRatioBreakdown.entries.joinToString(" • ") { "${it.key}: ${it.value}" },
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (onCategoryClick != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(activeItem.color.copy(alpha = 0.2f))
                                            .clickable { onCategoryClick(activeItem.classId) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Lihat Galeri",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeItem.color
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = activeItem.color,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { selectedItem = null },
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
}
