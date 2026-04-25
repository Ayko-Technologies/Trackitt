package com.ayko.trackitt.util

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun oklchToColor(l: Float, c: Float, h: Float, alpha: Float = 1f): Color {
    val hRad = Math.toRadians(h.toDouble())
    val a = (c * cos(hRad)).toFloat()
    val b = (c * sin(hRad)).toFloat()
    return oklabToColor(l, a, b, alpha)
}

private fun oklabToColor(l: Float, a: Float, b: Float, alpha: Float): Color {
    val l_ = l + 0.3963377774f * a + 0.2158037573f * b
    val m_ = l - 0.1055613458f * a - 0.0638541728f * b
    val s_ = l - 0.0894841775f * a - 1.2914855480f * b

    val l3 = l_ * l_ * l_
    val m3 = m_ * m_ * m_
    val s3 = s_ * s_ * s_

    val r = +4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
    val g = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
    val bl = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3

    return Color(
        red = linearToSrgb(r),
        green = linearToSrgb(g),
        blue = linearToSrgb(bl),
        alpha = alpha,
    )
}

private fun linearToSrgb(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    return if (clamped <= 0.0031308f) {
        12.92f * clamped
    } else {
        1.055f * clamped.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f
    }
}
