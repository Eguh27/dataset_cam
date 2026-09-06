package com.example.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.example.data.model.AugmentationParams
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Image processing utility for applying common computer vision and machine learning augmentations:
 * - Arbitrary continuous rotation (-180° to +180°) and fixed-angle rotation (90°, 180°, 270°)
 * - Fine-grained brightness adjustment (negative/dimming, positive/boosting)
 * - Horizontal flipping (left-to-right reflection)
 * - Vertical flipping (top-to-bottom reflection)
 * - Contrast scaling and composite augmentation pipeline
 */
object ImageProcessingUtility {

    /**
     * Rotates a bitmap around its center point by [degrees].
     * If [keepCanvasBounds] is true, preserves the original width and height without expanding the canvas.
     */
    fun rotate(
        source: Bitmap,
        degrees: Float,
        keepCanvasBounds: Boolean = true
    ): Bitmap {
        val normalizedDegrees = degrees % 360f
        if (normalizedDegrees == 0f) return source

        val w = source.width
        val h = source.height
        val cx = w / 2f
        val cy = h / 2f

        return if (keepCanvasBounds) {
            val matrix = Matrix().apply {
                postRotate(normalizedDegrees, cx, cy)
            }
            val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(source, matrix, paint)
            result
        } else {
            val matrix = Matrix().apply {
                postRotate(normalizedDegrees)
            }
            Bitmap.createBitmap(source, 0, 0, w, h, matrix, true)
        }
    }

    /**
     * Adjusts the brightness of a bitmap by [brightnessDelta] (-100f to +100f).
     * 0f means no modification. Negative values dim the image, positive values brighten it.
     */
    fun adjustBrightness(
        source: Bitmap,
        brightnessDelta: Float
    ): Bitmap {
        val clampedDelta = brightnessDelta.coerceIn(-100f, 100f)
        if (clampedDelta == 0f) return source

        val offset = (clampedDelta / 100f) * 255f
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            val cm = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, offset,
                    0f, 1f, 0f, 0f, offset,
                    0f, 0f, 1f, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Horizontally flips (mirrors) a bitmap across its vertical center axis.
     */
    fun flipHorizontal(source: Bitmap): Bitmap {
        return flip(source, horizontal = true, vertical = false)
    }

    /**
     * Vertically flips (mirrors) a bitmap across its horizontal center axis.
     */
    fun flipVertical(source: Bitmap): Bitmap {
        return flip(source, horizontal = false, vertical = true)
    }

    /**
     * Applies horizontal and/or vertical flipping in a single matrix transform.
     */
    fun flip(
        source: Bitmap,
        horizontal: Boolean,
        vertical: Boolean
    ): Bitmap {
        if (!horizontal && !vertical) return source

        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        val matrix = Matrix().apply {
            postScale(sx, sy, source.width / 2f, source.height / 2f)
        }
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Adjusts contrast using an RGB scaling factor ([contrastFactor]: 0.2f to 3.0f, 1.0f = neutral).
     */
    fun adjustContrast(
        source: Bitmap,
        contrastFactor: Float
    ): Bitmap {
        val scale = contrastFactor.coerceIn(0.2f, 3.0f)
        if (scale == 1.0f) return source

        val translate = (-0.5f * scale + 0.5f) * 255f
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
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
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Unified pipeline that applies rotation, brightness adjustment, horizontal flipping,
     * vertical flipping, and contrast in a single high-performance graphics pass.
     */
    fun applyAugmentations(
        source: Bitmap,
        params: AugmentationParams
    ): Bitmap {
        if (!params.isModified) return source

        val w = source.width
        val h = source.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Matrix composition (scale/flips + rotation)
        val matrix = Matrix()
        if (params.flipHorizontal || params.flipVertical) {
            val sx = if (params.flipHorizontal) -1f else 1f
            val sy = if (params.flipVertical) -1f else 1f
            matrix.postScale(sx, sy, cx, cy)
        }
        if (params.rotationDegrees != 0f) {
            matrix.postRotate(params.rotationDegrees, cx, cy)
        }

        // 2. ColorMatrix composition (Contrast + Brightness)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val hasColorAdjustment = params.brightnessDelta != 0f || params.contrastFactor != 1.0f

        if (hasColorAdjustment) {
            val scale = params.contrastFactor.coerceIn(0.2f, 3.0f)
            val contrastOffset = (-0.5f * scale + 0.5f) * 255f
            val brightnessOffset = (params.brightnessDelta.coerceIn(-100f, 100f) / 100f) * 255f
            val totalOffset = contrastOffset + brightnessOffset

            val cm = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, totalOffset,
                    0f, scale, 0f, 0f, totalOffset,
                    0f, 0f, scale, 0f, totalOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }

        val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, matrix, paint)
        return result
    }

    /**
     * Downsamples and generates a quick live preview bitmap for real-time UI sliders.
     */
    fun generatePreview(
        source: Bitmap,
        params: AugmentationParams,
        maxDimension: Int = 480
    ): Bitmap {
        val w = source.width
        val h = source.height
        val scale = if (max(w, h) > maxDimension) {
            maxDimension.toFloat() / max(w, h)
        } else {
            1f
        }

        val targetW = max(32, (w * scale).roundToInt())
        val targetH = max(32, (h * scale).roundToInt())

        val scaledSource = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } else {
            source
        }

        return applyAugmentations(scaledSource, params)
    }

    /**
     * Loads a bitmap safely from file with optional downsampling.
     */
    fun loadBitmapFromFile(file: File, maxDimension: Int? = null): Bitmap? {
        if (!file.exists()) return null
        return try {
            if (maxDimension != null) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                val maxSrc = max(options.outWidth, options.outHeight)
                var sampleSize = 1
                while (maxSrc / (sampleSize * 2) >= maxDimension) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            } else {
                BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Applies [params] to [sourceFile] and writes the output directly to [outputFile].
     * Returns the output file length in bytes.
     */
    fun createAugmentedFile(
        sourceFile: File,
        outputFile: File,
        params: AugmentationParams,
        isPng: Boolean = false,
        quality: Int = 88
    ): Long {
        val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: throw IllegalArgumentException("Gagal membaca file gambar sumber: ${sourceFile.absolutePath}")

        val augmentedBitmap = applyAugmentations(sourceBitmap, params)
        outputFile.parentFile?.mkdirs()

        FileOutputStream(outputFile).use { out ->
            if (isPng) {
                augmentedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else {
                augmentedBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out)
            }
            out.flush()
        }
        return outputFile.length()
    }
}
