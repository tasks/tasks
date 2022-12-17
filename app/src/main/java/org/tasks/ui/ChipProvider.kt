package org.tasks.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.compose.Chip
import org.tasks.compose.FilterChip
import org.tasks.compose.SubtaskChip
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.filters.PlaceFilter
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils.startOfDay
import java.time.format.FormatStyle
import java.util.*
import javax.inject.Inject

class ChipProvider @Inject constructor(
    private val activity: Activity,
    private val inventory: Inventory,
    private val lists: ChipListCache,
    private val preferences: Preferences,
    private val colorProvider: ColorProvider,
    private val locale: Locale
) {
    private val showIcon: Boolean
    private val showText: Boolean

    init {
        val appearance = preferences.getIntegerFromString(R.string.p_chip_appearance, 0)
        showText = appearance != 2
        showIcon = appearance != 1
    }

    @Composable
    private fun StartDateChip(task: TaskContainer, compact: Boolean, timeOnly: Boolean) {
        val text = if (timeOnly
            && task.sortGroup?.startOfDay() == task.startDate.startOfDay()
            && preferences.showGroupHeaders()
        ) {
            task.startDate
                .takeIf { Task.hasDueTime(it) }
                ?.let { DateUtilities.getTimeString(activity, it.toDateTime()) }
                ?: return
        } else {
            DateUtilities.getRelativeDateTime(
                activity,
                task.startDate,
                locale,
                if (compact) FormatStyle.SHORT else FormatStyle.MEDIUM,
                false,
                false
            )
        }
        Chip(
            R.drawable.ic_pending_actions_24px,
            text,
            0,
            showText = true,
            showIcon = true,
            onClick = {},
            colorProvider = this::getColor,
        )
    }

    @Composable
    fun Chips(
        filter: Filter?,
        isSubtask: Boolean,
        task: TaskContainer,
        sortByStartDate: Boolean,
        onClick: (Any) -> Unit,
    ) {
        if (task.hasChildren() && preferences.showSubtaskChip) {
            SubtaskChip(task, !showText, onClick = { onClick(task) })
        }
        if (task.isHidden && preferences.showStartDateChip) {
            StartDateChip(task, !showText, sortByStartDate)
        }
        if (task.hasLocation() && filter !is PlaceFilter && preferences.showPlaceChip) {
            val location = task.getLocation()
            if (location != null) {
                FilterChip(
                    filter = PlaceFilter(location.place),
                    defaultIcon = R.drawable.ic_outline_place_24px,
                    onClick = onClick,
                    showText = showText,
                    showIcon = showIcon,
                    colorProvider = this::getColor,
                )
            }
        }
        if (!isSubtask && preferences.showListChip && filter !is CaldavFilter) {
            lists.getCaldavList(task.caldav)?.let { list ->
                FilterChip(
                    filter = if (task.isGoogleTask) GtasksFilter(list) else CaldavFilter(list),
                    defaultIcon = R.drawable.ic_list_24px,
                    onClick = onClick,
                    showText = showText,
                    showIcon = showIcon,
                    colorProvider = this::getColor,
                )
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
                .forEach {
                    FilterChip(
                        filter = it,
                        defaultIcon = R.drawable.ic_outline_label_24px,
                        onClick = onClick,
                        showText = showText,
                        showIcon = showIcon,
                        colorProvider = this::getColor,
                    )
                }
        }
    }

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
