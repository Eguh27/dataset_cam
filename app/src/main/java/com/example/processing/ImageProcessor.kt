package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.example.data.model.AspectRatioPreset
import com.example.data.model.AugmentationConfig
import com.example.data.model.AugmentationType
import com.example.data.model.ResolutionDimension
import com.example.data.model.SegmentationConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AugmentedVariation(
    val bitmap: Bitmap,
    val type: AugmentationType,
    val filenameSuffix: String
)

object ImageProcessor {

    /**
     * Standardizes an incoming photo bitmap:
     * 1. Center crops to the exact aspect ratio (1:1, 4:3, 16:9).
     * 2. Scales down/up to the exact target dimensions (e.g. 224x224, 512x512, 640x480).
     */
    fun standardizeBitmap(
        source: Bitmap,
        targetDimension: ResolutionDimension,
        rotationDegrees: Int = 0
    ): Bitmap {
        var orientedBitmap = source
        if (rotationDegrees != 0) {
            val rotMatrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            orientedBitmap = Bitmap.createBitmap(
                source, 0, 0, source.width, source.height, rotMatrix, true
            )
        }

        val srcWidth = orientedBitmap.width.toFloat()
        val srcHeight = orientedBitmap.height.toFloat()
        val targetAspect = targetDimension.aspectPreset.aspectRatio

        val srcAspect = srcWidth / srcHeight

        val cropWidth: Float
        val cropHeight: Float
        val cropX: Float
        val cropY: Float

        if (srcAspect > targetAspect) {
            // Source is wider than target
            cropHeight = srcHeight
            cropWidth = srcHeight * targetAspect
            cropX = (srcWidth - cropWidth) / 2f
            cropY = 0f
        } else {
            // Source is taller than target
            cropWidth = srcWidth
            cropHeight = srcWidth / targetAspect
            cropX = 0f
            cropY = (srcHeight - cropHeight) / 2f
        }

        val cropped = Bitmap.createBitmap(
            orientedBitmap,
            cropX.toInt().coerceAtLeast(0),
            cropY.toInt().coerceAtLeast(0),
            cropWidth.toInt().coerceAtMost(orientedBitmap.width),
            cropHeight.toInt().coerceAtMost(orientedBitmap.height)
        )

        // Rescale to exact target resolution
        return Bitmap.createScaledBitmap(
            cropped,
            targetDimension.width,
            targetDimension.height,
            true
        )
    }

    /**
     * Auto background segmentation algorithm:
     * Removes the background based on color saliency, perimeter baseline sampling,
     * and center-weighted foreground detection.
     */
    fun segmentBackground(
        source: Bitmap,
        config: SegmentationConfig
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample border pixels to determine ambient background color
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var sampleCount = 0

        // Sample top and bottom rows
        for (x in 0 until width step 4) {
            val topP = pixels[x]
            val botP = pixels[(height - 1) * width + x]
            totalR += Color.red(topP) + Color.red(botP)
            totalG += Color.green(topP) + Color.green(botP)
            totalB += Color.blue(topP) + Color.blue(botP)
            sampleCount += 2
        }

        // Sample left and right columns
        for (y in 0 until height step 4) {
            val leftP = pixels[y * width]
            val rightP = pixels[y * width + (width - 1)]
            totalR += Color.red(leftP) + Color.red(rightP)
            totalG += Color.green(leftP) + Color.green(rightP)
            totalB += Color.blue(leftP) + Color.blue(rightP)
            sampleCount += 2
        }

        val bgR = (totalR / sampleCount).toInt()
        val bgG = (totalG / sampleCount).toInt()
        val bgB = (totalB / sampleCount).toInt()

        val maxColorDist = sqrt(255.0 * 255.0 * 3.0)
        val thresholdDist = maxColorDist * config.thresholdSensitivity.toDouble()
        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxRadius = sqrt(centerX * centerX + centerY * centerY)

        val outPixels = IntArray(width * height)

        for (y in 0 until height) {
            val rowOffset = y * width
            val distY = (y - centerY) / centerY
            for (x in 0 until width) {
                val p = pixels[rowOffset + x]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                val dr = (r - bgR).toDouble()
                val dg = (g - bgG).toDouble()
                val db = (b - bgB).toDouble()
                val colorDist = sqrt(dr * dr + dg * dg + db * db)

                // Foreground center weight: center pixels are more likely foreground
                val distX = (x - centerX) / centerX
                val radialDist = sqrt(distX * distX + distY * distY).coerceIn(0.0, 1.0)
                // Center bias lowers threshold for the center region
                val adjustedThreshold = thresholdDist * (0.65 + 0.45 * radialDist)

                if (colorDist < adjustedThreshold) {
                    // Background pixel -> make transparent or fill color
                    if (config.replaceWithColor != null) {
                        outPixels[rowOffset + x] = config.replaceWithColor
                    } else {
                        // Soft edge feathering
                        val diff = adjustedThreshold - colorDist
                        if (diff < 12.0) {
                            val alpha = ((12.0 - diff) / 12.0 * 255.0).toInt().coerceIn(0, 255)
                            outPixels[rowOffset + x] = Color.argb(alpha, r, g, b)
                        } else {
                            outPixels[rowOffset + x] = Color.TRANSPARENT
                        }
                    }
                } else {
                    // Foreground pixel -> keep original
                    outPixels[rowOffset + x] = Color.argb(255, r, g, b)
                }
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Generates augmented variations of a base image:
     * - Rotations (+15°, -15°, 90°)
     * - Horizontal flip
     * - Translation shift (offset)
     * - Lighting variations (brightness up, brightness down, high contrast)
     */
    fun generateAugmentations(
        baseBitmap: Bitmap,
        config: AugmentationConfig
    ): List<AugmentedVariation> {
        val variations = mutableListOf<AugmentedVariation>()
        val w = baseBitmap.width
        val h = baseBitmap.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Rotation +15°
        if (config.rotateAngles && variations.size < config.multiplier) {
            val matrix = Matrix().apply {
                postRotate(15f, cx, cy)
            }
            val rotBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(rotBm)
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(rotBm, AugmentationType.ROTATE_CW_15, "rot15"))
        }

        // 2. Rotation -15°
        if (config.rotateAngles && variations.size < config.multiplier) {
            val matrix = Matrix().apply {
                postRotate(-15f, cx, cy)
            }
            val rotBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(rotBm)
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(rotBm, AugmentationType.ROTATE_CCW_15, "rot_neg15"))
        }

        // 3. Horizontal Flip
        if (config.horizontalFlip && variations.size < config.multiplier) {
            val matrix = Matrix().apply {
                postScale(-1f, 1f, cx, cy)
            }
            val flipBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(flipBm)
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(flipBm, AugmentationType.FLIP_HORIZONTAL, "flip_h"))
        }

        // 3b. Vertical Flip
        if (config.verticalFlip && variations.size < config.multiplier) {
            val matrix = Matrix().apply {
                postScale(1f, -1f, cx, cy)
            }
            val flipVBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(flipVBm)
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(flipVBm, AugmentationType.FLIP_VERTICAL, "flip_v"))
        }

        // 4. Lighting Up (+25%)
        if (config.lightingVariations && variations.size < config.multiplier) {
            val brightBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(brightBm)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, 15f,
                        0f, 1.25f, 0f, 0f, 15f,
                        0f, 0f, 1.25f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
            variations.add(AugmentedVariation(brightBm, AugmentationType.BRIGHTNESS_HIGH, "bright_hi"))
        }

        // 5. Lighting Down (-25%)
        if (config.lightingVariations && variations.size < config.multiplier) {
            val dimBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(dimBm)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                val cm = ColorMatrix(
                    floatArrayOf(
                        0.75f, 0f, 0f, 0f, -10f,
                        0f, 0.75f, 0f, 0f, -10f,
                        0f, 0f, 0.75f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
            variations.add(AugmentedVariation(dimBm, AugmentationType.BRIGHTNESS_LOW, "bright_lo"))
        }

        // 6. Translation Shift (+8% X, -8% Y)
        if (config.translationShift && variations.size < config.multiplier) {
            val shiftBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(shiftBm)
            val matrix = Matrix().apply {
                postTranslate(w * 0.08f, -h * 0.08f)
            }
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(shiftBm, AugmentationType.SHIFT_OFFSET, "shift"))
        }

        // 7. High Contrast (+30%)
        if (config.lightingVariations && variations.size < config.multiplier) {
            val contrastBm = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(contrastBm)
            val scale = 1.3f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                val cm = ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
            variations.add(AugmentedVariation(contrastBm, AugmentationType.CONTRAST_HIGH, "contrast_hi"))
        }

        // 8. Rotate 90°
        if (config.rotateAngles && variations.size < config.multiplier) {
            val matrix = Matrix().apply {
                postRotate(90f, cx, cy)
            }
            val rot90 = Bitmap.createBitmap(w, h, baseBitmap.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(rot90)
            canvas.drawBitmap(baseBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            variations.add(AugmentedVariation(rot90, AugmentationType.ROTATE_90, "rot90"))
        }

        return variations
    }

    /**
     * Saves bitmap to disk with consistent compression.
     * When transparent/PNG, uses PNG format.
     * When standard ML dataset, uses JPEG with adjustable quality to minimize file size on disk.
     */
    fun saveBitmapToFile(
        bitmap: Bitmap,
        targetFile: File,
        isPngOrTransparent: Boolean,
        jpegQuality: Int = 88
    ): Long {
        FileOutputStream(targetFile).use { out ->
            if (isPngOrTransparent) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(40, 100), out)
            }
            out.flush()
        }
        return targetFile.length()
    }
}
