package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import org.tasks.R
import org.tasks.injection.ForActivity
import org.tasks.preferences.Preferences
import javax.inject.Inject

class ColorProvider @Inject constructor(@ForActivity private val context: Context, preferences: Preferences) {

    companion object {
        const val BLUE = -14575885
        private const val RED = -769226
        private const val AMBER = -16121
        private const val GREY = -6381922
        private const val WHITE = -1
        private const val BLACK = -16777216

        private val saturated: Map<Int, Int> = hashMapOf(
                // 2014 material design palette
                -10453621 to -5194043, // blue_grey
                -12434878 to -14606047, // grey
                RED to -1074534, // red
                -1499549 to -749647, // pink
                -6543440 to -3238952, // purple
                -10011977 to -5005861, // deep purple
//                -12627531 to -6313766, // indigo
                BLUE to -7288071, // blue
                -16537100 to -8268550, // light blue
                -16728876 to -8331542, // cyan
//                -16738680 to -8336444, // teal
                -11751600 to -5908825, // green
                -7617718 to -3808859, // light green
                -3285959 to -1642852, // lime
                -5317 to -2659, // yellow
                AMBER to -8062, // amber
                -26624 to -13184, // orange
                -43230 to -21615, // deep orange
//                -8825528 to -4412764, // brown
                GREY to -1118482, // grey
                WHITE to BLACK,

                // 2019 google calendar
                -2818048 to -3397335, // tomato
                -765666 to -2136512, // tangerine
                -1086464 to -2459092, // pumpkin
                -1010944 to -2254804, // mango
                -606426 to -2050234, // banana
                -1784767 to -2769834, // citron
                -4142541 to -4274613, // avocado
                -8604862 to -7817131, // pistachio
                -16023485 to -14116514, // basil
                -16738680 to -14571622, // eucalyptus
                -13388167 to -11879802, // sage
                -16540699 to -13787178, // peacock
                -12417548 to -10974241, // cobalt
                -12627531 to -11312199, // blueberry
                -8812853 to -8615738, // lavender
                -5005861 to -5597744, // wisteria
                -6395473 to -5934410, // amethyst
                -7461718 to -6668365, // grape
                -5434281 to -4967572, // radicchio
                -2614432 to -3261327, // cherry blossom
                -1672077 to -2654344, // flamingo
                -8825528 to -6984611, // cocoa
                -10395295 to -7895161, // graphite
                -5792882 to -5135210 // birch
        )
    }

    private val isDark = context.resources.getBoolean(R.bool.is_dark)
    private val desaturate = preferences.getBoolean(R.string.p_desaturate_colors, true)

    private fun getColor(@ColorInt color: Int, adjust: Boolean) =
            if (adjust && isDark && desaturate) {
                saturated[color] ?: color
            } else {
                color
            }

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust))

    fun getPriorityColor(priority: Int, adjust: Boolean = true) = when (priority) {
        in Int.MIN_VALUE..0 -> getColor(RED, adjust)
        1 -> getColor(AMBER, adjust)
        2 -> getColor(BLUE, adjust)
        else -> GREY
    }

    fun getThemeAccent(index: Int) = ThemeAccent(context, if (isDark && desaturate) {
        ThemeAccent.ACCENTS_DESATURATED[index]
    } else {
        ThemeAccent.ACCENTS[index]
    })

    fun getThemeColors(adjust: Boolean = true) = ThemeColor.COLORS.map { c ->
        getThemeColor(ContextCompat.getColor(context, c), adjust)
    }

    fun getWidgetColors() = getThemeColors(false)

    fun getAccentColors() = ThemeAccent.ACCENTS.indices.map(this@ColorProvider::getThemeAccent)
}