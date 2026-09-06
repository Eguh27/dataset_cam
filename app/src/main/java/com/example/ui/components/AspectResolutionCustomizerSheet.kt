package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioPreset
import com.example.data.model.ResolutionOption
import com.example.data.model.ResolutionPresets
import com.example.data.model.ResolutionQuality
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumSheet
import com.example.ui.viewmodel.DatasetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectResolutionCustomizerSheet(
    viewModel: DatasetViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentAspect by viewModel.aspectRatio.collectAsState()
    val currentQuality by viewModel.resolutionQuality.collectAsState()
    val customDimension by viewModel.customDimension.collectAsState()
    val compressionQuality by viewModel.compressionQuality.collectAsState()

    val currentDim = viewModel.currentDimension

    var activeAspectTab by remember { mutableStateOf(currentAspect) }
    var isCustomSliderActive by remember { mutableStateOf(customDimension != null && currentQuality == ResolutionQuality.CUSTOM) }
    var customBaseSliderValue by remember { mutableFloatStateOf(currentDim.width.toFloat().coerceIn(160f, 1920f)) }

    val optionsForActiveAspect = remember(activeAspectTab) {
        ResolutionPresets.getOptionsForAspect(activeAspectTab)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmPlumSheet,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x60FFFFFF))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Title and Close
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Rasio & Resolusi",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Preset Rasio & Resolusi ML",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Optimasi dimensi dan ukuran file dataset untuk training",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x28FFFFFF))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 1. ASPECT RATIO SELECTOR TABS (Liquid Animated Pill)
            item {
                Column {
                    Text(
                        text = "1. PILIH PRESET RASIO ASPEK GAMBAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x35000000))
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            val isSelected = preset == activeAspectTab
                            val bgAlpha = if (isSelected) 0.35f else 0.0f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    listOf(NeonCyan.copy(alpha = 0.4f), NeonPurple.copy(alpha = 0.3f))
                                                )
                                            )
                                        } else {
                                            Modifier.background(Color.Transparent)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) NeonCyan else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        activeAspectTab = preset
                                        viewModel.setAspectRatio(preset)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Visual aspect preview mini box
                                    Box(
                                        modifier = Modifier
                                            .size(
                                                width = when (preset) {
                                                    AspectRatioPreset.SQUARE_1_1 -> 12.dp
                                                    AspectRatioPreset.STANDARD_4_3 -> 14.dp
                                                    AspectRatioPreset.WIDESCREEN_16_9 -> 16.dp
                                                },
                                                height = when (preset) {
                                                    AspectRatioPreset.SQUARE_1_1 -> 12.dp
                                                    AspectRatioPreset.STANDARD_4_3 -> 10.5.dp
                                                    AspectRatioPreset.WIDESCREEN_16_9 -> 9.dp
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) NeonCyan else Color(0x88FFFFFF),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = preset.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = when (preset) {
                                                AspectRatioPreset.SQUARE_1_1 -> "Square"
                                                AspectRatioPreset.STANDARD_4_3 -> "Standard"
                                                AspectRatioPreset.WIDESCREEN_16_9 -> "Widescreen"
                                            },
                                            fontSize = 9.sp,
                                            color = if (isSelected) NeonCyan else Color(0x77FFFFFF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. CURATED RESOLUTION OPTIONS FOR CHOSEN ASPECT RATIO
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. RESOLUSI OPTIMAL UNTUK RASIO ${activeAspectTab.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = NeonPurple
                        )

                        Text(
                            text = if (isCustomSliderActive) "Mode Kustom Aktif" else "Pilih Preset",
                            fontSize = 10.sp,
                            color = if (isCustomSliderActive) NeonAmber else TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        optionsForActiveAspect.forEach { option ->
                            val isOptionSelected = !isCustomSliderActive &&
                                    currentAspect == option.aspectPreset &&
                                    currentDim.width == option.width &&
                                    currentDim.height == option.height

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isOptionSelected) Color(0x3500F0FF) else Color(0x22000000)
                                    )
                                    .border(
                                        width = if (isOptionSelected) 1.5.dp else 1.dp,
                                        color = if (isOptionSelected) NeonCyan else GlassBorder,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        isCustomSliderActive = false
                                        viewModel.setResolutionOption(option)
                                    }
                                    .padding(12.dp)
                                    .testTag("resolution_option_${option.width}x${option.height}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = option.displayString,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOptionSelected) NeonCyan else TextPrimary
                                            )

                                            // Speed badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x30A855F7))
                                                    .border(0.8.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = option.speedBadge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeonPurple
                                                )
                                            }

                                            // File Size badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0x3010B981))
                                                    .border(0.8.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "~${option.estimatedKb} KB",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonEmerald
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = option.mlUseCases,
                                            fontSize = 10.5.sp,
                                            color = TextSecondary,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    if (isOptionSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(NeonCyan),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Dipilih",
                                                tint = Color.Black,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SLIDER PENYESUAIAN RESOLUSI KUSTOM (CUSTOM SLIDER)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCustomSliderActive) Color(0x35FFB800) else Color(0x22000000)
                        )
                        .border(
                            width = if (isCustomSliderActive) 1.5.dp else 1.dp,
                            color = if (isCustomSliderActive) NeonAmber else GlassBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Custom Slider",
                                    tint = if (isCustomSliderActive) NeonAmber else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Atur Resolusi Kustom Sendiri",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustomSliderActive) NeonAmber else TextPrimary
                                )
                            }

                            // Current custom resolution value
                            val customComputedDim = remember(activeAspectTab, customBaseSliderValue) {
                                ResolutionPresets.createCustomDimension(activeAspectTab, customBaseSliderValue.toInt(), true)
                            }
                            Text(
                                text = "${customComputedDim.displayString} px",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustomSliderActive) NeonAmber else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = customBaseSliderValue,
                            onValueChange = { newValue ->
                                isCustomSliderActive = true
                                customBaseSliderValue = newValue
                                viewModel.setCustomResolutionSize(newValue.toInt(), true)
                            },
                            valueRange = 160f..1920f,
                            steps = 44,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonAmber,
                                activeTrackColor = NeonAmber,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "160 px (Ultra-Lite)", fontSize = 9.sp, color = TextSecondary)
                            Text(text = "1024 px (HD)", fontSize = 9.sp, color = TextSecondary)
                            Text(text = "1920 px (FHD)", fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // 4. STORAGE SAVINGS & COMPRESSION QUALITY CONTROL
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x3510B981))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Efisiensi Penyimpanan",
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Dampak Ukuran File & Efisiensi Training",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Metric Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Raw camera comparison
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x30000000))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Kamera Raw", fontSize = 9.sp, color = TextSecondary)
                                    Text(text = "~3,500 KB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    Text(text = "Tidak terstandar", fontSize = 8.5.sp, color = Color(0x99FFFFFF))
                                }
                            }

                            // Dataset Studio output
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x30000000))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Output Dataset", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "~${currentDim.estimatedKb * compressionQuality / 88} KB",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonEmerald
                                    )
                                    Text(text = "Presisi Kanonik", fontSize = 8.5.sp, color = NeonEmerald)
                                }
                            }

                            // Storage savings %
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x30000000))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(text = "Hemat Storage", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        text = "-${currentDim.storageSavingsPercent}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(text = "1k foto ≈ ${(currentDim.estimatedKb * 1000) / 1024}MB", fontSize = 8.5.sp, color = NeonCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Compression Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Kualitas Kompresi (JPEG):", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "$compressionQuality%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }

                        Slider(
                            value = compressionQuality.toFloat(),
                            onValueChange = { viewModel.setCompressionQuality(it.toInt()) },
                            valueRange = 50f..100f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonEmerald,
                                activeTrackColor = NeonEmerald,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )

                        Text(
                            text = "💡 Ukuran file yang konsisten dan ringan (~${currentDim.estimatedKb * compressionQuality / 88} KB) melipatgandakan kecepatan batch DataLoader PyTorch/TensorFlow dan menghemat konsumsi memori VRAM GPU.",
                            fontSize = 10.sp,
                            color = Color(0xDDFFFFFF),
                            lineHeight = 13.5.sp
                        )
                    }
                }
            }

            // 5. APPLY BUTTON
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_resolution_preset_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terapkan",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Terapkan Preset ${activeAspectTab.displayName} (${currentDim.displayString} • ~${currentDim.estimatedKb * compressionQuality / 88} KB)",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
