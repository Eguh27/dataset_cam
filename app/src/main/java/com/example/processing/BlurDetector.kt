package com.example.processing

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Blur state classification for training data suitability.
 */
enum class BlurStatus(
    val title: String,
    val shortLabel: String,
    val description: String,
    val icon: String
) {
    SHARP(
        title = "Fokus Tajam",
        shortLabel = "Tajam",
        description = "Kualitas detail dan tepi sangat baik untuk dataset ML.",
        icon = "✓"
    ),
    MODERATE(
        title = "Cukup Fokus",
        shortLabel = "Cukup",
        description = "Ketajaman memadai, tahan kamera stabil untuk hasil maksimal.",
        icon = "ℹ"
    ),
    BLURRY(
        title = "Terlalu Buram",
        shortLabel = "Buram",
        description = "Gambar terlalu buram dan dapat menurunkan keandalan model ML.",
        icon = "⚠️"
    )
}

/**
 * Result metrics emitted by real-time blur analysis.
 */
data class BlurAnalysisResult(
    val rawVariance: Double = 150.0,
    val score: Int = 85, // 0 to 100
    val status: BlurStatus = BlurStatus.SHARP,
    val isBlurry: Boolean = false,
    val message: String = "Fokus tajam",
    val timestamp: Long = System.currentTimeMillis()
)

object BlurDetector {

    const val DEFAULT_BLUR_THRESHOLD = 70.0
    const val MODERATE_THRESHOLD = 120.0

    /**
     * Analyzes an incoming CameraX ImageProxy frame using direct Y (luminance) plane access.
     * Operates zero-copy on the buffer for high throughput (30+ FPS) with minimal CPU usage.
     */
    fun analyzeImageProxy(
        imageProxy: ImageProxy,
        blurThreshold: Double = DEFAULT_BLUR_THRESHOLD
    ): BlurAnalysisResult {
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        // Focus ROI on the central 60% of the viewfinder where subject usually sits
        val startX = (width * 0.20).toInt()
        val endX = (width * 0.80).toInt()
        val startY = (height * 0.20).toInt()
        val endY = (height * 0.80).toInt()

        // Downsample step to keep processing well under 3ms per frame
        val step = 2

        var sumLaplacian = 0.0
        var sumLaplacianSq = 0.0
        var count = 0

        // Helper lambda to read a luminance byte at (x, y)
        fun getPixel(x: Int, y: Int): Int {
            val index = y * rowStride + x * pixelStride
            return if (index >= 0 && index < buffer.limit()) {
                buffer.get(index).toInt() and 0xFF
            } else 0
        }

        for (y in (startY + step) until (endY - step) step step) {
            for (x in (startX + step) until (endX - step) step step) {
                val center = getPixel(x, y)
                val left = getPixel(x - step, y)
                val right = getPixel(x + step, y)
                val top = getPixel(x, y - step)
                val bottom = getPixel(x, y + step)

                // 2D discrete Laplacian: L(x+1) + L(x-1) + L(y+1) + L(y-1) - 4*L(x,y)
                val laplacian = (left + right + top + bottom) - (4 * center)
                sumLaplacian += laplacian
                sumLaplacianSq += (laplacian * laplacian)
                count++
            }
        }

        if (count == 0) {
            return BlurAnalysisResult()
        }

        val mean = sumLaplacian / count
        val variance = max(0.0, (sumLaplacianSq / count) - (mean * mean))

        // Normalized score 0..100 based on standard natural camera variance
        val normalizedScore = min(100, max(0, (variance / 1.8).toInt()))

        val status = when {
            variance < blurThreshold -> BlurStatus.BLURRY
            variance < MODERATE_THRESHOLD -> BlurStatus.MODERATE
            else -> BlurStatus.SHARP
        }

        val isBlurry = status == BlurStatus.BLURRY
        val message = when (status) {
            BlurStatus.SHARP -> "Fokus Tajam ($normalizedScore%)"
            BlurStatus.MODERATE -> "Cukup Fokus ($normalizedScore%)"
            BlurStatus.BLURRY -> "Citra Buram! Tahan kamera tetap stabil ($normalizedScore%)"
        }

        return BlurAnalysisResult(
            rawVariance = variance,
            score = normalizedScore,
            status = status,
            isBlurry = isBlurry,
            message = message
        )
    }

    /**
     * Analyzes a Bitmap (e.g. captured photo or imported image from gallery).
     */
    fun analyzeBitmap(
        bitmap: Bitmap,
        blurThreshold: Double = DEFAULT_BLUR_THRESHOLD
    ): BlurAnalysisResult {
        // Downscale large bitmap for fast analysis
        val targetWidth = min(bitmap.width, 320)
        val targetHeight = (bitmap.height * (targetWidth.toFloat() / bitmap.width)).toInt()
        val scaled = if (bitmap.width > targetWidth) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
        } else bitmap

        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale luminance
        val lum = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        var sumLaplacian = 0.0
        var sumLaplacianSq = 0.0
        var count = 0
        val step = 1

        val startX = (width * 0.15).toInt()
        val endX = (width * 0.85).toInt()
        val startY = (height * 0.15).toInt()
        val endY = (height * 0.85).toInt()

        for (y in (startY + step) until (endY - step) step step) {
            for (x in (startX + step) until (endX - step) step step) {
                val center = lum[y * width + x]
                val left = lum[y * width + (x - step)]
                val right = lum[y * width + (x + step)]
                val top = lum[(y - step) * width + x]
                val bottom = lum[(y + step) * width + x]

                val laplacian = (left + right + top + bottom) - (4 * center)
                sumLaplacian += laplacian
                sumLaplacianSq += (laplacian * laplacian)
                count++
            }
        }

        if (count == 0) return BlurAnalysisResult()

        val mean = sumLaplacian / count
        val variance = max(0.0, (sumLaplacianSq / count) - (mean * mean))
        val normalizedScore = min(100, max(0, (variance / 1.8).toInt()))

        val status = when {
            variance < blurThreshold -> BlurStatus.BLURRY
            variance < MODERATE_THRESHOLD -> BlurStatus.MODERATE
            else -> BlurStatus.SHARP
        }

        return BlurAnalysisResult(
            rawVariance = variance,
            score = normalizedScore,
            status = status,
            isBlurry = status == BlurStatus.BLURRY,
            message = if (status == BlurStatus.BLURRY) "Citra Buram ($normalizedScore%)" else "Fokus Tajam ($normalizedScore%)"
        )
    }
}
