package org.tasks.themes

import androidx.compose.ui.graphics.Color
import org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

fun contentColorFor(backgroundColor: Int): Color =
    when {
        backgroundColor == 0 -> Color.White
        calculateContrast(WHITE, backgroundColor) < 3 -> Color.Black
        else -> Color.White
    }

fun calculateContrast(foreground: Int, background: Int): Double {
    var foreground = foreground
    require(alpha(background) == 255) {
        ("background can not be translucent: #" + Integer.toHexString(background))
    }
    if (alpha(foreground) < 255) {
        // If the foreground is translucent, composite the foreground over the background
        foreground = compositeColors(foreground, background)
    }

    val luminance1: Double = calculateLuminance(foreground) + 0.05
    val luminance2: Double = calculateLuminance(background) + 0.05

    // Now return the lighter luminance divided by the darker luminance
    return max(luminance1, luminance2) / min(luminance1, luminance2)
}

private fun compositeAlpha(foregroundAlpha: Int, backgroundAlpha: Int): Int =
    0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF)

private fun compositeComponent(fgC: Int, fgA: Int, bgC: Int, bgA: Int, a: Int): Int {
    if (a == 0) return 0
    return ((0xFF * fgC * fgA) + (bgC * bgA * (0xFF - fgA))) / (a * 0xFF)
}

private fun compositeColors(foreground: Int, background: Int): Int {
    val bgAlpha = alpha(background)
    val fgAlpha = alpha(foreground)
    val a = compositeAlpha(fgAlpha, bgAlpha)
    return argb(
        alpha = a,
        red = compositeComponent(red(foreground), fgAlpha, red(background), bgAlpha, a),
        green = compositeComponent(green(foreground), fgAlpha, green(background), bgAlpha, a),
        blue = compositeComponent(blue(foreground), fgAlpha, blue(background), bgAlpha, a)
    )
}

private fun alpha(color: Int): Int = color ushr 24

private fun red(color: Int): Int = (color shr 16) and 0xFF

private fun green(color: Int): Int = (color shr 8) and 0xFF

private fun blue(color: Int): Int = color and 0xFF

private fun argb(alpha: Int, red: Int, green: Int, blue: Int) =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue

private fun calculateLuminance(color: Int): Double {
    val result: DoubleArray = getTempDouble3Array()
    colorToXYZ(color, result)
    // Luminance is the Y component
    return result[1] / 100
}

private fun getTempDouble3Array(): DoubleArray {
    var result: DoubleArray? = TEMP_ARRAY.get()
    if (result == null) {
        result = DoubleArray(3)
        TEMP_ARRAY.set(result)
    }
    return result
}

private val TEMP_ARRAY = ThreadLocal<DoubleArray>()

private fun colorToXYZ(color: Int, outXyz: DoubleArray) {
    RGBToXYZ(red(color), green(color), blue(color), outXyz)
}

private fun RGBToXYZ(r: Int, g: Int, b: Int, outXyz: DoubleArray) {
    require(outXyz.size == 3) { "outXyz must have a length of 3." }

    var sr = r / 255.0
    sr = if (sr < 0.04045) sr / 12.92 else ((sr + 0.055) / 1.055).pow(2.4)
    var sg = g / 255.0
    sg = if (sg < 0.04045) sg / 12.92 else ((sg + 0.055) / 1.055).pow(2.4)
    var sb = b / 255.0
    sb = if (sb < 0.04045) sb / 12.92 else ((sb + 0.055) / 1.055).pow(2.4)

    outXyz[0] = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805)
    outXyz[1] = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722)
    outXyz[2] = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505)
}
