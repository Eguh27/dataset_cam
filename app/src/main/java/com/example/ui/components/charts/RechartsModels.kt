package com.example.ui.components.charts

import androidx.compose.ui.graphics.Color

/**
 * Data model for category image count in Recharts-style bar charts.
 */
data class CategoryBarItem(
    val classId: Long,
    val name: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val aspectRatioBreakdown: Map<String, Int> = emptyMap()
)

/**
 * Data model for aspect ratio distribution in Recharts-style donut/pie charts.
 */
data class AspectRatioSliceItem(
    val ratioKey: String,
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val recommendedModels: String
)

/**
 * Health assessment of dataset balance and uniformity.
 */
data class DatasetHealthIndex(
    val statusLabel: String,
    val statusColor: Color,
    val description: String,
    val maxClassRatio: Float,
    val minClassRatio: Float,
    val isBalanced: Boolean
)

object RechartsPalette {
    val FallbackColors = listOf(
        Color(0xFF38BDF8), // Cyan
        Color(0xFFC084FC), // Purple
        Color(0xFFF472B6), // Rose
        Color(0xFF34D399), // Emerald
        Color(0xFFFBBF24), // Amber
        Color(0xFF60A5FA), // Blue
        Color(0xFFA78BFA)  // Violet
    )

    fun parseColor(hex: String?, index: Int = 0): Color {
        if (hex.isNullOrBlank()) return FallbackColors[index % FallbackColors.size]
        return try {
            val formatted = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(formatted))
        } catch (_: Exception) {
            FallbackColors[index % FallbackColors.size]
        }
    }
}
