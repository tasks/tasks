package org.tasks.ui

import android.app.Activity
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.data.Task
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.billing.Inventory
import org.tasks.data.TagData
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.filters.PlaceFilter
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import org.tasks.themes.CustomIcons
import org.tasks.themes.CustomIcons.getIconResId
import org.tasks.themes.ThemeColor
import org.tasks.time.DateTimeUtils.startOfDay
import java.time.format.FormatStyle
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
                locale.locale,
                if (compact) FormatStyle.SHORT else FormatStyle.MEDIUM,
                false,
                false
            )
        }
        TasksChip(
            R.drawable.ic_pending_actions_24px,
            text,
            0,
            showText = true,
            showIcon = true,
            onClick = {},
        )
    }

    @Composable
    fun SubtaskChip(
        task: TaskContainer,
        compact: Boolean,
        onClick: () -> Unit,
    ) {
        TasksChip(
            if (task.isCollapsed) R.drawable.ic_keyboard_arrow_down_black_24dp else R.drawable.ic_keyboard_arrow_up_black_24dp,
            if (compact) locale.formatNumber(task.children) else activity
                .resources
                .getQuantityString(R.plurals.subtask_count, task.children, task.children),
            0,
            showText = true,
            showIcon = true,
            onClick = onClick,
        )
    }

    @Composable
    fun FilterChip(
        filter: Filter,
        defaultIcon: Int,
        showText: Boolean = this@ChipProvider.showText,
        showIcon: Boolean = this@ChipProvider.showIcon,
        onClick: (Any) -> Unit,
    ) {
        TasksChip(
            getIcon(filter.icon, defaultIcon),
            filter.listingTitle,
            filter.tint,
            showText,
            showIcon,
            onClick = { onClick(filter) },
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
            FilterChip(
                filter = PlaceFilter(location.place),
                defaultIcon = R.drawable.ic_outline_place_24px,
                onClick = onClick
            )
        }
        if (!isSubtask && preferences.showListChip) {
            if (!isNullOrEmpty(task.googleTaskList) && filter !is GtasksFilter) {
                lists.getGoogleTaskList(task.googleTaskList)?.let { list ->
                    FilterChip(
                        filter = list,
                        defaultIcon = R.drawable.ic_list_24px,
                        onClick = onClick
                    )
                }
            } else if (!isNullOrEmpty(task.caldav) && filter !is CaldavFilter) {
                lists.getCaldavList(task.caldav)?.let { list ->
                    FilterChip(
                        filter = list,
                        defaultIcon = R.drawable.ic_list_24px,
                        onClick = onClick
                    )
                }
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
                        onClick = onClick
                    )
                }
        }
    }

    @Composable
    fun TagChip(tag: TagData, onClick: () -> Unit) {
        TasksChip(
            getIcon(tag.getIcon()!!, R.drawable.ic_outline_label_24px),
            tag.name,
            tag.getColor()!!,
            showText = true,
            showIcon = true,
            onClick = onClick,
        )
    }

    @Composable
    fun TasksChip(
        @DrawableRes icon: Int?,
        name: String?,
        theme: Int,
        showText: Boolean,
        showIcon: Boolean,
        onClick: () -> Unit,
    ) {
        val color =
            getColor(theme)?.primaryColor ?: activity.getColor(R.color.default_chip_background)
        TasksChip(
            color = Color(color),
            text = if (showText) name else null,
            icon = if (showIcon && icon != null) icon else null,
            onClick = onClick,
        )
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
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TasksChip(
    text: String? = null,
    icon: Int? = null,
    color: Color,
    onClick: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalMinimumTouchTargetEnforcement provides false
    ) {
        Chip(
            onClick = onClick,
            border = BorderStroke(1.dp, color = color),
            leadingIcon = {
                if (text != null) {
                    ChipIcon(iconRes = icon)
                }
            },
            modifier = Modifier.defaultMinSize(minHeight = 26.dp),
            colors = ChipDefaults.chipColors(
                backgroundColor = color.copy(alpha = .1f),
                contentColor = MaterialTheme.colors.onSurface
            ),
        ) {
            if (text == null) {
                ChipIcon(iconRes = icon)
            }
            text?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ChipIcon(iconRes: Int?) {
    iconRes?.let {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipIconAndTextPreview() {
    MdcTheme {
        TasksChip(
            text = "Home",
            icon = getIconResId(CustomIcons.LABEL),
            color = Color.Red,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipIconPreview() {
    MdcTheme {
        TasksChip(
            text = null,
            icon = getIconResId(CustomIcons.LABEL),
            color = Color.Red,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TasksChipTextPreview() {
    MdcTheme {
        TasksChip(
            text = "Home",
            icon = null,
            color = Color.Red,
        )
    }
}
