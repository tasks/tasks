package org.tasks.ui

import android.app.Activity
import android.content.res.ColorStateList
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.data.TaskContainer
import org.tasks.filters.PlaceFilter
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons.getIconResId
import org.tasks.themes.ThemeColor
import java.util.*
import javax.inject.Inject

class ChipProvider @Inject constructor(
        private val activity: Activity,
        private val inventory: Inventory,
        private val lists: ChipListCache,
        private val preferences: Preferences,
        private val colorProvider: ColorProvider,
        private val locale: Locale) {

    private val iconAlpha: Int = (255 * ResourcesCompat.getFloat(activity.resources, R.dimen.alpha_secondary)).toInt()
    private var filled = false
    private var showIcon = false
    private var showText = false

    fun setStyle(style: Int) {
        filled = style == 1
    }

    fun setAppearance(appearance: Int) {
        showText = appearance != 2
        showIcon = appearance != 1
    }

    fun newSubtaskChip(task: TaskContainer, compact: Boolean): Chip {
        val chip = newChip(task)
        apply(
                chip,
                if (task.isCollapsed) R.drawable.ic_keyboard_arrow_down_black_24dp else R.drawable.ic_keyboard_arrow_up_black_24dp,
                if (compact) locale.formatNumber(task.children) else activity
                        .resources
                        .getQuantityString(R.plurals.subtask_count, task.children, task.children),
                0,
                showText = true,
                showIcon = true)
        return chip
    }

    fun getChips(filter: Filter?, isSubtask: Boolean, task: TaskContainer): List<Chip> {
        AndroidUtilities.assertMainThread()
        val chips = ArrayList<Chip>()
        if (task.hasChildren() && preferences.showSubtaskChip) {
            chips.add(newSubtaskChip(task, !showText))
        }
        if (task.hasLocation() && filter !is PlaceFilter && preferences.showPlaceChip) {
            val location = task.getLocation()
            newChip(PlaceFilter(location.place), R.drawable.ic_outline_place_24px)?.let(chips::add)
        }
        if (!isSubtask && preferences.showListChip) {
            if (!isNullOrEmpty(task.googleTaskList) && filter !is GtasksFilter) {
                newChip(lists.getGoogleTaskList(task.googleTaskList), R.drawable.ic_list_24px)
                        ?.let(chips::add)
            } else if (!isNullOrEmpty(task.caldav) && filter !is CaldavFilter) {
                newChip(lists.getCaldavList(task.caldav), R.drawable.ic_list_24px)?.let(chips::add)
            }
        }
        val tagString = task.tagsString
        if (!isNullOrEmpty(tagString) && preferences.showTagChip) {
            val tags = tagString.split(",").toHashSet()
            if (filter is TagFilter) {
                tags.remove(filter.uuid)
            }
            tags.mapNotNull(lists::getTag)
                    .sortedBy(TagFilter::listingTitle)
                    .map { newChip(it, R.drawable.ic_outline_label_24px)!! }
                    .let(chips::addAll)
        }
        return chips
    }

    fun apply(chip: Chip, tagData: TagData) {
        apply(
                chip,
                getIcon(tagData.getIcon()!!, R.drawable.ic_outline_label_24px),
                tagData.name,
                tagData.getColor()!!,
                showText = true,
                showIcon = true)
    }

    private fun newChip(filter: Filter?, defIcon: Int): Chip? {
        return newChip(filter, defIcon, showText, showIcon)
    }

    fun newChip(filter: Filter?, defIcon: Int, showText: Boolean, showIcon: Boolean): Chip? {
        if (filter == null) {
            return null
        }
        val chip = newChip(filter)
        apply(chip, getIcon(filter.icon, defIcon), filter.listingTitle, filter.tint, showText, showIcon)
        return chip
    }

    fun newClosableChip(tag: Any?): Chip {
        val chip = chip
        chip.isCloseIconVisible = true
        chip.tag = tag
        return chip
    }

    private fun newChip(tag: Any?): Chip {
        val chip = chip
        chip.tag = tag
        return chip
    }

    private val chip: Chip
        get() = activity
                .layoutInflater
                .inflate(if (filled) R.layout.chip_filled else R.layout.chip_outlined, null) as Chip

    private fun apply(
            chip: Chip,
            @DrawableRes icon: Int?,
            name: String?,
            theme: Int,
            showText: Boolean,
            showIcon: Boolean) {
        if (showText) {
            chip.text = name
            chip.iconEndPadding = 0f
        } else {
            chip.text = null
            chip.contentDescription = name
            chip.textStartPadding = 0f
            chip.chipEndPadding = 0f
        }
        val themeColor = getColor(theme)
        if (themeColor != null) {
            val primaryColor = themeColor.primaryColor
            val primaryColorSL = ColorStateList(arrayOf(intArrayOf()), intArrayOf(primaryColor))
            if (filled) {
                val colorOnPrimary = themeColor.colorOnPrimary
                val colorOnPrimarySL = ColorStateList(arrayOf(intArrayOf()), intArrayOf(colorOnPrimary))
                chip.chipBackgroundColor = primaryColorSL
                chip.setTextColor(colorOnPrimary)
                chip.closeIconTint = colorOnPrimarySL
                chip.chipIconTint = colorOnPrimarySL
            } else {
                chip.setTextColor(primaryColor)
                chip.closeIconTint = primaryColorSL
                chip.chipIconTint = primaryColorSL
                chip.chipStrokeColor = primaryColorSL
            }
        }
        if (showIcon && icon != null) {
            chip.setChipIconResource(icon)
            chip.chipDrawable.alpha = iconAlpha
        }
    }

    @DrawableRes
    private fun getIcon(index: Int, def: Int) = getIconResId(index) ?: def

    private fun getColor(theme: Int): ThemeColor? {
        if (theme != 0) {
            val color = colorProvider.getThemeColor(theme, true)
            if (color.isFree || inventory.purchasedThemes()) {
                return color
            }
        }
        return null
    }

    init {
        setStyle(preferences.getIntegerFromString(R.string.p_chip_style, 0))
        setAppearance(preferences.getIntegerFromString(R.string.p_chip_appearance, 0))
    }
}