package com.example.ui.screens.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DatasetClassEntity
import com.example.data.db.DatasetSampleEntity
import com.example.ui.components.GlassBox
import com.example.ui.components.charts.AspectRatioSliceItem
import com.example.ui.components.charts.CategoryBarItem
import com.example.ui.components.charts.DatasetHealthIndex
import com.example.ui.components.charts.RechartsBarChart
import com.example.ui.components.charts.RechartsDonutChart
import com.example.ui.components.charts.RechartsPalette
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumCanvas
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.theme.WarmPlumSurfaceElevated
import com.example.ui.viewmodel.DatasetViewModel

enum class SplitFilter(val label: String) {
    ALL("Semua"),
    TRAIN("Train"),
    VAL("Val"),
    TEST("Test")
}

@Composable
fun DatasetStatsScreen(
    viewModel: DatasetViewModel,
    onNavigateToExplorer: ((Long?) -> Unit)? = null,
    onNavigateToStudio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsState()
    val allSamples by viewModel.allSamples.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var selectedSplit by remember { mutableStateOf(SplitFilter.ALL) }

    // Filter samples according to selected split
    val currentSamples = remember(allSamples, selectedSplit) {
        when (selectedSplit) {
            SplitFilter.ALL -> allSamples
            SplitFilter.TRAIN -> allSamples.filter { it.split.equals("TRAIN", ignoreCase = true) }
            SplitFilter.VAL -> allSamples.filter { it.split.equals("VAL", ignoreCase = true) }
            SplitFilter.TEST -> allSamples.filter { it.split.equals("TEST", ignoreCase = true) }
        }
    }

    val totalSampleCount = currentSamples.size
    val totalStorageBytes = remember(currentSamples) {
        currentSamples.sumOf { it.fileSizeBytes }
    }
    val storageMbStr = remember(totalStorageBytes) {
        String.format("%.2f MB", totalStorageBytes / (1024.0 * 1024.0))
    }

    // 1. Prepare Category Distribution Data for Recharts Bar Chart
    val categoryBarItems = remember(allClasses, currentSamples, totalSampleCount) {
        val samplesByClass = currentSamples.groupBy { it.className }
        val items = mutableListOf<CategoryBarItem>()

        allClasses.forEachIndexed { index, cls ->
            val classSamples = samplesByClass[cls.name] ?: emptyList()
            val count = classSamples.size
            val pct = if (totalSampleCount > 0) (count.toFloat() / totalSampleCount * 100f) else 0f
            val ratioBreakdown = classSamples.groupingBy { it.aspectRatio }.eachCount()

            items.add(
                CategoryBarItem(
                    classId = cls.id,
                    name = cls.name,
                    count = count,
                    percentage = pct,
                    color = RechartsPalette.parseColor(cls.colorHex, index),
                    aspectRatioBreakdown = ratioBreakdown
                )
            )
        }

        // Include any orphan class names if present in samples
        samplesByClass.forEach { (name, samples) ->
            if (allClasses.none { it.name == name }) {
                val count = samples.size
                val pct = if (totalSampleCount > 0) (count.toFloat() / totalSampleCount * 100f) else 0f
                val ratioBreakdown = samples.groupingBy { it.aspectRatio }.eachCount()
                items.add(
                    CategoryBarItem(
                        classId = -1L,
                        name = name,
                        count = count,
                        percentage = pct,
                        color = RechartsPalette.FallbackColors[items.size % RechartsPalette.FallbackColors.size],
                        aspectRatioBreakdown = ratioBreakdown
                    )
                )
            }
        }
        items
    }

    // 2. Prepare Aspect Ratio Distribution Data for Recharts Donut Chart
    val aspectRatioItems = remember(currentSamples, totalSampleCount) {
        if (totalSampleCount == 0) {
            emptyList()
        } else {
            val ratioGroups = currentSamples.groupBy { it.aspectRatio }
            val colorMap = mapOf(
                "1:1" to NeonCyan,
                "4:3" to NeonPurple,
                "16:9" to NeonRose,
                "9:16" to NeonAmber,
                "3:4" to NeonEmerald
            )
            val modelMap = mapOf(
                "1:1" to "Optimal untuk YOLOv8, Vision Transformer (ViT), ResNet50, MobileNetV3",
                "4:3" to "Standar klasifikasi citra objek & kamera industri",
                "16:9" to "Cocok untuk deteksi horizontal, lanskap, dan frame video surveillance",
                "9:16" to "Optimal untuk pemindaian potret dokumen & mobile UI",
                "3:4" to "Format potret standar fotografi komersial"
            )

            ratioGroups.entries.mapIndexed { idx, entry ->
                val ratioKey = entry.key.ifEmpty { "1:1" }
                val count = entry.value.size
                val pct = (count.toFloat() / totalSampleCount * 100f)
                val label = when (ratioKey) {
                    "1:1" -> "1:1 Square"
                    "4:3" -> "4:3 Standard"
                    "16:9" -> "16:9 Wide"
                    "9:16" -> "9:16 Portrait"
                    "3:4" -> "3:4 Tall"
                    else -> "$ratioKey Ratio"
                }

                AspectRatioSliceItem(
                    ratioKey = ratioKey,
                    label = label,
                    count = count,
                    percentage = pct,
                    color = colorMap[ratioKey] ?: RechartsPalette.FallbackColors[idx % RechartsPalette.FallbackColors.size],
                    recommendedModels = modelMap[ratioKey] ?: "Model Computer Vision umum"
                )
            }.sortedByDescending { it.count }
        }
    }

    // 3. Dominant Ratio
    val dominantRatio = remember(aspectRatioItems) {
        aspectRatioItems.firstOrNull()?.let {
            "${it.ratioKey} (${String.format("%.0f%%", it.percentage)})"
        } ?: "Belum ada data"
    }

    // 4. Dataset Balance Health Assessment
    val healthIndex = remember(categoryBarItems, totalSampleCount) {
        val nonZeroClasses = categoryBarItems.filter { it.count > 0 }
        if (totalSampleCount == 0 || nonZeroClasses.isEmpty()) {
            DatasetHealthIndex(
                statusLabel = "Dataset Kosong",
                statusColor = TextMuted,
                description = "Ambil foto atau tambahkan sampel untuk menganalisis keseimbangan data.",
                maxClassRatio = 0f,
                minClassRatio = 0f,
                isBalanced = false
            )
        } else {
            val maxCount = nonZeroClasses.maxOf { it.count }
            val minCount = nonZeroClasses.minOf { it.count }
            val ratio = if (minCount > 0) maxCount.toFloat() / minCount.toFloat() else 99f

            when {
                nonZeroClasses.size < 2 -> DatasetHealthIndex(
                    statusLabel = "1 Kelas Aktif",
                    statusColor = NeonAmber,
                    description = "Baru 1 kelas yang memiliki data. Tambahkan sampel pada kelas lain untuk klasifikasi multi-kelas.",
                    maxClassRatio = ratio,
                    minClassRatio = 1f,
                    isBalanced = false
                )
                ratio <= 1.35f -> DatasetHealthIndex(
                    statusLabel = "Keseimbangan Optimal",
                    statusColor = NeonEmerald,
                    description = "Distribusi sampel sangat seimbang (variasi <35%). Bobot loss model ML akan konvergen optimal.",
                    maxClassRatio = ratio,
                    minClassRatio = 1f,
                    isBalanced = true
                )
                ratio <= 2.2f -> DatasetHealthIndex(
                    statusLabel = "Cukup Seimbang",
                    statusColor = NeonCyan,
                    description = "Distribusi cukup baik. Pertimbangkan menambah sampel pada kelas dengan volume lebih rendah.",
                    maxClassRatio = ratio,
                    minClassRatio = 1f,
                    isBalanced = true
                )
                else -> DatasetHealthIndex(
                    statusLabel = "Imbalance Terdeteksi",
                    statusColor = NeonRose,
                    description = "Terdapat ketimpangan signifikan (${String.format("%.1fx", ratio)}). Dianjurkan menambah data atau augmentasi pada kelas minoritas.",
                    maxClassRatio = ratio,
                    minClassRatio = 1f,
                    isBalanced = false
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmPlumCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp // Safe space above bottom navigation bar
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onNavigateToStudio != null) {
                            IconButton(
                                onClick = onNavigateToStudio,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WarmPlumSurface)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .testTag("stats_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Studio",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Statistik Dataset",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ringkasan Visualisasi Recharts & Analisis ML",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Top Action Buttons (Demo Seed & Camera Shortcut)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Seed Demo Data Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33C084FC))
                                .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isProcessing) {
                                    viewModel.seedDemoSamples()
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("stats_seed_demo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = NeonPurple,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Demo Seed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonPurple
                                )
                            }
                        }

                        if (onNavigateToStudio != null) {
                            IconButton(
                                onClick = onNavigateToStudio,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WarmPlumSurface)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .testTag("stats_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Ambil Foto",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Split Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmPlumSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SplitFilter.entries.forEach { split ->
                        val isSelected = selectedSplit == split
                        val count = when (split) {
                            SplitFilter.ALL -> allSamples.size
                            SplitFilter.TRAIN -> allSamples.count { it.split.equals("TRAIN", ignoreCase = true) }
                            SplitFilter.VAL -> allSamples.count { it.split.equals("VAL", ignoreCase = true) }
                            SplitFilter.TEST -> allSamples.count { it.split.equals("TEST", ignoreCase = true) }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { selectedSplit = split }
                                .padding(vertical = 6.dp)
                                .testTag("split_filter_${split.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = split.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else TextSecondary
                                )
                                Text(
                                    text = "($count)",
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // KPI Metric Summary Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Total Images
                        KpiSummaryCard(
                            title = "Total Gambar",
                            value = "$totalSampleCount",
                            subtitle = storageMbStr,
                            icon = Icons.Default.Image,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 2: Active Categories
                        val activeClassesCount = categoryBarItems.count { it.count > 0 }
                        KpiSummaryCard(
                            title = "Kategori Aktif",
                            value = "$activeClassesCount",
                            subtitle = "dari ${allClasses.size} kelas",
                            icon = Icons.Default.Category,
                            accentColor = NeonPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 3: Dominant Aspect Ratio
                        KpiSummaryCard(
                            title = "Rasio Dominan",
                            value = dominantRatio,
                            subtitle = if (aspectRatioItems.isNotEmpty()) "${aspectRatioItems.size} rasio terpakai" else "-",
                            icon = Icons.Default.Layers,
                            accentColor = NeonRose,
                            modifier = Modifier.weight(1f)
                        )

                        // Card 4: Dataset Balance Index
                        KpiSummaryCard(
                            title = "Keseimbangan",
                            value = healthIndex.statusLabel,
                            subtitle = if (healthIndex.isBalanced) "Optimal untuk ML" else "Perlu Tambahan",
                            icon = if (healthIndex.isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                            accentColor = healthIndex.statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Empty State Banner if no samples
            if (totalSampleCount == 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(WarmPlumSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "Belum Ada Citra di Dataset",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Mulai ambil foto dengan kamera studio, atau buat 12 sampel demo ML instan untuk melihat grafik Recharts secara langsung.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonPurple)
                                    .clickable(enabled = !isProcessing) {
                                        viewModel.seedDemoSamples()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("empty_seed_demo_button")
                            ) {
                                Text(
                                    text = "Buat Sampel Demo ML",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (onNavigateToStudio != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33FFFFFF))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToStudio() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("empty_open_studio_button")
                                ) {
                                    Text(
                                        text = "Buka Studio",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1: Recharts Bar Chart - Category Distribution
            item {
                RechartsBarChart(
                    items = categoryBarItems,
                    totalSamples = totalSampleCount,
                    onCategoryClick = { classId ->
                        onNavigateToExplorer?.invoke(classId)
                    }
                )
            }

            // SECTION 2: Recharts Donut Chart - Aspect Ratio Distribution
            item {
                RechartsDonutChart(
                    items = aspectRatioItems,
                    totalSamples = totalSampleCount,
                    onRatioSelected = { ratio ->
                        // User tapped a ratio
                    }
                )
            }

            // SECTION 3: ML Dataset Balance & Architecture Insights
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(WarmPlumSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .padding(18.dp)
                        .testTag("ml_dataset_health_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(healthIndex.statusColor)
                            )
                            Text(
                                text = "Analisis Kesiapan Model ML",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(healthIndex.statusColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = healthIndex.statusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = healthIndex.statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = healthIndex.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Split Distribution Progress Bar
                    val trainCount = allSamples.count { it.split.equals("TRAIN", ignoreCase = true) }
                    val valCount = allSamples.count { it.split.equals("VAL", ignoreCase = true) }
                    val testCount = allSamples.count { it.split.equals("TEST", ignoreCase = true) }
                    val totalAll = (trainCount + valCount + testCount).coerceAtLeast(1)

                    val trainPct = (trainCount.toFloat() / totalAll * 100f).toInt()
                    val valPct = (valCount.toFloat() / totalAll * 100f).toInt()
                    val testPct = (100 - trainPct - valPct).coerceAtLeast(0)

                    Text(
                        text = "Proporsi Split Dataset (Train • Val • Test)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x22FFFFFF))
                    ) {
                        if (trainCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(trainPct.coerceAtLeast(1).toFloat())
                                    .fillMaxSize()
                                    .background(NeonCyan)
                            )
                        }
                        if (valCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(valPct.coerceAtLeast(1).toFloat())
                                    .fillMaxSize()
                                    .background(NeonPurple)
                            )
                        }
                        if (testCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(testPct.coerceAtLeast(1).toFloat())
                                    .fillMaxSize()
                                    .background(NeonAmber)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonCyan))
                            Text(text = "Train: $trainCount ($trainPct%)", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonPurple))
                            Text(text = "Val: $valCount ($valPct%)", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NeonAmber))
                            Text(text = "Test: $testCount ($testPct%)", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(WarmPlumSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1
        )

        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = TextMuted,
            maxLines = 1
        )
    }
}
