package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.data.model.AspectRatioPreset
import com.example.ui.theme.NeonCyan

@Composable
fun AspectRatioViewfinderMask(
    aspectPreset: AspectRatioPreset,
    showGrid: Boolean = true,
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val totalWidth = size.width
        val totalHeight = size.height
        val targetAspect = aspectPreset.aspectRatio

        // Calculate maximum fitting rectangle inside canvas with target aspect ratio
        val (frameWidth, frameHeight) = if (totalWidth / totalHeight > targetAspect) {
            val h = totalHeight * 0.90f
            val w = h * targetAspect
            Pair(w, h)
        } else {
            val w = totalWidth * 0.92f
            val h = w / targetAspect
            Pair(w, h)
        }

        val frameLeft = (totalWidth - frameWidth) / 2f
        val frameTop = (totalHeight - frameHeight) / 2f
        val frameRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)

        // Dim outside region
        val framePath = Path().apply {
            addRect(frameRect)
        }

        clipPath(framePath, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = size
            )
        }

        // Draw Frame Border
        drawRect(
            color = accentColor.copy(alpha = 0.85f),
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameWidth, frameHeight),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Corner Brackets
        val bracketLen = 20.dp.toPx()
        val bracketStroke = Stroke(width = 3.5.dp.toPx())

        // Top-Left
        drawLine(accentColor, Offset(frameLeft - 1, frameTop), Offset(frameLeft + bracketLen, frameTop), strokeWidth = bracketStroke.width)
        drawLine(accentColor, Offset(frameLeft, frameTop - 1), Offset(frameLeft, frameTop + bracketLen), strokeWidth = bracketStroke.width)

        // Top-Right
        drawLine(accentColor, Offset(frameRect.right - bracketLen, frameTop), Offset(frameRect.right + 1, frameTop), strokeWidth = bracketStroke.width)
        drawLine(accentColor, Offset(frameRect.right, frameTop - 1), Offset(frameRect.right, frameTop + bracketLen), strokeWidth = bracketStroke.width)

        // Bottom-Left
        drawLine(accentColor, Offset(frameLeft - 1, frameRect.bottom), Offset(frameLeft + bracketLen, frameRect.bottom), strokeWidth = bracketStroke.width)
        drawLine(accentColor, Offset(frameLeft, frameRect.bottom - bracketLen), Offset(frameLeft, frameRect.bottom + 1), strokeWidth = bracketStroke.width)

        // Bottom-Right
        drawLine(accentColor, Offset(frameRect.right - bracketLen, frameRect.bottom), Offset(frameRect.right + 1, frameRect.bottom), strokeWidth = bracketStroke.width)
        drawLine(accentColor, Offset(frameRect.right, frameRect.bottom - bracketLen), Offset(frameRect.right, frameRect.bottom + 1), strokeWidth = bracketStroke.width)

        // Rule of Thirds Grid lines (subtle)
        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.20f)
            val gridStroke = Stroke(width = 1.dp.toPx())

            val thirdW = frameWidth / 3f
            val thirdH = frameHeight / 3f

            // Vertical lines
            drawLine(gridColor, Offset(frameLeft + thirdW, frameTop), Offset(frameLeft + thirdW, frameRect.bottom), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(frameLeft + 2 * thirdW, frameTop), Offset(frameLeft + 2 * thirdW, frameRect.bottom), strokeWidth = gridStroke.width)

            // Horizontal lines
            drawLine(gridColor, Offset(frameLeft, frameTop + thirdH), Offset(frameRect.right, frameTop + thirdH), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(frameLeft, frameTop + 2 * thirdH), Offset(frameRect.right, frameTop + 2 * thirdH), strokeWidth = gridStroke.width)
        }
    }
}
