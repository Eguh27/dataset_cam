package com.example.ui.components

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DatasetClassEntity
import com.example.data.model.AspectRatioPreset
import com.example.data.model.ResolutionQuality
import com.example.export.DatasetExporter
import com.example.ui.screens.export.copyToClipboard
import com.example.ui.theme.BlueActionGradient
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
import com.example.ui.theme.WarmPlumSheet
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.viewmodel.DatasetViewModel

enum class DatasetToolTab(val title: String, val icon: ImageVector) {
    REMOVE("Remove", Icons.Default.ContentCut),
    CAPTION("Caption", Icons.Default.Description),
    STYLER("Styler", Icons.Default.WbSunny),
    CROP("Crop", Icons.Default.Crop)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetManagementSheet(
    viewModel: DatasetViewModel,
    onDismiss: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val allClasses by viewModel.allClasses.collectAsState()
    val allSamples by viewModel.allSamples.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val segmentationConfig by viewModel.segmentationConfig.collectAsState()
    val augmentationConfig by viewModel.augmentationConfig.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val resolutionQuality by viewModel.resolutionQuality.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var activeToolTab by remember { mutableStateOf(DatasetToolTab.REMOVE) }
    var showNewClassDialog by remember { mutableStateOf(false) }
    var showMlPresetModal by remember { mutableStateOf(false) }
    var showAspectResolutionCustomizer by remember { mutableStateOf(false) }
    val selectedMlPreset by viewModel.selectedMlPreset.collectAsState()
    val compressionQuality by viewModel.compressionQuality.collectAsState()
    val customDimension by viewModel.customDimension.collectAsState()

    val blurResult by viewModel.blurAnalysisResult.collectAsState()
    val blurDetectionEnabled by viewModel.blurDetectionEnabled.collectAsState()
    val blurThreshold by viewModel.blurSensitivityThreshold.collectAsState()
    val preventBlurryCapture by viewModel.preventBlurryCapture.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmPlumSheet,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x60FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // ML Model Preset Recommendation Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x3500F0FF))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { showMlPresetModal = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("sheet_ml_preset_banner")
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
                        Text(text = selectedMlPreset?.iconTag ?: "⚡", fontSize = 18.sp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Target Model:",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = selectedMlPreset?.title ?: "Vision Transformer",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Text(
                                text = "${selectedMlPreset?.targetResolutionString} • ${if (selectedMlPreset?.autoSegmentation == true) "Auto-Cutout" else "Full Background"}",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x4000F0FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ganti Preset ▾",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ROW 1: 4 Tool Icons (Remove, Caption, Styler, Crop) matching mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatasetToolTab.entries.forEach { tab ->
                    val isSelected = tab == activeToolTab

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { activeToolTab = tab }
                            .testTag("tool_tab_${tab.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color.White else Color(0x2EFFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White else GlassBorder,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color(0xFF1E1416) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ROW 2: Contextual Tool Controls Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x2B000000))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                when (activeToolTab) {
                    DatasetToolTab.REMOVE -> {
                        // Background Segmentation Controls
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Auto Segmentasi Latar Belakang",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Hapus background otomatis saat memotret objek",
                                        fontSize = 11.sp,
                                        color = NeonEmerald
                                    )
                                }
                                Switch(
                                    checked = segmentationConfig.autoSegmentBackground,
                                    onCheckedChange = { viewModel.toggleSegmentation(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NeonEmerald,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.testTag("sheet_segmentation_switch")
                                )
                            }

                            if (segmentationConfig.autoSegmentBackground) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Sensitivitas Threshold Deteksi",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${(segmentationConfig.thresholdSensitivity * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonEmerald
                                    )
                                }
                                Slider(
                                    value = segmentationConfig.thresholdSensitivity,
                                    onValueChange = { viewModel.updateSegmentationThreshold(it) },
                                    valueRange = 0.10f..0.50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonEmerald,
                                        activeTrackColor = NeonEmerald,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-Time Blur Detection & Quality Gate Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33FFFFFF))
                                .border(
                                    1.dp,
                                    if (blurDetectionEnabled && blurResult.isBlurry) NeonRose.copy(alpha = 0.6f) else GlassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (blurDetectionEnabled && blurResult.isBlurry) NeonRose.copy(alpha = 0.25f)
                                                else NeonCyan.copy(alpha = 0.25f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = null,
                                            tint = if (blurDetectionEnabled && blurResult.isBlurry) NeonRose else NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Deteksi Blur Stream CameraX",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (blurDetectionEnabled) {
                                                "Skor: ${blurResult.score}% • Var: ${blurResult.rawVariance.toInt()} (${blurResult.status.shortLabel})"
                                            } else {
                                                "Laplacian variance stream dinonaktifkan"
                                            },
                                            fontSize = 10.sp,
                                            color = if (blurDetectionEnabled) {
                                                if (blurResult.isBlurry) NeonRose else NeonEmerald
                                            } else TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = blurDetectionEnabled,
                                    onCheckedChange = { viewModel.toggleBlurDetection(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NeonCyan,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier.testTag("sheet_blur_detection_switch")
                                )
                            }

                            if (blurDetectionEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ambang Batas Ketajaman (Threshold)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${blurThreshold.toInt()} var",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                                Slider(
                                    value = blurThreshold.toFloat(),
                                    onValueChange = { viewModel.setBlurSensitivityThreshold(it.toDouble()) },
                                    valueRange = 50f..350f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = NeonCyan,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Kunci Shutter (Quality Gate)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Cegah penambahan citra buram ke dataset ML",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = preventBlurryCapture,
                                        onCheckedChange = { viewModel.togglePreventBlurryCapture(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = NeonRose,
                                            uncheckedTrackColor = Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier.testTag("sheet_prevent_blur_switch")
                                    )
                                }
                            }
                        }
                    }
                    DatasetToolTab.CAPTION -> {
                        // Annotations Metadata (JSON & CSV Quick Action)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Anotasi & Metadata ML",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${allSamples.size} gambar terindeks",
                                        fontSize = 11.sp,
                                        color = NeonCyan
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33FFFFFF))
                                            .clickable {
                                                val json = DatasetExporter.generateJson(allSamples, allClasses)
                                                copyToClipboard(context, "dataset_annotations.json", json)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(text = "Salin JSON", fontSize = 10.sp, color = Color.White)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33FFFFFF))
                                            .clickable {
                                                val csv = DatasetExporter.generateCsv(allSamples)
                                                copyToClipboard(context, "dataset_index.csv", csv)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(text = "Salin CSV", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "File anotasi berisi nama berkas, path folder label, ukuran pixel, rasio, dan partisi split TRAIN/VAL untuk model pelatihan.",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    DatasetToolTab.STYLER -> {
                        // Augmentation Variations
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Perbanyak Dataset Otomatis",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "1 Foto = 1 Asli + ${augmentationConfig.multiplier} Variasi",
                                        fontSize = 11.sp,
                                        color = NeonPurple
                                    )
                                }
                                Switch(
                                    checked = augmentationConfig.enableAugmentation,
                                    onCheckedChange = { viewModel.toggleAugmentation(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = NeonPurple,
                                        uncheckedTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                            if (augmentationConfig.enableAugmentation) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Jumlah Variasi:", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = "${augmentationConfig.multiplier}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPurple
                                    )
                                }
                                Slider(
                                    value = augmentationConfig.multiplier.toFloat(),
                                    onValueChange = { viewModel.setAugmentationMultiplier(it.toInt()) },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple,
                                        inactiveTrackColor = Color(0x33FFFFFF)
                                    )
                                )
                            }
                        }
                    }
                    DatasetToolTab.CROP -> {
                        // Aspect Ratio & Resolution Presets
                        val currentDim = viewModel.currentDimension
                        val options = remember(aspectRatio) {
                            com.example.data.model.ResolutionPresets.getOptionsForAspect(aspectRatio)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rasio & Resolusi ML (Hemat Ruang)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "~${currentDim.estimatedKb * compressionQuality / 88} KB/file (Hemat ${currentDim.storageSavingsPercent}%)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }

                            // 1. Aspect Ratio Pills (1:1, 4:3, 16:9)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AspectRatioPreset.entries.forEach { preset ->
                                    val isSelected = preset == aspectRatio
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0x22FFFFFF))
                                            .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setAspectRatio(preset) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) NeonCyan else TextPrimary
                                        )
                                    }
                                }
                            }

                            // 2. Curated Resolutions for the active aspect ratio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                options.forEach { option ->
                                    val isSelected = currentDim.width == option.width && currentDim.height == option.height
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonPurple.copy(alpha = 0.35f) else Color(0x22000000))
                                            .border(1.dp, if (isSelected) NeonPurple else GlassBorder, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setResolutionOption(option) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = option.displayString,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) NeonPurple else TextPrimary
                                            )
                                            Text(
                                                text = "~${option.estimatedKb} KB",
                                                fontSize = 8.5.sp,
                                                color = if (isSelected) NeonEmerald else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Open Detailed Customizer & Storage Saver Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x3010B981))
                                    .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { showAspectResolutionCustomizer = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Pengatur Resolusi Kustom",
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Kustomisasi Resolusi Bebas & Kualitas Kompresi...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeonEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ROW 3: Category Label Color Palettes & Floating Action Upload/Export Button (matching mockup)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scrollable category tiles
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allClasses.forEach { cls ->
                        val isSelected = selectedClass?.id == cls.id
                        val classColor = try {
                            Color(android.graphics.Color.parseColor(cls.colorHex))
                        } catch (e: Exception) {
                            NeonCyan
                        }

                        // Colored square category tile like in the reference image
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(classColor, classColor.copy(alpha = 0.65f))
                                    )
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x40FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectClass(cls) }
                                .testTag("sheet_class_tile_${cls.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle class initial or icon inside
                            Text(
                                text = cls.name.take(2).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Plus button to add a new category folder
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x28FFFFFF))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { showNewClassDialog = true }
                            .testTag("sheet_add_class_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Label",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Floating Action Blue Arrow Button (Export ZIP) from mockup
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BlueActionGradient)
                        .clickable {
                            viewModel.exportDatasetZip { zipFile ->
                                DatasetExporter.shareFile(
                                    context,
                                    zipFile,
                                    "application/zip",
                                    "ML_Dataset_Bundle.zip"
                                )
                            }
                        }
                        .testTag("sheet_export_zip_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Ekspor Dataset ZIP",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to add a new category class
    if (showNewClassDialog) {
        NewClassModal(
            onDismiss = { showNewClassDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.addNewClass(name, colorHex)
                showNewClassDialog = false
            }
        )
    }

    // Modal dialog to choose ML Architecture & Task Preset
    if (showMlPresetModal) {
        MlPresetModal(
            viewModel = viewModel,
            onDismiss = { showMlPresetModal = false }
        )
    }

    // Modal bottom sheet to customize aspect ratio, resolution, and compression
    if (showAspectResolutionCustomizer) {
        AspectResolutionCustomizerSheet(
            viewModel = viewModel,
            onDismiss = { showAspectResolutionCustomizer = false }
        )
    }
}
