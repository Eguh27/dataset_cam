package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Warm Plum & Obsidian Palette (as featured in reference mockup)
val WarmPlumCanvas = Color(0xFF221417)
val WarmPlumSurface = Color(0xFF2C191D)
val WarmPlumSurfaceElevated = Color(0xFF3B2126)
val WarmPlumDark = Color(0xFF180D10)
val WarmPlumSheet = Color(0xEB331D22)

// Neon & Pastel Accents
val NeonCyan = Color(0xFF38BDF8)
val NeonCyanDim = Color(0xFF0284C7)
val NeonPurple = Color(0xFFC084FC)
val NeonPurpleDim = Color(0xFF9333EA)
val NeonEmerald = Color(0xFF34D399)
val NeonRose = Color(0xFFF472B6)
val NeonAmber = Color(0xFFFBBF24)

// Canvas aliases
val DarkCanvas = WarmPlumCanvas
val DarkSurface = WarmPlumSurface
val DarkSurfaceElevated = WarmPlumSurfaceElevated
val DarkSurfaceHighlight = Color(0xFF4A2B31)

// Glassmorphism Token Colors
val GlassBackground = Color(0x28FFFFFF)
val GlassBackgroundSolid = Color(0x38FFFFFF)
val GlassBorder = Color(0x2EFFFFFF)
val GlassBorderActive = Color(0x80C084FC)
val GlassOverlay = Color(0xCC180D10)
val FrostedStripBackground = Color(0x4D362226)

// Text & Accents
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFD4C2C5)
val TextMuted = Color(0xFF9E8B8F)

// Liquid Animation Gradients
val LiquidCyanViolet = Brush.linearGradient(
    colors = listOf(Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFF38BDF8))
)

val LiquidEmeraldCyan = Brush.linearGradient(
    colors = listOf(Color(0xFF34D399), Color(0xFF38BDF8))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0x38FFFFFF), Color(0x18FFFFFF))
)

val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF2A181C), Color(0xFF201215), Color(0xFF160B0E))
)

val BlueActionGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
)
