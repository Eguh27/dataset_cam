package com.example.data.model

import kotlin.math.roundToInt

enum class AspectRatioPreset(val displayName: String, val ratioWidth: Int, val ratioHeight: Int) {
    SQUARE_1_1("1:1", 1, 1),
    STANDARD_4_3("4:3", 4, 3),
    WIDESCREEN_16_9("16:9", 16, 9);

    val aspectRatio: Float
        get() = ratioWidth.toFloat() / ratioHeight.toFloat()
}

enum class ResolutionQuality(val label: String, val description: String) {
    LITE("Model-Ready (Lite)", "Optimized for direct ML model input & lightweight epoch processing"),
    BALANCED("Balanced (Med)", "Great balance of feature fidelity and storage efficiency"),
    HIGH("High-Res (Max)", "Maximum visual detail and edge precision"),
    CUSTOM("Custom Sized", "User-specified pixel dimensions adhering to aspect ratio")
}

data class ResolutionOption(
    val width: Int,
    val height: Int,
    val aspectPreset: AspectRatioPreset,
    val quality: ResolutionQuality,
    val label: String,
    val mlUseCases: String,
    val estimatedKb: Int,
    val savingsPercent: Int,
    val speedBadge: String
) {
    val displayString: String
        get() = "${width}x${height}"
}

data class ResolutionDimension(
    val width: Int,
    val height: Int,
    val aspectPreset: AspectRatioPreset,
    val quality: ResolutionQuality = ResolutionQuality.LITE,
    val customLabel: String? = null
) {
    val displayString: String
        get() = "${width}x${height}"

    val pixelCount: Long
        get() = width.toLong() * height.toLong()

    /**
     * Estimated compressed JPEG file size in KB at ~85% quality
     */
    val estimatedKb: Int
        get() = kotlin.math.max(12, (pixelCount * 0.42 / 1024).toInt())

    /**
     * Estimated savings compared to raw 12MP uncompressed phone photo (~3,500 KB)
     */
    val storageSavingsPercent: Int
        get() = kotlin.math.max(10, kotlin.math.min(99, (100 - (estimatedKb.toDouble() / 3500.0 * 100)).toInt()))

    val trainingSpeedRating: String
        get() = when {
            width <= 256 -> "⚡⚡⚡ Ultra Fast (12x Epoch)"
            width <= 640 -> "⚡⚡ High Speed (5x Epoch)"
            width <= 1280 -> "⚡ Balanced Speed"
            else -> "🐢 High Compute Required"
        }
}

object ResolutionPresets {

    fun getDimension(aspect: AspectRatioPreset, quality: ResolutionQuality): ResolutionDimension {
        return when (aspect) {
            AspectRatioPreset.SQUARE_1_1 -> when (quality) {
                ResolutionQuality.LITE -> ResolutionDimension(224, 224, aspect, quality)
                ResolutionQuality.BALANCED -> ResolutionDimension(512, 512, aspect, quality)
                ResolutionQuality.HIGH -> ResolutionDimension(1024, 1024, aspect, quality)
                ResolutionQuality.CUSTOM -> ResolutionDimension(256, 256, aspect, quality)
            }
            AspectRatioPreset.STANDARD_4_3 -> when (quality) {
                ResolutionQuality.LITE -> ResolutionDimension(640, 480, aspect, quality)
                ResolutionQuality.BALANCED -> ResolutionDimension(800, 600, aspect, quality)
                ResolutionQuality.HIGH -> ResolutionDimension(1024, 768, aspect, quality)
                ResolutionQuality.CUSTOM -> ResolutionDimension(640, 480, aspect, quality)
            }
            AspectRatioPreset.WIDESCREEN_16_9 -> when (quality) {
                ResolutionQuality.LITE -> ResolutionDimension(640, 360, aspect, quality)
                ResolutionQuality.BALANCED -> ResolutionDimension(1280, 720, aspect, quality)
                ResolutionQuality.HIGH -> ResolutionDimension(1920, 1080, aspect, quality)
                ResolutionQuality.CUSTOM -> ResolutionDimension(1280, 720, aspect, quality)
            }
        }
    }

    /**
     * Predefined curated ML-optimal resolution options for each aspect ratio
     */
    fun getOptionsForAspect(aspect: AspectRatioPreset): List<ResolutionOption> {
        return when (aspect) {
            AspectRatioPreset.SQUARE_1_1 -> listOf(
                ResolutionOption(
                    width = 224,
                    height = 224,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.LITE,
                    label = "224x224 (ViT / MobileNet)",
                    mlUseCases = "Standar emas Vision Transformer (ViT-B/16), MobileNetV3 & ResNet-50. Sangat ringan.",
                    estimatedKb = 18,
                    savingsPercent = 95,
                    speedBadge = "⚡⚡⚡ Ultra Fast"
                ),
                ResolutionOption(
                    width = 256,
                    height = 256,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.CUSTOM,
                    label = "256x256 (PyTorch Benchmark)",
                    mlUseCases = "Standar ImageNet benchmarking, GAN training & convolutional autoencoders.",
                    estimatedKb = 25,
                    savingsPercent = 93,
                    speedBadge = "⚡⚡ Fast"
                ),
                ResolutionOption(
                    width = 512,
                    height = 512,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.BALANCED,
                    label = "512x512 (Face / Diffusion)",
                    mlUseCases = "Optimal untuk dataset biometrik wajah (ArcFace), landmark detection & latent diffusion.",
                    estimatedKb = 68,
                    savingsPercent = 81,
                    speedBadge = "⚡ Balanced"
                ),
                ResolutionOption(
                    width = 1024,
                    height = 1024,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.HIGH,
                    label = "1024x1024 (High-Res Detail)",
                    mlUseCases = "Segmentasi medis, deteksi cacat mikro industri & fine-grained visual inspection.",
                    estimatedKb = 215,
                    savingsPercent = 40,
                    speedBadge = "🐢 High Compute"
                )
            )

            AspectRatioPreset.STANDARD_4_3 -> listOf(
                ResolutionOption(
                    width = 320,
                    height = 240,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.LITE,
                    label = "320x240 (QVGA Ultra-Lite)",
                    mlUseCases = "Mikrokontroler ESP32-CAM, TinyML, Arduino Vision & wearable camera.",
                    estimatedKb = 16,
                    savingsPercent = 95,
                    speedBadge = "⚡⚡⚡ Ultra Fast"
                ),
                ResolutionOption(
                    width = 640,
                    height = 480,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.BALANCED,
                    label = "640x480 (VGA OpenCV Standar)",
                    mlUseCases = "Standar industri robotika, webcam vision stream & OCR klasifikasi formulir.",
                    estimatedKb = 52,
                    savingsPercent = 85,
                    speedBadge = "⚡⚡ Fast"
                ),
                ResolutionOption(
                    width = 800,
                    height = 600,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.CUSTOM,
                    label = "800x600 (SVGA Balanced)",
                    mlUseCases = "Keseimbangan presisi spasial dan kompresi memori untuk inspeksi visual.",
                    estimatedKb = 86,
                    savingsPercent = 75,
                    speedBadge = "⚡ Balanced"
                ),
                ResolutionOption(
                    width = 1024,
                    height = 768,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.HIGH,
                    label = "1024x768 (XGA High-Res)",
                    mlUseCases = "Kamera pengawas industri resolusi tinggi & document recognition.",
                    estimatedKb = 168,
                    savingsPercent = 52,
                    speedBadge = "🐢 High Compute"
                )
            )

            AspectRatioPreset.WIDESCREEN_16_9 -> listOf(
                ResolutionOption(
                    width = 426,
                    height = 240,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.LITE,
                    label = "426x240 (240p Stream Lite)",
                    mlUseCases = "Streaming video berlatensi sangat rendah, model gesture tracking real-time.",
                    estimatedKb = 18,
                    savingsPercent = 95,
                    speedBadge = "⚡⚡⚡ Ultra Fast"
                ),
                ResolutionOption(
                    width = 640,
                    height = 360,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.CUSTOM,
                    label = "640x360 (360p Mobile Real-Time)",
                    mlUseCases = "Tracking objek mobile, CCTV low-bandwidth edge deployment.",
                    estimatedKb = 42,
                    savingsPercent = 88,
                    speedBadge = "⚡⚡ Fast"
                ),
                ResolutionOption(
                    width = 1280,
                    height = 720,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.BALANCED,
                    label = "1280x720 (720p HD Standar YOLO)",
                    mlUseCases = "Standar optimal deteksi objek YOLOv8/v9/v11 & RT-DETR konteks nyata.",
                    estimatedKb = 135,
                    savingsPercent = 61,
                    speedBadge = "⚡ Balanced"
                ),
                ResolutionOption(
                    width = 1920,
                    height = 1080,
                    aspectPreset = aspect,
                    quality = ResolutionQuality.HIGH,
                    label = "1920x1080 (1080p FHD High Precision)",
                    mlUseCases = "Autonomous vehicles, traffic surveillance & drone aerial recognition.",
                    estimatedKb = 320,
                    savingsPercent = 18,
                    speedBadge = "🐢 High Compute"
                )
            )
        }
    }

    /**
     * Calculates custom dimensions locked strictly to aspect ratio.
     * Ensures width and height are even integers (required for ML convolutions/codecs).
     */
    fun createCustomDimension(aspect: AspectRatioPreset, baseWidthOrHeight: Int, isWidthBase: Boolean = true): ResolutionDimension {
        val w: Int
        val h: Int
        if (isWidthBase) {
            val clampedW = baseWidthOrHeight.coerceIn(160, 2048)
            val computedH = (clampedW / aspect.aspectRatio).toInt()
            w = (clampedW / 2) * 2
            h = (computedH / 2) * 2
        } else {
            val clampedH = baseWidthOrHeight.coerceIn(160, 2048)
            val computedW = (clampedH * aspect.aspectRatio).toInt()
            w = (computedW / 2) * 2
            h = (clampedH / 2) * 2
        }
        return ResolutionDimension(
            width = kotlin.math.max(128, w),
            height = kotlin.math.max(128, h),
            aspectPreset = aspect,
            quality = ResolutionQuality.CUSTOM,
            customLabel = "Kustom (${w}x${h})"
        )
    }
}

enum class ImageOutputFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

enum class AugmentationType(val displayName: String, val tag: String) {
    ORIGINAL("Original", "orig"),
    ROTATE_CW_15("Rotate +15°", "rot_p15"),
    ROTATE_CCW_15("Rotate -15°", "rot_m15"),
    ROTATE_90("Rotate 90°", "rot_90"),
    FLIP_HORIZONTAL("Flip Horizontal", "flip_h"),
    FLIP_VERTICAL("Flip Vertical", "flip_v"),
    SHIFT_OFFSET("Translation Shift", "shift"),
    BRIGHTNESS_HIGH("High Lighting (+25%)", "bright_hi"),
    BRIGHTNESS_LOW("Low Lighting (-25%)", "bright_lo"),
    CONTRAST_HIGH("High Contrast (+30%)", "contrast_hi"),
    ROTATION_CUSTOM("Custom Rotation", "rot_deg"),
    BRIGHTNESS_ADJUSTMENT("Brightness Adjustment", "bright_adj"),
    CUSTOM_AUGMENTATION("Custom Augmentation", "custom_aug")
}

data class AugmentationConfig(
    val enableAugmentation: Boolean = true,
    val multiplier: Int = 4, // Number of variations per capture (2, 4, 6, 8)
    val rotateAngles: Boolean = true,
    val horizontalFlip: Boolean = true,
    val verticalFlip: Boolean = false,
    val lightingVariations: Boolean = true,
    val translationShift: Boolean = true
)

data class AugmentationParams(
    val rotationDegrees: Float = 0f,
    val brightnessDelta: Float = 0f, // -100f to +100f (0 = unchanged)
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val contrastFactor: Float = 1.0f // 0.5f to 2.0f (1.0 = unchanged)
) {
    val isModified: Boolean
        get() = rotationDegrees != 0f ||
                brightnessDelta != 0f ||
                flipHorizontal ||
                flipVertical ||
                contrastFactor != 1.0f

    val description: String
        get() = buildList {
            if (rotationDegrees != 0f) add("Rotasi ${rotationDegrees.toInt()}°")
            if (brightnessDelta != 0f) add("Kecerahan ${if (brightnessDelta > 0) "+" else ""}${brightnessDelta.toInt()}%")
            if (flipHorizontal) add("Flip H")
            if (flipVertical) add("Flip V")
            if (contrastFactor != 1.0f) add("Kontras ${String.format(java.util.Locale.US, "%.1fx", contrastFactor)}")
        }.joinToString(" • ").ifEmpty { "Tanpa Augmentasi" }
}

data class SegmentationConfig(
    val autoSegmentBackground: Boolean = false,
    val thresholdSensitivity: Float = 0.28f, // 0.1f - 0.5f
    val featherRadius: Int = 2,
    val replaceWithColor: Int? = null // null means transparent PNG, otherwise color int
)

enum class DatasetSplit(val displayName: String) {
    TRAIN("Train (80%)"),
    VAL("Val (10%)"),
    TEST("Test (10%)")
}

/**
 * Smart Preset Recommendations for ML Architectures & Tasks
 * (ViT, VGG, ResNet, Face Recognition, YOLO, Edge AI)
 */
enum class MlArchitecturePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val recommendedAspect: AspectRatioPreset,
    val recommendedQuality: ResolutionQuality,
    val autoSegmentation: Boolean,
    val autoSegmentationSensitivity: Float,
    val multiplier: Int,
    val rotate: Boolean,
    val flip: Boolean,
    val lighting: Boolean,
    val shift: Boolean,
    val targetResolutionString: String,
    val rationale: String,
    val iconTag: String
) {
    VISION_TRANSFORMER(
        id = "vit",
        title = "Vision Transformer (ViT)",
        subtitle = "ViT-B/16, ViT-L/14, DeiT",
        recommendedAspect = AspectRatioPreset.SQUARE_1_1,
        recommendedQuality = ResolutionQuality.LITE,
        autoSegmentation = false,
        autoSegmentationSensitivity = 0.28f,
        multiplier = 4,
        rotate = true,
        flip = true,
        lighting = true,
        shift = true,
        targetResolutionString = "224x224 (Square 1:1)",
        rationale = "ViT memecah gambar menjadi patch 16x16 px dari ukuran kanonik 224x224. Variasi pencahayaan & rotasi sangat krusial mencegah overfitting pada attention weights.",
        iconTag = "🔮"
    ),
    VGG_CONVNET(
        id = "vgg",
        title = "VGG-16 / VGG-19",
        subtitle = "Classic Deep CNNs",
        recommendedAspect = AspectRatioPreset.SQUARE_1_1,
        recommendedQuality = ResolutionQuality.LITE,
        autoSegmentation = false,
        autoSegmentationSensitivity = 0.28f,
        multiplier = 4,
        rotate = true,
        flip = true,
        lighting = true,
        shift = false,
        targetResolutionString = "224x224 (Square 1:1)",
        rationale = "Arsitektur VGG membutuhkan input tepat 224x224 px 3-channel. Flip horizontal dan pencahayaan menjaga kestabilan stack konvolusi 3x3.",
        iconTag = "🧠"
    ),
    FACE_RECOGNITION(
        id = "face",
        title = "Dataset Wajah (Face / Identity)",
        subtitle = "FaceNet, ArcFace, InsightFace",
        recommendedAspect = AspectRatioPreset.SQUARE_1_1,
        recommendedQuality = ResolutionQuality.BALANCED,
        autoSegmentation = true,
        autoSegmentationSensitivity = 0.32f,
        multiplier = 4,
        rotate = true,
        flip = true,
        lighting = true,
        shift = false,
        targetResolutionString = "512x512 (1:1 + Auto-Cutout)",
        rationale = "Auto-segmentasi aktif otomatis membuang background bising agar loss fungsi embedding (ArcFace/CosFace) murni mempelajari landmark wajah. Pencahayaan bervariasi melatih ketahanan bayangan.",
        iconTag = "👤"
    ),
    YOLO_OBJECT_DETECTION(
        id = "yolo",
        title = "YOLO / Deteksi Objek",
        subtitle = "YOLOv8, YOLOv9, YOLOv11, RT-DETR",
        recommendedAspect = AspectRatioPreset.WIDESCREEN_16_9,
        recommendedQuality = ResolutionQuality.BALANCED,
        autoSegmentation = false,
        autoSegmentationSensitivity = 0.25f,
        multiplier = 4,
        rotate = false,
        flip = true,
        lighting = true,
        shift = true,
        targetResolutionString = "1280x720 (16:9 Real-World Context)",
        rationale = "Deteksi objek membutuhkan background alami untuk konteks spasial bounding box. Rasio 16:9 dan pergeseran posisi melatih model melacak objek bergerak.",
        iconTag = "🎯"
    ),
    RESNET_EFFICIENTNET(
        id = "resnet",
        title = "ResNet / EfficientNet",
        subtitle = "ResNet-50, EfficientNet-B0/B2",
        recommendedAspect = AspectRatioPreset.SQUARE_1_1,
        recommendedQuality = ResolutionQuality.LITE,
        autoSegmentation = false,
        autoSegmentationSensitivity = 0.28f,
        multiplier = 4,
        rotate = true,
        flip = true,
        lighting = true,
        shift = true,
        targetResolutionString = "224x224 (Square 1:1)",
        rationale = "Resolusi default 224x224 dengan scaling optimal untuk residual connections dan feature pyramid networks.",
        iconTag = "⚡"
    ),
    MOBILE_EDGE_AI(
        id = "edge",
        title = "Edge AI / MobileNet",
        subtitle = "MobileNetV3, MediaPipe, Coral TPU",
        recommendedAspect = AspectRatioPreset.SQUARE_1_1,
        recommendedQuality = ResolutionQuality.LITE,
        autoSegmentation = false,
        autoSegmentationSensitivity = 0.28f,
        multiplier = 2,
        rotate = true,
        flip = true,
        lighting = true,
        shift = true,
        targetResolutionString = "224x224 (Ultra Lightweight)",
        rationale = "Kompresi optimal dan ukuran file ringan untuk inferensi latensi rendah pada NPU ponsel pintar dan perangkat mikrokontroler hemat daya.",
        iconTag = "📱"
    )
}

