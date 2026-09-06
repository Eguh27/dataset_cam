package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.DatasetSampleEntity
import com.example.data.model.AugmentationParams
import com.example.processing.ImageProcessingUtility
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
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.viewmodel.DatasetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class AugmentationTargetMode {
    SAVE_AS_NEW,
    OVERWRITE_EXISTING,
    BATCH_ENTIRE_CLASS
}

@Composable
fun DatasetAugmentationDialog(
    sample: DatasetSampleEntity,
    viewModel: DatasetViewModel,
    onDismiss: () -> Unit,
    onApplied: (() -> Unit)? = null
) {
    // Current parameters state
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var brightnessDelta by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var contrastFactor by remember { mutableFloatStateOf(1.0f) }

    var targetMode by remember { mutableStateOf(AugmentationTargetMode.SAVE_AS_NEW) }
    var showOriginalPreview by remember { mutableStateOf(false) }

    // Source Bitmap (downsampled for fast UI response)
    var baseSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(false) }

    val isProcessing by viewModel.isProcessing.collectAsState()
    val batchProgress by viewModel.batchAugmentationProgress.collectAsState()

    val currentParams = remember(rotationDegrees, brightnessDelta, flipHorizontal, flipVertical, contrastFactor) {
        AugmentationParams(
            rotationDegrees = rotationDegrees,
            brightnessDelta = brightnessDelta,
            flipHorizontal = flipHorizontal,
            flipVertical = flipVertical,
            contrastFactor = contrastFactor
        )
    }

    // Load initial bitmap
    LaunchedEffect(sample.filePath) {
        withContext(Dispatchers.IO) {
            val loaded = viewModel.loadSampleBitmap(sample, maxDimension = 480)
            withContext(Dispatchers.Main) {
                baseSourceBitmap = loaded
                previewBitmap = loaded
            }
        }
    }

    // Update preview whenever parameters change
    LaunchedEffect(currentParams, baseSourceBitmap) {
        val src = baseSourceBitmap ?: return@LaunchedEffect
        isRenderingPreview = true
        withContext(Dispatchers.Default) {
            val updated = ImageProcessingUtility.generatePreview(src, currentParams, maxDimension = 480)
            withContext(Dispatchers.Main) {
                previewBitmap = updated
                isRenderingPreview = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassBox(
                shape = RoundedCornerShape(24.dp),
                borderColor = NeonCyan.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxSize(0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Bar
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Utilitas Augmentasi Dataset",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Rotasi, Kecerahan & Pembalikan (H/V)",
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isProcessing,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Main Content: Split into Preview (Top) and Controls (Bottom scrollable)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Live Interactive Preview Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF070B14))
                                .border(1.dp, if (currentParams.isModified) NeonCyan else GlassBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeDisplayBitmap = if (showOriginalPreview) baseSourceBitmap else previewBitmap

                            if (activeDisplayBitmap != null) {
                                Image(
                                    bitmap = activeDisplayBitmap.asImageBitmap(),
                                    contentDescription = "Augmented Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(32.dp))
                            }

                            // Top Pill: Current Augmentation Summary
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xCC000000))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (showOriginalPreview) "Mode: Foto Asli (Original)" else currentParams.description,
                                    color = if (showOriginalPreview) NeonAmber else if (currentParams.isModified) NeonCyan else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Compare Button (Hold/Tap to view original)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (showOriginalPreview) NeonAmber else Color(0x99000000))
                                    .border(1.dp, if (showOriginalPreview) NeonAmber else GlassBorder, RoundedCornerShape(12.dp))
                                    .clickable { showOriginalPreview = !showOriginalPreview }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Bandingkan",
                                        tint = if (showOriginalPreview) Color.Black else Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (showOriginalPreview) "Melihat Asli" else "Bandingkan Asli",
                                        color = if (showOriginalPreview) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 2. ROTATION CONTROLS (Rotasi Sudut)
                        ControlCard(
                            title = "Rotasi Sudut (${rotationDegrees.roundToInt()}°)",
                            icon = Icons.Default.RotateRight,
                            iconColor = NeonCyan
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rentang: -180° s/d +180°",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                if (rotationDegrees != 0f) {
                                    Text(
                                        text = "Reset 0°",
                                        fontSize = 11.sp,
                                        color = NeonRose,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { rotationDegrees = 0f }
                                    )
                                }
                            }

                            Slider(
                                value = rotationDegrees,
                                onValueChange = { rotationDegrees = it.roundToInt().toFloat() },
                                valueRange = -180f..180f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("rotation_slider")
                            )

                            // Quick Presets Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-90f, -45f, -15f, 0f, 15f, 45f, 90f, 180f).forEach { deg ->
                                    val isSelected = rotationDegrees == deg
                                    PresetChip(
                                        label = if (deg == 0f) "0°" else "${deg.toInt()}°",
                                        selected = isSelected,
                                        activeColor = NeonCyan,
                                        onClick = { rotationDegrees = deg }
                                    )
                                }
                            }
                        }

                        // 3. BRIGHTNESS ADJUSTMENT CONTROLS (Kecerahan)
                        ControlCard(
                            title = "Pengaturan Kecerahan (${if (brightnessDelta > 0) "+" else ""}${brightnessDelta.roundToInt()}%)",
                            icon = Icons.Default.Brightness6,
                            iconColor = NeonAmber
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Redup (-100%) hingga Terang (+100%)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                if (brightnessDelta != 0f) {
                                    Text(
                                        text = "Reset 0%",
                                        fontSize = 11.sp,
                                        color = NeonRose,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { brightnessDelta = 0f }
                                    )
                                }
                            }

                            Slider(
                                value = brightnessDelta,
                                onValueChange = { brightnessDelta = it.roundToInt().toFloat() },
                                valueRange = -100f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonAmber,
                                    activeTrackColor = NeonAmber,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("brightness_slider")
                            )

                            // Quick Presets Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-50f, -25f, 0f, 25f, 50f).forEach { bVal ->
                                    val isSelected = brightnessDelta == bVal
                                    PresetChip(
                                        label = if (bVal == 0f) "Normal (0%)" else "${bVal.toInt()}%",
                                        selected = isSelected,
                                        activeColor = NeonAmber,
                                        onClick = { brightnessDelta = bVal }
                                    )
                                }
                            }
                        }

                        // 4. FLIPPING CONTROLS (Pembalikan Horizontal & Vertikal)
                        ControlCard(
                            title = "Pembalikan Gambar (Mirror Flipping)",
                            icon = Icons.Default.Flip,
                            iconColor = NeonEmerald
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Flip Horizontal Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (flipHorizontal) NeonCyan.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                        .border(
                                            1.5.dp,
                                            if (flipHorizontal) NeonCyan else Color(0x33FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { flipHorizontal = !flipHorizontal }
                                        .padding(12.dp)
                                        .testTag("flip_horizontal_button")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flip,
                                                contentDescription = "Flip Horizontal",
                                                tint = if (flipHorizontal) NeonCyan else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Flip Horizontal",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (flipHorizontal) NeonCyan else TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "Pencerminan Kiri ↔ Kanan",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Flip Vertical Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (flipVertical) NeonEmerald.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                        .border(
                                            1.5.dp,
                                            if (flipVertical) NeonEmerald else Color(0x33FFFFFF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { flipVertical = !flipVertical }
                                        .padding(12.dp)
                                        .testTag("flip_vertical_button")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RotateRight, // Or Flip icon
                                                contentDescription = "Flip Vertical",
                                                tint = if (flipVertical) NeonEmerald else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Flip Vertikal",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (flipVertical) NeonEmerald else TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "Pencerminan Atas ↕ Bawah",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 5. CONTRAST TUNING (Optional fine-tune)
                        ControlCard(
                            title = "Kontras (${String.format(java.util.Locale.US, "%.1fx", contrastFactor)})",
                            icon = Icons.Default.AutoAwesome,
                            iconColor = NeonPurple
                        ) {
                            Slider(
                                value = contrastFactor,
                                onValueChange = { contrastFactor = (it * 10).roundToInt() / 10f },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPurple,
                                    activeTrackColor = NeonPurple,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("contrast_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(0.7f, 1.0f, 1.3f, 1.6f).forEach { cVal ->
                                    PresetChip(
                                        label = if (cVal == 1.0f) "Netral (1.0x)" else "${cVal}x",
                                        selected = contrastFactor == cVal,
                                        activeColor = NeonPurple,
                                        onClick = { contrastFactor = cVal }
                                    )
                                }
                            }
                        }

                        // 6. TARGET APPLICATION SCOPE
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tindakan Penyimpanan:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            TargetModeRow(
                                title = "Simpan Sebagai Sampel Baru di Dataset",
                                subtitle = "Membuat variasi tambahan tanpa mengubah gambar asli",
                                isSelected = targetMode == AugmentationTargetMode.SAVE_AS_NEW,
                                onClick = { targetMode = AugmentationTargetMode.SAVE_AS_NEW }
                            )

                            TargetModeRow(
                                title = "Perbarui File Sampel Ini (In-Place)",
                                subtitle = "Mengganti berkas asli dengan versi yang telah dimodifikasi",
                                isSelected = targetMode == AugmentationTargetMode.OVERWRITE_EXISTING,
                                onClick = { targetMode = AugmentationTargetMode.OVERWRITE_EXISTING }
                            )

                            TargetModeRow(
                                title = "Terapkan Massal (Batch) ke Kategori '${sample.className}'",
                                subtitle = "Menerapkan konfigurasi ini ke semua foto di folder kelas ini",
                                isSelected = targetMode == AugmentationTargetMode.BATCH_ENTIRE_CLASS,
                                onClick = { targetMode = AugmentationTargetMode.BATCH_ENTIRE_CLASS }
                            )
                        }

                        // Batch Progress Indicator
                        AnimatedVisibility(visible = batchProgress != null) {
                            val prog = batchProgress ?: Pair(0, 1)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.1f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Memproses batch dataset: ${prog.first} dari ${prog.second} gambar...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { prog.first.toFloat() / prog.second.coerceAtLeast(1) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonCyan,
                                    trackColor = Color(0x33FFFFFF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Actions (Bottom)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Button
                        GlassBox(
                            shape = RoundedCornerShape(12.dp),
                            borderColor = GlassBorder,
                            modifier = Modifier
                                .clickable(enabled = !isProcessing) {
                                    rotationDegrees = 0f
                                    brightnessDelta = 0f
                                    flipHorizontal = false
                                    flipVertical = false
                                    contrastFactor = 1.0f
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(text = "Reset", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // Apply Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan)
                                .clickable(enabled = !isProcessing) {
                                    when (targetMode) {
                                        AugmentationTargetMode.SAVE_AS_NEW -> {
                                            viewModel.applyAugmentationToSample(
                                                sample = sample,
                                                params = currentParams,
                                                saveAsNew = true,
                                                onComplete = { success ->
                                                    if (success) {
                                                        onApplied?.invoke()
                                                        onDismiss()
                                                    }
                                                }
                                            )
                                        }
                                        AugmentationTargetMode.OVERWRITE_EXISTING -> {
                                            viewModel.applyAugmentationToSample(
                                                sample = sample,
                                                params = currentParams,
                                                saveAsNew = false,
                                                onComplete = { success ->
                                                    if (success) {
                                                        onApplied?.invoke()
                                                        onDismiss()
                                                    }
                                                }
                                            )
                                        }
                                        AugmentationTargetMode.BATCH_ENTIRE_CLASS -> {
                                            viewModel.batchApplyAugmentation(
                                                classId = sample.classId,
                                                params = currentParams,
                                                onComplete = {
                                                    onApplied?.invoke()
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp)
                                .testTag("apply_augmentation_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (targetMode == AugmentationTargetMode.BATCH_ENTIRE_CLASS) "Terapkan Massal ke Kategori" else "Terapkan ke Dataset",
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
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

@Composable
private fun ControlCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
        content()
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) activeColor.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
            .border(
                1.dp,
                if (selected) activeColor else Color(0x22FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) activeColor else TextSecondary
        )
    }
}

@Composable
private fun TargetModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) NeonCyan.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isSelected) NeonCyan else Color.Transparent)
                .border(1.5.dp, if (isSelected) NeonCyan else TextSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
