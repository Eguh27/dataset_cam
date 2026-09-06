package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    backgroundBrush: Brush? = null,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val bgModifier = when {
        backgroundBrush != null -> Modifier.background(backgroundBrush)
        backgroundColor != null -> Modifier.background(backgroundColor)
        else -> Modifier.background(GlassCardGradient)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(bgModifier)
            .border(borderWidth, borderColor, shape)
            .padding(1.dp),
        content = content
    )
}

@Composable
fun LiquidPulseRing(
    modifier: Modifier = Modifier,
    pulseColor: Color = NeonCyan,
    minScale: Float = 1.0f,
    maxScale: Float = 1.25f,
    durationMillis: Int = 1800
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .border(
                width = 2.dp,
                brush = Brush.radialGradient(
                    colors = listOf(pulseColor.copy(alpha = alpha), Color.Transparent)
                ),
                shape = RoundedCornerShape(percent = 50)
            )
    )
}

@Composable
fun LiquidGlowBorder(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    primaryColor: Color = NeonCyan,
    secondaryColor: Color = NeonPurple,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_border")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_gradient"
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.8f + 0.2f * animatedProgress),
            secondaryColor.copy(alpha = 0.5f + 0.3f * (1f - animatedProgress)),
            primaryColor.copy(alpha = 0.4f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0x351F1318))
            .border(1.5.dp, gradientBrush, shape),
        content = content
    )
}

