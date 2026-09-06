package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.BlurAnalysisResult
import com.example.processing.BlurStatus
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DatasetViewModel

/**
 * Compact Pill indicator showing real-time sharpness score & status from CameraX analysis stream.
 */
@Composable
fun BlurMeterPill(
    blurResult: BlurAnalysisResult,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val statusColor = when (blurResult.status) {
        BlurStatus.SHARP -> NeonEmerald
        BlurStatus.MODERATE -> NeonAmber
        BlurStatus.BLURRY -> NeonRose
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_blur")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (blurResult.isBlurry) 400 else 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blur_pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x75000000))
            .border(
                1.dp,
                if (blurResult.isBlurry) statusColor.copy(alpha = alphaAnim) else Color(0x35FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("blur_meter_pill"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Glowing Indicator Dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = if (blurResult.isBlurry) alphaAnim else 1.0f))
            )

            // Status Short Label
            Text(
                text = "${blurResult.status.shortLabel} ${blurResult.score}%",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (blurResult.isBlurry) NeonRose else TextPrimary
            )

            // Mini 3-bar quality gauge
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (blurResult.score >= 30) statusColor else Color(0x40FFFFFF))
                )
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (blurResult.score >= 60) statusColor else Color(0x40FFFFFF))
                )
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (blurResult.score >= 85) statusColor else Color(0x40FFFFFF))
                )
            }
        }
    }
}

/**
 * Prominent animated Glass Warning Banner alerting users when the current frame is too blurry for ML dataset collection.
 */
@Composable
fun BlurWarningBanner(
    blurResult: BlurAnalysisResult,
    isEnabled: Boolean,
    onTuneClicked: () -> Unit,
    onDismissWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isEnabled && blurResult.isBlurry,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF0300A14), Color(0xF018050A))
                    )
                )
                .border(1.5.dp, NeonRose, RoundedCornerShape(18.dp))
                .padding(14.dp)
                .testTag("blur_warning_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing Caution Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(NeonRose.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan Blur",
                        tint = NeonRose,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Citra Terlalu Buram!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRose
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonRose.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Skor: ${blurResult.score}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonRose
                            )
                        }
                    }
                    Text(
                        text = "Tahan kamera stabil atau bersihkan lensa. Foto buram dapat menurunkan performa pelatihan model ML.",
                        fontSize = 11.sp,
                        color = Color(0xFFF1F5F9),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Action to open sensitivity tuner
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x35FFFFFF))
                        .clickable(onClick = onTuneClicked),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Atur Sensitivitas Blur",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive settings modal to adjust blur threshold, toggle real-time CameraX analysis, and enable quality gate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurSettingsDialog(
    viewModel: DatasetViewModel,
    blurResult: BlurAnalysisResult,
    isEnabled: Boolean,
    threshold: Double,
    preventCapture: Boolean,
    onDismiss: () -> Unit
) {
    var localThreshold by remember(threshold) { mutableFloatStateOf(threshold.toFloat()) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCanvas)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(20.dp)
            .testTag("blur_settings_dialog")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Analisis Blur Real-Time (CameraX)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable(onClick = onDismiss),
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

            // Real-Time Diagnostic Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Status Aliran Frame:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = blurResult.status.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (blurResult.status) {
                                BlurStatus.SHARP -> NeonEmerald
                                BlurStatus.MODERATE -> NeonAmber
                                BlurStatus.BLURRY -> NeonRose
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Skor Ketajaman Citra:", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${blurResult.score} / 100",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Varians Laplacian (σ²):", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", blurResult.rawVariance),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Toggle 1: Enable Real-Time Blur Detection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deteksi Blur Real-Time",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Menganalisis bidang luminansi CameraX 30 FPS secara instan.",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { viewModel.setBlurDetectionEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = Color(0x5500F0FF)
                    )
                )
            }

            // Slider: Sensitivity Threshold
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ambang Batas Toleransi Blur:",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${localThreshold.toInt()} (Tingkat: ${if (localThreshold < 60) "Toleran" else if (localThreshold < 90) "Standar" else "Ketat"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Slider(
                    value = localThreshold,
                    onValueChange = {
                        localThreshold = it
                        viewModel.setBlurSensitivityThreshold(it.toDouble())
                    },
                    valueRange = 30f..140f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            // Toggle 2: Quality Gate (Prevent blur capture)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quality Gate (Cegah Jepret Buram)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Mencegah penekanan tombol jepret jika kamera sedang buram.",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = preventCapture,
                    onCheckedChange = { viewModel.setPreventBlurryCapture(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonRose,
                        checkedTrackColor = Color(0x55FF0055)
                    )
                )
            }

            // Done Button
            GlassBox(
                shape = RoundedCornerShape(12.dp),
                borderColor = NeonCyan,
                backgroundBrush = Brush.linearGradient(
                    listOf(NeonCyan.copy(alpha = 0.35f), Color(0x15FFFFFF))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .testTag("blur_dialog_done_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Terapkan & Tutup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
