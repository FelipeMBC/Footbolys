package com.fmarquez.footboly.util

import androidx.compose.ui.graphics.Color

fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLong(16)
        val r = ((rgb shr 16) and 0xFF).toInt()
        val g = ((rgb shr 8) and 0xFF).toInt()
        val b = (rgb and 0xFF).toInt()
        Color(r, g, b)
    } catch (_: Exception) {
        Color(0xFF1E6B45)
    }
}

/** Mezcla el color base con blanco al 15%, generando un tono muy suave para fondos. */
fun teamColorLight(base: Color): Color = Color(
    red   = base.red   * 0.15f + 0.85f,
    green = base.green * 0.15f + 0.85f,
    blue  = base.blue  * 0.15f + 0.85f,
    alpha = 1f
)