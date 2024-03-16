package org.tasks.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import com.todoroo.astrid.data.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.extensions.setColorFilter
import org.tasks.filters.PlaceFilter
import org.tasks.themes.CustomIcons
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.ui.ChipListCache
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

class WidgetChipProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chipListCache: ChipListCache,
    private val locale: Locale,
) {
    var isDark = false

    fun getSubtaskChip(task: TaskContainer): RemoteViews {
        return newChip().apply {
            setTextViewText(
                R.id.chip_text,
                context
                    .resources
                    .getQuantityString(R.plurals.subtask_count, task.children, task.children)
            )
            setImageViewResource(
                R.id.chip_icon,
                if (task.isCollapsed) {
                    R.drawable.ic_keyboard_arrow_down_black_24dp
                } else {
                    R.drawable.ic_keyboard_arrow_up_black_24dp
                }
            )
        }
    }

    fun getStartDateChip(task: TaskContainer, showFullDate: Boolean, sortByStartDate: Boolean): RemoteViews? {
        return if (task.isHidden) {
            val time = if (sortByStartDate && task.sortGroup?.startOfDay() == task.task.hideUntil.startOfDay()) {
                task.task.hideUntil
                    .takeIf { Task.hasDueTime(it) }
                    ?.let { DateUtilities.getTimeString(context, it.toDateTime()) }
                    ?: return null
            } else {
                DateUtilities.getRelativeDateTime(
                    context,
                    task.task.hideUntil,
                    locale,
                    FormatStyle.MEDIUM,
                    showFullDate,
                    false
                )
            }
            newChip().apply {
                setTextViewText(R.id.chip_text, time)
                setImageViewResource(R.id.chip_icon, R.drawable.ic_pending_actions_24px)
            }
        } else {
            null
        }
    }

    fun getListChip(filter: Filter, task: TaskContainer): RemoteViews? {
        return task.caldav
                ?.takeIf { filter !is CaldavFilter && filter !is GtasksFilter }
                ?.let { chipListCache.getCaldavList(it) }
                ?.let {
                    newChip(
                        filter = if (task.isGoogleTask) GtasksFilter(it) else CaldavFilter(it),
                        defaultIcon = R.drawable.ic_list_24px
                    )
                }
    }

    fun getPlaceChip(filter: Filter, task: TaskContainer): RemoteViews? {
        task.location
                ?.takeIf { filter !is PlaceFilter || it.place != filter.place}
                ?.let { return newChip(PlaceFilter(it.place), R.drawable.ic_outline_place_24px) }
        return null
    }

    fun getTagChips(filter: Filter, task: TaskContainer): List<RemoteViews> {
        val tags = task.tagsString?.split(",")?.toHashSet() ?: return emptyList()
        if (filter is TagFilter) {
            tags.remove(filter.uuid)
        }
        return tags
                .mapNotNull(chipListCache::getTag)
                .sortedBy(TagFilter::title)
                .map { newChip(it, R.drawable.ic_outline_label_24px) }
    }

    private fun newChip(filter: Filter, defaultIcon: Int) =
        newChip(filter.tint).apply {
            setTextViewText(R.id.chip_text, filter.title)
            val icon = filter.icon
                .takeIf { it >= 0 }
                ?.let { CustomIcons.getIconResId(it) }
                ?: defaultIcon
            setImageViewResource(R.id.chip_icon, icon)
        }

    private fun newChip(@ColorInt color: Int = 0) = RemoteViews(BuildConfig.APPLICATION_ID, R.layout.widget_chip).apply {
        val tint = if (color == 0) {
            context.getColor(
                if (isDark) R.color.icon_tint_dark_alpha else R.color.icon_tint_light_alpha
            )
        } else {
            color
        }
        setColorFilter(R.id.chip_icon, tint)
        setColorFilter(R.id.chip_background, tint)
        setTextColor(R.id.chip_text, tint)
    }
}