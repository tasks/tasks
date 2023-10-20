package org.tasks.ui

import android.app.Activity
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.themes.ColorProvider
import javax.inject.Inject

@Deprecated("remove me")
class ChipProvider @Inject constructor(
    private val activity: Activity,
    private val inventory: Inventory,
    val lists: ChipListCache,
    private val colorProvider: ColorProvider,
) {

    fun getColor(theme: Int): Int {
        if (theme != 0) {
            val color = colorProvider.getThemeColor(theme, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color.primaryColor
            }
        }
        return activity.getColor(R.color.default_chip_background)
    }
}
