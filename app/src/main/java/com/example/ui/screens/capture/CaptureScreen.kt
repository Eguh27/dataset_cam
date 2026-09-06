package com.example.ui.screens.capture

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.db.DatasetClassEntity
import com.example.data.model.AspectRatioPreset
import com.example.data.model.ResolutionPresets
import com.example.data.model.ResolutionQuality
import com.example.processing.BlurDetector
import com.example.processing.BlurStatus
import com.example.ui.components.AspectResolutionCustomizerSheet
import com.example.ui.components.AspectRatioViewfinderMask
import com.example.ui.components.BlurMeterPill
import com.example.ui.components.BlurSettingsDialog
import com.example.ui.components.BlurWarningBanner
import com.example.ui.components.CameraFlashMode
import com.example.ui.components.DatasetManagementSheet
import com.example.ui.components.FlashModeSelectionDialog
import com.example.ui.components.GlassBox
import com.example.ui.components.ProjectSwitchBottomSheet
import com.example.ui.components.LiquidPulseRing
import com.example.ui.components.MlPresetModal
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumCanvas
import com.example.ui.theme.WarmPlumSurface
import com.example.ui.viewmodel.DatasetViewModel
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CaptureScreen(
    viewModel: DatasetViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToExplorer: () -> Unit,
    onNavigateToStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val resolutionQuality by viewModel.resolutionQuality.collectAsState()
    val segmentationConfig by viewModel.segmentationConfig.collectAsState()
    val augmentationConfig by viewModel.augmentationConfig.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val allClasses by viewModel.allClasses.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val hudNotification by viewModel.hudNotification.collectAsState()
    val lastCapturedSample by viewModel.lastCapturedSample.collectAsState()
    val autoCategorization by viewModel.autoCategorization.collectAsState()
    val currentShotCount by viewModel.currentClassShotCount.collectAsState()
    val shotsPerClass by viewModel.shotsPerClassInAutoMode.collectAsState()

    var showDatasetManagementSheet by remember { mutableStateOf(false) }
    var showMlPresetModal by remember { mutableStateOf(false) }
    var showAspectResolutionSheet by remember { mutableStateOf(false) }
    var showProjectSwitchSheet by remember { mutableStateOf(false) }
    val activeProject by viewModel.activeProject.collectAsState()
    var flashMode by remember { mutableStateOf(CameraFlashMode.OFF) }
    var showFlashDialog by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    val selectedMlPreset by viewModel.selectedMlPreset.collectAsState()
    val compressionQuality by viewModel.compressionQuality.collectAsState()
    val currentDim = viewModel.currentDimension

    // Real-Time Blur Detection State
    val blurResult by viewModel.blurAnalysisResult.collectAsState()
    val blurDetectionEnabled by viewModel.blurDetectionEnabled.collectAsState()
    val blurThreshold by viewModel.blurSensitivityThreshold.collectAsState()
    val preventBlurryCapture by viewModel.preventBlurryCapture.collectAsState()
    var showBlurSettingsDialog by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Synchronize hardware camera flash mode and torch (Always-on)
    LaunchedEffect(flashMode, activeCamera) {
        val cam = activeCamera
        when (flashMode) {
            CameraFlashMode.AUTO -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
                try { cam?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
            }
            CameraFlashMode.OFF -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
                try { cam?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
            }
            CameraFlashMode.ON -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
                try { cam?.cameraControl?.enableTorch(false) } catch (_: Exception) {}
            }
            CameraFlashMode.ALWAYS_ON -> {
                imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
                try {
                    if (cam?.cameraInfo?.hasFlashUnit() == true) {
                        cam.cameraControl.enableTorch(true)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    DisposableEffect(activeCamera) {
        onDispose {
            try {
                activeCamera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
        }
    }

    // Set real-time frame analyzer for zero-copy blur & sharpness calculation
    LaunchedEffect(blurDetectionEnabled, blurThreshold) {
        if (blurDetectionEnabled) {
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val result = BlurDetector.analyzeImageProxy(imageProxy, blurThreshold)
                    viewModel.updateBlurResult(result)
                } catch (e: Exception) {
                    // Ignore transient analyzer frame exceptions
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageAnalysis.clearAnalyzer()
        }
    }

    // Photo picker launcher for importing real photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val analysis = BlurDetector.analyzeBitmap(bitmap, blurThreshold)
                        viewModel.updateBlurResult(analysis)
                        if (analysis.isBlurry && preventBlurryCapture) {
                            Toast.makeText(context, "⚠️ Peringatan: Gambar yang diimpor buram (${analysis.score}%)", Toast.LENGTH_LONG).show()
                        }
                        viewModel.processAndSaveCapturedImage(bitmap, 0)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Main Outer Canvas (Warm Plum Obsidian from Mockup)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmPlumCanvas)
    ) {
        // Rounded Viewfinder Container Frame (as depicted in "Viewfinder Frame Container")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(WarmPlumSurface)
                .border(
                    if (blurDetectionEnabled && blurResult.isBlurry) 2.dp else 1.5.dp,
                    if (blurDetectionEnabled && blurResult.isBlurry) NeonRose.copy(alpha = 0.85f) else Color(0x33FFFFFF),
                    RoundedCornerShape(32.dp)
                )
        ) {
            // 1. Camera Viewfinder or Simulator View
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                                activeCamera = camera
                            } catch (exc: Exception) {
                                // Camera bind fallback
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                                activeCamera = camera
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            } else {
                // Permission request / Simulated lens view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF231417)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera Permission",
                            tint = NeonCyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Akses Kamera Dibutuhkan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan kamera untuk menangkap objek dataset dengan konsistensi kompresi dan resolusi ML.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        GlassBox(
                            modifier = Modifier
                                .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                .testTag("request_camera_button"),
                            shape = RoundedCornerShape(12.dp),
                            borderColor = NeonCyan
                        ) {
                            Text(
                                text = "Aktifkan Kamera",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassBox(
                                modifier = Modifier.clickable {
                                    val testBitmap = createSyntheticMLSample(aspectRatio)
                                    viewModel.processAndSaveCapturedImage(testBitmap, 0)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⚡ Objek Simulasi ML",
                                    color = NeonEmerald,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                            GlassBox(
                                modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "📁 Impor Foto",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Aspect Ratio HUD Framing Mask
            AspectRatioViewfinderMask(
                aspectPreset = aspectRatio,
                showGrid = true,
                accentColor = Color(0x70FFFFFF)
            )

            // ==========================================
            // TOP BAR (Responsive Viewfinder Header)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Close / Reset Button (✕)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x50000000))
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable {
                            viewModel.clearNotification()
                        }
                        .testTag("top_close_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center: Interactive Pills (Project Selector + ML Preset + Aspect & Resolution Customizer)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Selector Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x700F172A))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { showProjectSwitchSheet = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("top_project_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Ganti Proyek",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = (activeProject?.name?.take(10) ?: "Proyek") + " ▾",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Left Pill: ML Architecture Preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x60000000))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { showMlPresetModal = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("top_preset_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = selectedMlPreset?.iconTag ?: "⚡",
                                fontSize = 11.sp
                            )
                            Text(
                                text = selectedMlPreset?.title?.split(" ")?.first() ?: "ViT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Right Pill: Aspect Ratio & Resolution
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x60000000))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .clickable { showAspectResolutionSheet = true }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                            .testTag("top_resolution_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Text(
                                text = "${aspectRatio.displayName} • ${currentDim.displayString} ▾",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                // Right: Flash Button, Stats shortcut, and Dataset Menu Button
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Flash Mode Button with dynamic icon and indicator
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (flashMode) {
                                    CameraFlashMode.ALWAYS_ON -> NeonAmber.copy(alpha = 0.25f)
                                    CameraFlashMode.OFF -> Color(0x50000000)
                                    else -> NeonCyan.copy(alpha = 0.2f)
                                }
                            )
                            .border(
                                1.dp,
                                when (flashMode) {
                                    CameraFlashMode.ALWAYS_ON -> NeonAmber
                                    CameraFlashMode.OFF -> GlassBorder
                                    else -> NeonCyan
                                },
                                CircleShape
                            )
                            .clickable { showFlashDialog = true }
                            .testTag("top_flash_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = flashMode.icon,
                            contentDescription = "Flash: ${flashMode.label}",
                            tint = when (flashMode) {
                                CameraFlashMode.ALWAYS_ON -> NeonAmber
                                CameraFlashMode.OFF -> Color.White.copy(alpha = 0.8f)
                                else -> NeonCyan
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x50000000))
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { showDatasetManagementSheet = true }
                            .testTag("top_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Menu kelola dataset",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ========================================================
            // RIGHT-SIDE VERTICAL FLOATING STRIP ("Menu preset" Mockup)
            // ========================================================
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x551E1418))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(24.dp))
                    .padding(vertical = 10.dp, horizontal = 6.dp)
                    .testTag("vertical_preset_menu")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top glowing pink indicator dot from mockup
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (blurDetectionEnabled && blurResult.isBlurry) NeonRose else NeonCyan)
                    )

                    // 0. Real-time Blur Detection & Sharpness HUD (CenterFocusStrong)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (blurDetectionEnabled) {
                                    if (blurResult.isBlurry) NeonRose.copy(alpha = 0.45f)
                                    else NeonEmerald.copy(alpha = 0.35f)
                                } else Color.Transparent
                            )
                            .clickable {
                                showBlurSettingsDialog = true
                            }
                            .testTag("side_blur_detection_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Deteksi Blur Real-Time",
                            tint = if (blurDetectionEnabled) {
                                if (blurResult.isBlurry) NeonRose else NeonEmerald
                            } else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 1. Auto Segmentation (Magic Wand / Scissors)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (segmentationConfig.autoSegmentBackground) NeonEmerald.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .clickable {
                                viewModel.toggleSegmentation(!segmentationConfig.autoSegmentBackground)
                            }
                            .testTag("side_auto_segmentation_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Auto Segmentation",
                            tint = if (segmentationConfig.autoSegmentBackground) NeonEmerald else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Data Augmentation Multiplier (Sparkles / Variations)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (augmentationConfig.enableAugmentation) NeonPurple.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .clickable {
                                val nextMult = when (augmentationConfig.multiplier) {
                                    1 -> 2
                                    2 -> 4
                                    4 -> 8
                                    else -> 1
                                }
                                viewModel.setAugmentationMultiplier(nextMult)
                                if (!augmentationConfig.enableAugmentation) {
                                    viewModel.toggleAugmentation(true)
                                }
                            }
                            .testTag("side_augmentation_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Augmentation",
                            tint = if (augmentationConfig.enableAugmentation) NeonPurple else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Aspect Ratio Preset Switcher (1:1, 4:3, 16:9)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextAspect = when (aspectRatio) {
                                    AspectRatioPreset.SQUARE_1_1 -> AspectRatioPreset.STANDARD_4_3
                                    AspectRatioPreset.STANDARD_4_3 -> AspectRatioPreset.WIDESCREEN_16_9
                                    AspectRatioPreset.WIDESCREEN_16_9 -> AspectRatioPreset.SQUARE_1_1
                                }
                                viewModel.setAspectRatio(nextAspect)
                            }
                            .testTag("side_aspect_ratio_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Resolution Preset (Opens Customizer Sheet)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { showAspectResolutionSheet = true }
                            .testTag("side_resolution_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                currentDim.width <= 320 -> "240"
                                currentDim.width <= 640 -> "640"
                                currentDim.width <= 1280 -> "720"
                                else -> "1k"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    // 5. Auto Categorization Sequencer (Auto advance class)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (autoCategorization) NeonAmber.copy(alpha = 0.35f) else Color.Transparent)
                            .clickable {
                                viewModel.toggleAutoCategorization(!autoCategorization)
                            }
                            .testTag("side_auto_categorization_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (autoCategorization) "$currentShotCount" else "A/C",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoCategorization) NeonAmber else Color.White
                        )
                    }

                    // 6. Camera Flip / Photo Import
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (hasCameraPermission) {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                } else {
                                    photoPickerLauncher.launch("image/*")
                                }
                            }
                            .testTag("side_camera_flip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-Time Blur Warning Banner (alerts user when frame is too blurry for reliable ML training data)
            BlurWarningBanner(
                blurResult = blurResult,
                isEnabled = blurDetectionEnabled,
                onTuneClicked = { showBlurSettingsDialog = true },
                onDismissWarning = { },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
            )

            // HUD Banner Notification
            AnimatedVisibility(
                visible = hudNotification != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (blurDetectionEnabled && blurResult.isBlurry) 150.dp else 64.dp, start = 20.dp, end = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xDD1C1014))
                        .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = hudNotification ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ========================================================
            // BOTTOM SECTION: ZOOM/ASPECT PILL, LABEL CAROUSEL, SHUTTER
            // ========================================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Real-Time Blur & Sharpness Quality Pill
                BlurMeterPill(
                    blurResult = blurResult,
                    isEnabled = blurDetectionEnabled,
                    onClick = { showBlurSettingsDialog = true },
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 1. Aspect Ratio / Zoom Pills matching "0.5x 1x 2x 5x" + Resolution Tune
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x60000000))
                        .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("aspect_zoom_pills")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            val isSelected = preset == aspectRatio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0x45FFFFFF) else Color.Transparent)
                                    .clickable { viewModel.setAspectRatio(preset) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xBBFFFFFF)
                                )
                            }
                        }

                        // Divider dot
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0x55FFFFFF))
                        )

                        // Quick trigger for custom resolution modal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x3500F0FF))
                                .clickable { showAspectResolutionSheet = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("open_resolution_customizer_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${currentDim.displayString} (~${currentDim.estimatedKb * compressionQuality / 88}KB)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Atur",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Horizontal Category Label Carousel with glowing dot indicator
                // Matching: "label1   label2 (with dot)   label3   label4"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allClasses.forEach { cls ->
                        val isSelected = selectedClass?.id == cls.id

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.selectClass(cls) }
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .testTag("label_carousel_${cls.name}")
                        ) {
                            Text(
                                text = cls.name,
                                fontSize = if (isSelected) 15.sp else 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF93C5FD) else Color(0x88FFFFFF)
                            )

                            // Glowing dot indicator underneath active label (from mockup!)
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF93C5FD))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Shutter Row: Gallery Thumb, Liquid Shutter Button, and "Menu kelola dataset" Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Gallery Thumbnail of last captured image
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x35FFFFFF))
                            .border(1.5.dp, GlassBorder, CircleShape)
                            .clickable { onNavigateToExplorer() }
                            .testTag("gallery_thumb_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (lastCapturedSample != null) {
                            AsyncImage(
                                model = File(lastCapturedSample!!.filePath),
                                contentDescription = "Gallery preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Center: Shutter Button with Liquid Glow Outer Ring
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Liquid pulsing gradient ring (turns Red/NeonRose when blurry)
                        LiquidPulseRing(
                            modifier = Modifier.fillMaxSize(),
                            pulseColor = if (blurDetectionEnabled && blurResult.isBlurry) NeonRose else Color(0xFFC084FC)
                        )

                        // Shutter trigger button
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    3.dp,
                                    if (blurDetectionEnabled && blurResult.isBlurry) NeonRose.copy(alpha = 0.8f) else Color(0x80C084FC),
                                    CircleShape
                                )
                                .clickable(enabled = !isProcessing) {
                                    if (blurDetectionEnabled && blurResult.isBlurry && preventBlurryCapture) {
                                        Toast.makeText(
                                            context,
                                            "⚠️ Pengambilan dicegah: Citra terlalu buram (${blurResult.score}%). Stabilkan kamera.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@clickable
                                    }

                                    if (hasCameraPermission) {
                                        imageCapture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val rotationDegrees = image.imageInfo.rotationDegrees
                                                    val bitmap = imageProxyToBitmap(image)
                                                    image.close()
                                                    if (bitmap != null) {
                                                        val capturedAnalysis = BlurDetector.analyzeBitmap(bitmap, blurThreshold)
                                                        viewModel.updateBlurResult(capturedAnalysis)
                                                        viewModel.processAndSaveCapturedImage(
                                                            bitmap,
                                                            rotationDegrees
                                                        )
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    val testBm = createSyntheticMLSample(aspectRatio)
                                                    viewModel.processAndSaveCapturedImage(testBm, 0)
                                                }
                                            }
                                        )
                                    } else {
                                        val testBm = createSyntheticMLSample(aspectRatio)
                                        val testAnalysis = BlurDetector.analyzeBitmap(testBm, blurThreshold)
                                        viewModel.updateBlurResult(testAnalysis)
                                        viewModel.processAndSaveCapturedImage(testBm, 0)
                                    }
                                }
                                .testTag("camera_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF231417),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    // Right: "Menu kelola dataset" (As marked in mockup with arrow!)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x35FFFFFF))
                            .border(1.5.dp, GlassBorder, CircleShape)
                            .clickable { showDatasetManagementSheet = true }
                            .testTag("menu_kelola_dataset_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Menu kelola dataset",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom Sheet: "Menu kelola dataset" (recreates left screen of mockup)
        if (showDatasetManagementSheet) {
            DatasetManagementSheet(
                viewModel = viewModel,
                onDismiss = { showDatasetManagementSheet = false },
                onOpenGallery = {
                    showDatasetManagementSheet = false
                    onNavigateToExplorer()
                }
            )
        }

        // Bottom Sheet: Smart ML Architecture Preset Recommendations
        if (showMlPresetModal) {
            MlPresetModal(
                viewModel = viewModel,
                onDismiss = { showMlPresetModal = false }
            )
        }

        // Bottom Sheet: Aspect Ratio & Customizable Resolution Preset Sheet
        if (showAspectResolutionSheet) {
            AspectResolutionCustomizerSheet(
                viewModel = viewModel,
                onDismiss = { showAspectResolutionSheet = false }
            )
        }

        // Dialog: Real-Time Blur Analysis Settings & Diagnostic Panel
        if (showBlurSettingsDialog) {
            BlurSettingsDialog(
                viewModel = viewModel,
                blurResult = blurResult,
                isEnabled = blurDetectionEnabled,
                threshold = blurThreshold,
                preventCapture = preventBlurryCapture,
                onDismiss = { showBlurSettingsDialog = false }
            )
        }

        // Dialog: Camera Flash Mode Selection (Auto, Off, On, Always On)
        if (showFlashDialog) {
            FlashModeSelectionDialog(
                currentMode = flashMode,
                hasFlashUnit = activeCamera?.cameraInfo?.hasFlashUnit() ?: true,
                onModeSelected = { mode ->
                    flashMode = mode
                    showFlashDialog = false
                    if ((mode == CameraFlashMode.ON || mode == CameraFlashMode.ALWAYS_ON) &&
                        activeCamera?.cameraInfo?.hasFlashUnit() == false
                    ) {
                        Toast.makeText(
                            context,
                            "Perangkat ini tidak memiliki unit lampu kilat hardware (LED Flash)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDismiss = { showFlashDialog = false }
            )
        }

        // Project Switcher Bottom Sheet
        if (showProjectSwitchSheet) {
            ProjectSwitchBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { showProjectSwitchSheet = false },
                onGoToProjectHub = {
                    showProjectSwitchSheet = false
                    viewModel.switchProject()
                }
            )
        }
    }
}

/**
 * Creates a synthetic ML subject with high-contrast geometric contours for testing.
 */
fun createSyntheticMLSample(aspectPreset: AspectRatioPreset): Bitmap {
    val width = 640
    val height = when (aspectPreset) {
        AspectRatioPreset.SQUARE_1_1 -> 640
        AspectRatioPreset.STANDARD_4_3 -> 480
        AspectRatioPreset.WIDESCREEN_16_9 -> 360
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Neutral dark background
    canvas.drawColor(android.graphics.Color.rgb(38, 22, 26))

    val paint = Paint().apply { isAntiAlias = true }

    // Outer glow target ring
    paint.color = android.graphics.Color.rgb(192, 132, 252)
    val cx = width / 2f
    val cy = height / 2f
    val radius = (minOf(width, height) * 0.32f)
    canvas.drawCircle(cx, cy, radius, paint)

    // Inner object core
    paint.color = android.graphics.Color.rgb(244, 114, 182)
    canvas.drawCircle(cx, cy, radius * 0.55f, paint)

    // Centroid highlight
    paint.color = android.graphics.Color.rgb(255, 255, 255)
    canvas.drawCircle(cx, cy, radius * 0.2f, paint)

    return bitmap
}

/**
 * Converts CameraX ImageProxy to Bitmap.
 */
fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
