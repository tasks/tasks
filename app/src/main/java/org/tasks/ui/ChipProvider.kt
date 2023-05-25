package org.tasks.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.compose.Chip
import org.tasks.compose.FilterChip
import org.tasks.compose.SubtaskChip
import org.tasks.data.Place
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.filters.PlaceFilter
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils.startOfDay
import java.time.format.FormatStyle
import java.util.Locale
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
    private fun StartDateChip(
        sortGroup: Long?,
        startDate: Long,
        compact: Boolean,
        timeOnly: Boolean
    ) {
        val text by remember(sortGroup, startDate, timeOnly, compact) {
            derivedStateOf {
                if (
                    timeOnly &&
                    sortGroup?.startOfDay() == startDate.startOfDay()
                ) {
                    startDate
                        .takeIf { Task.hasDueTime(it) }
                        ?.let { DateUtilities.getTimeString(activity, it.toDateTime()) }
                } else {
                    DateUtilities.getRelativeDateTime(
                        activity,
                        startDate,
                        locale,
                        if (compact) FormatStyle.SHORT else FormatStyle.MEDIUM,
                        false,
                        false
                    )
                }
            }
        }
        if (text != null) {
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
    }

    @Composable
    fun Chips(
        filter: Filter?,
        id: Long,
        children: Int,
        collapsed: Boolean,
        isHidden: Boolean,
        sortGroup: Long?,
        startDate: Long,
        place: Place?,
        tagsString: String?,
        sortByStartDate: Boolean,
        sortByList: Boolean,
        list: String?,
        isSubtask: Boolean,
        isGoogleTask: Boolean,
        toggleSubtasks: (Long, Boolean) -> Unit,
        onClick: (Filter) -> Unit,
    ) {
        if (children > 0 && remember { preferences.showSubtaskChip }) {
            SubtaskChip(
                collapsed = collapsed,
                children = children,
                compact = !showText,
                onClick = { toggleSubtasks(id, !collapsed) }
            )
        }
        if (isHidden && remember { preferences.showStartDateChip }) {
            StartDateChip(sortGroup, startDate, !showText, sortByStartDate)
        }
        if (place != null && filter !is PlaceFilter && remember { preferences.showPlaceChip }) {
            FilterChip(
                filter = PlaceFilter(place),
                defaultIcon = R.drawable.ic_outline_place_24px,
                onClick = onClick,
                showText = showText,
                showIcon = showIcon,
                colorProvider = this::getColor,
            )
        }

        if (
            !isSubtask &&
            !sortByList &&
            preferences.showListChip &&
            filter !is CaldavFilter &&
            filter !is GtasksFilter
        ) {
            remember(list, isGoogleTask) {
                lists
                    .getCaldavList(list)
                    ?.let { if (isGoogleTask) GtasksFilter(it) else CaldavFilter(it) }
            }?.let {
                FilterChip(
                    filter = it,
                    defaultIcon = R.drawable.ic_list_24px,
                    onClick = onClick,
                    showText = showText,
                    showIcon = showIcon,
                    colorProvider = this::getColor,
                )
            }
        }
        if (!tagsString.isNullOrBlank() && remember { preferences.showTagChip }) {
            remember(tagsString, filter) {
                val tags = tagsString.split(",").toHashSet()
                if (filter is TagFilter) {
                    tags.remove(filter.uuid)
                }
                tags.mapNotNull(lists::getTag)
                    .sortedBy(TagFilter::listingTitle)
            }
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
