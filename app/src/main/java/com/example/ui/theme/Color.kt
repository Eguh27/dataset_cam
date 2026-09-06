package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Obsidian & Slate Palette
val SlateDark950 = Color(0xFF090D14)
val SlateDark900 = Color(0xFF0F172A)
val SlateDark800 = Color(0xFF1E293B)
val SlateDark700 = Color(0xFF334155)

// Aliases mapped to unified Slate Obsidian
val WarmPlumCanvas = SlateDark950
val WarmPlumSurface = SlateDark900
val WarmPlumSurfaceElevated = SlateDark800
val WarmPlumDark = Color(0xFF06090E)
val WarmPlumSheet = Color(0xF00F172A)

// Focused, cohesive accents (Precision Cobalt & Sky Cyan)
val NeonCyan = Color(0xFF38BDF8)
val NeonCyanDim = Color(0xFF0284C7)
val NeonPurple = Color(0xFF818CF8)
val NeonPurpleDim = Color(0xFF6366F1)
val NeonEmerald = Color(0xFF10B981)
val NeonRose = Color(0xFFF43F5E)
val NeonAmber = Color(0xFFF59E0B)

// Canvas aliases
val DarkCanvas = SlateDark950
val DarkSurface = SlateDark900
val DarkSurfaceElevated = SlateDark800
val DarkSurfaceHighlight = Color(0xFF283548)

// Glassmorphism Token Colors
val GlassBackground = Color(0x1A38BDF8)
val GlassBackgroundSolid = Color(0x221E293B)
val GlassBorder = Color(0x2694A3B8)
val GlassBorderActive = Color(0x8038BDF8)
val GlassOverlay = Color(0xDC090D14)
val FrostedStripBackground = Color(0x660F172A)

// Text & Accents (High contrast, clean typography)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Refined Gradients
val LiquidCyanViolet = Brush.linearGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
)

val LiquidEmeraldCyan = Brush.linearGradient(
    colors = listOf(Color(0xFF10B981), Color(0xFF0EA5E9))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0x241E293B), Color(0x141E293B))
)

val DarkBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF090D14))
)

val BlueActionGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0EA5E9), Color(0xFF2563EB))
)
