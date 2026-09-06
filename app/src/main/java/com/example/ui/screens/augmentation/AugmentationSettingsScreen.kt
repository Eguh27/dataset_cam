package com.example.ui.screens.augmentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DatasetSampleEntity
import com.example.data.model.AspectRatioPreset
import com.example.data.model.ResolutionQuality
import com.example.ui.components.DatasetAugmentationDialog
import com.example.ui.components.GlassBox
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DatasetViewModel

@Composable
fun AugmentationSettingsScreen(
    viewModel: DatasetViewModel,
    modifier: Modifier = Modifier
) {
    val augmentationConfig by viewModel.augmentationConfig.collectAsState()
    val segmentationConfig by viewModel.segmentationConfig.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val resolutionQuality by viewModel.resolutionQuality.collectAsState()
    val allSamples by viewModel.allSamples.collectAsState()

    var selectedSampleForAugmentation by remember { mutableStateOf<DatasetSampleEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 90.dp)
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Pengaturan Preprocessing & Variasi",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }
        Text(
            text = "Kustomisasi augmentasi data otomatis, segmentasi latar belakang, dan optimasi resolusi epoch.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // SECTION 1: DATASET MULTIPLIER (AUGMENTASI OTOMATIS)
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = if (augmentationConfig.enableAugmentation) NeonCyan.copy(alpha = 0.6f) else GlassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Perbanyak Dataset Otomatis",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "1 Potretan = ${augmentationConfig.multiplier + 1} Sampel (1 Ori + ${augmentationConfig.multiplier} Variasi)",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    Switch(
                        checked = augmentationConfig.enableAugmentation,
                        onCheckedChange = { viewModel.toggleAugmentation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonCyan,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("toggle_augmentation_switch")
                    )
                }

                if (augmentationConfig.enableAugmentation) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Jumlah Variasi Per Potretan: ${augmentationConfig.multiplier}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Slider(
                        value = augmentationConfig.multiplier.toFloat(),
                        onValueChange = { viewModel.setAugmentationMultiplier(it.toInt()) },
                        valueRange = 1f..8f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("augmentation_multiplier_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Augmentation sub-toggles
                    Text(
                        text = "Komponen Transformasi Aktif:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AugmentationOptionRow(
                            icon = Icons.Default.RotateRight,
                            title = "Rotasi Sudut (±15°, 90°)",
                            description = "Membuat model invariansi orientasi rotasi",
                            checked = augmentationConfig.rotateAngles,
                            onCheckedChange = { viewModel.toggleAugmentationOption(rotate = it) }
                        )

                        AugmentationOptionRow(
                            icon = Icons.Default.Flip,
                            title = "Pembalikan Horizontal (Flip H)",
                            description = "Variasi pencerminan objek kiri-kanan",
                            checked = augmentationConfig.horizontalFlip,
                            onCheckedChange = { viewModel.toggleAugmentationOption(flip = it) }
                        )

                        AugmentationOptionRow(
                            icon = Icons.Default.Flip,
                            title = "Pembalikan Vertikal (Flip V)",
                            description = "Variasi pencerminan objek atas-bawah (invariansi simetri)",
                            checked = augmentationConfig.verticalFlip,
                            onCheckedChange = { viewModel.toggleAugmentationOption(verticalFlip = it) }
                        )

                        AugmentationOptionRow(
                            icon = Icons.Default.Brightness6,
                            title = "Variasi Pencahayaan & Kontras",
                            description = "Simulasi intensitas cahaya +25%, -25%, & gamma",
                            checked = augmentationConfig.lightingVariations,
                            onCheckedChange = { viewModel.toggleAugmentationOption(lighting = it) }
                        )

                        AugmentationOptionRow(
                            icon = Icons.Default.OpenWith,
                            title = "Pergeseran Posisi (Shift)",
                            description = "Translasi koordinat X/Y agar model tidak terikat pusat",
                            checked = augmentationConfig.translationShift,
                            onCheckedChange = { viewModel.toggleAugmentationOption(shift = it) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 1B: INTERACTIVE IMAGE AUGMENTATION UTILITY STUDIO
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = NeonPurple.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Utilitas Augmentasi Interaktif",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Uji langsung rotasi, kecerahan, & flip H/V pada dataset",
                                fontSize = 11.sp,
                                color = NeonPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Aplikasi utilitas pemrosesan citra interaktif dengan pratinjau langsung. Anda dapat menerapkan transformasi individual ke satu sampel atau mengeksekusi batch augmentation ke seluruh folder kategori dataset.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Launch Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurple.copy(alpha = 0.25f))
                        .border(1.dp, NeonPurple, RoundedCornerShape(12.dp))
                        .clickable {
                            val candidate = allSamples.firstOrNull()
                            if (candidate != null) {
                                selectedSampleForAugmentation = candidate
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .testTag("launch_interactive_augmentation_utility"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (allSamples.isNotEmpty()) {
                                "Buka Studio Augmentasi (${allSamples.size} Sampel Tersedia)"
                            } else {
                                "Belum Ada Sampel (Ambil Foto Terlebih Dahulu)"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 2: AUTO BACKGROUND SEGMENTATION
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = if (segmentationConfig.autoSegmentBackground) NeonEmerald.copy(alpha = 0.6f) else GlassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Auto Segmentasi Latar Belakang",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Hapus background otomatis saat pengambilan foto",
                                fontSize = 11.sp,
                                color = NeonEmerald
                            )
                        }
                    }

                    Switch(
                        checked = segmentationConfig.autoSegmentBackground,
                        onCheckedChange = { viewModel.toggleSegmentation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonEmerald,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.testTag("toggle_segmentation_switch")
                    )
                }

                if (segmentationConfig.autoSegmentBackground) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val sensPercent = (segmentationConfig.thresholdSensitivity * 100).toInt()
                    Text(
                        text = "Sensitivitas Threshold Deteksi: $sensPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

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

                    Text(
                        text = "Hasil segmentasi disimpan sebagai PNG dengan alpha transparan murni atau masking isolasi objek.",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 3: STORAGE & EPOCH OPTIMIZATION (PRESET RESOLUSI)
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = NeonPurple.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Optimasi Storage & Pelatihan Epoch",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Standarisasi resolusi konsisten untuk CNN, MobileNet, & YOLO",
                            fontSize = 11.sp,
                            color = NeonPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ResolutionQuality.entries.forEach { quality ->
                    val isSelected = quality == resolutionQuality
                    GlassBox(
                        shape = RoundedCornerShape(12.dp),
                        borderColor = if (isSelected) NeonPurple else GlassBorder,
                        backgroundBrush = if (isSelected) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(NeonPurple.copy(alpha = 0.25f), Color(0x14FFFFFF))
                            )
                        } else Color(0x0CFFFFFF).let { androidx.compose.ui.graphics.SolidColor(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.setResolutionQuality(quality) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonPurple)
                                    )
                                }
                            }
                            Text(
                                text = quality.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interactive Augmentation Utility Dialog
        selectedSampleForAugmentation?.let { sample ->
            DatasetAugmentationDialog(
                sample = sample,
                viewModel = viewModel,
                onDismiss = { selectedSampleForAugmentation = null },
                onApplied = { selectedSampleForAugmentation = null }
            )
        }
    }
}

@Composable
fun AugmentationOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x10FFFFFF))
            .clickable { onCheckedChange(!checked) }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) NeonCyan else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (checked) TextPrimary else TextSecondary
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonCyan,
                uncheckedTrackColor = Color(0x22FFFFFF)
            ),
            modifier = Modifier.size(36.dp)
        )
    }
}
