package org.tasks.themes

import android.content.Context
import androidx.annotation.ColorInt
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.preferences.Preferences
import javax.inject.Inject

class ColorProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    preferences: Preferences
) {

    companion object {
        const val BLUE_500 = -14575885
        private const val RED_500 = -769226
        private const val AMBER_500 = -16121
        private const val GREY_500 = -6381922
        private const val WHITE = -1
        private const val BLACK = -16777216

        private val saturated: Map<Int, Int> = hashMapOf(
                // 2014 material design palette
                -10453621 to -7297874, // blue_grey
                RED_500 to -1739917, // red
                -1499549 to -1023342, // pink
                -6543440 to -4560696, // purple
                -10011977 to -6982195, // deep purple
//                -12627531 to -8812853, // indigo
                BLUE_500 to -10177034, // blue
                -16537100 to -11549705, // light blue
                -16728876 to -11677471, // cyan
//                -16738680 to -11684180, // teal
                -11751600 to -8271996, // green
                -7617718 to -5319295, // light green
                -3285959 to -2300043, // lime
                -5317 to -3722, // yellow
                AMBER_500 to -10929, // amber
                -26624 to -18611, // orange
                -43230 to -30107, // deep orange
//                -8825528 to -6190977, // brown
                GREY_500 to -2039584, // grey
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

        fun priorityColor(priority: Int, isDarkMode: Boolean = false, desaturate: Boolean = false): Int {
            val color = when (priority) {
                in Int.MIN_VALUE..Task.Priority.HIGH -> RED_500
                Task.Priority.MEDIUM -> AMBER_500
                Task.Priority.LOW -> BLUE_500
                else -> GREY_500
            }
            return if (isDarkMode && desaturate) {
                saturated[color] ?: color
            } else {
                color
            }
        }
    }

    private val isDark = context.resources.getBoolean(R.bool.is_dark)
    private val desaturate = preferences.desaturateDarkMode

    private fun getColor(@ColorInt color: Int, adjust: Boolean) =
            if (adjust && isDark && desaturate) {
                saturated[color] ?: color
            } else {
                color
            }

    fun getThemeColor(@ColorInt color: Int, adjust: Boolean = true) =
            ThemeColor(context, color, getColor(color, adjust))

    fun getPriorityColor(priority: Int, adjust: Boolean = true) = priorityColor(
        priority = priority,
        isDarkMode = isDark,
        desaturate = adjust && desaturate,
    )

    fun getThemeAccent(index: Int) = ThemeAccent(context, if (isDark && desaturate) {
        ThemeAccent.ACCENTS_DESATURATED[index]
    } else {
        ThemeAccent.ACCENTS[index]
    })

    fun getThemeColors(adjust: Boolean = true) = ThemeColor.COLORS.map { c ->
        getThemeColor(context.getColor(c), adjust)
    }

    fun getWidgetColors() = getThemeColors(false)

    fun getAccentColors() = ThemeAccent.ACCENTS.indices.map(this@ColorProvider::getThemeAccent)
}