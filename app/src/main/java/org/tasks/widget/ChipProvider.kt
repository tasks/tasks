package org.tasks.widget

import android.content.Context
import android.widget.RemoteViews
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.TagFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.TaskContainer
import org.tasks.filters.PlaceFilter
import org.tasks.locale.Locale
import org.tasks.preferences.Preferences
import org.tasks.themes.CustomIcons
import org.tasks.ui.ChipListCache
import java.time.format.FormatStyle
import javax.inject.Inject

class ChipProvider @Inject constructor(
        @ApplicationContext private val context: Context,
        private val chipListCache: ChipListCache,
        private val locale: Locale,
        private val preferences: Preferences,
) {

    var isDark = false

    fun getSubtaskChip(task: TaskContainer): RemoteViews {
        val chip = newChip()
        chip.setTextViewText(
                R.id.chip_text,
                context
                        .resources
                        .getQuantityString(R.plurals.subtask_count, task.children, task.children)
        )
        chip.setImageViewResource(
                R.id.chip_icon,
                if (task.isCollapsed) {
                    R.drawable.ic_keyboard_arrow_down_black_24dp
                } else {
                    R.drawable.ic_keyboard_arrow_up_black_24dp
                }
        )
        return chip
    }

    fun getStartDateChip(task: TaskContainer, showFullDate: Boolean): RemoteViews? {
        return if (task.isHidden) {
            val chip = newChip()
            chip.setTextViewText(
                    R.id.chip_text,
                    DateUtilities.getRelativeDateTime(
                            context,
                            task.startDate,
                            locale.locale,
                            FormatStyle.MEDIUM,
                            showFullDate,
                            false
                    )
            )
            chip.setImageViewResource(R.id.chip_icon, R.drawable.ic_pending_actions_24px)
            chip
        } else {
            null
        }
    }

    fun getListChip(filter: Filter?, task: TaskContainer): RemoteViews? {
        task.googleTaskList
                ?.takeIf { filter !is GtasksFilter }
                ?.let { newChip(chipListCache.getGoogleTaskList(it), R.drawable.ic_list_24px) }
                ?.let { return it }
        task.caldav
                ?.takeIf { filter !is CaldavFilter }
                ?.let { newChip(chipListCache.getCaldavList(it), R.drawable.ic_list_24px) }
                ?.let { return it }
        return null
    }

    fun getPlaceChip(filter: Filter?, task: TaskContainer): RemoteViews? {
        task.location
                ?.takeIf { filter !is PlaceFilter || it.place != filter.place}
                ?.let { return newChip(PlaceFilter(it.place), R.drawable.ic_outline_place_24px) }
        return null
    }

    fun getTagChips(filter: Filter?, task: TaskContainer): List<RemoteViews> {
        val tags = task.tags?.split(",")?.toHashSet() ?: return emptyList()
        if (filter is TagFilter) {
            tags.remove(filter.uuid)
        }
        return tags
                .mapNotNull(chipListCache::getTag)
                .sortedBy(TagFilter::listingTitle)
                .mapNotNull { newChip(it, R.drawable.ic_outline_label_24px) }
    }

    private fun newChip(filter: Filter?, defaultIcon: Int): RemoteViews? {
        if (filter == null) {
            return null
        }
        val chip = newChip()
        chip.setTextViewText(R.id.chip_text, filter.listingTitle)
        val icon = filter.icon
                .takeIf { it >= 0 }
                ?.let { CustomIcons.getIconResId(it) }
                ?: defaultIcon
        chip.setImageViewResource(R.id.chip_icon, icon)
        if (filter.tint != 0) {
            chip.setInt(R.id.chip_background, "setColorFilter", filter.tint)
            chip.setTextColor(R.id.chip_text, filter.tint)
            chip.setInt(R.id.chip_icon, "setColorFilter", filter.tint)
        }
        return chip
    }

    private fun newChip() = RemoteViews(
            BuildConfig.APPLICATION_ID,
            if (isDark) R.layout.widget_chip_dark else R.layout.widget_chip_light
    )
}