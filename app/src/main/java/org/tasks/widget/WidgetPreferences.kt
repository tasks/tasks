package org.tasks.widget

import android.content.Context
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.service.Upgrader.Companion.getLegacyColor
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences
import timber.log.Timber

class WidgetPreferences(
    private val context: Context,
    private val preferences: Preferences,
    private val widgetId: Int
) : QueryPreferences {
    fun showHeader(): Boolean {
        return getBoolean(R.string.p_widget_show_header, true)
    }

    fun showTitle(): Boolean {
        return getBoolean(R.string.p_widget_show_title, true)
    }

    fun showCheckboxes(): Boolean {
        return getBoolean(R.string.p_widget_show_checkboxes, true)
    }

    fun showSettings(): Boolean {
        return getBoolean(R.string.p_widget_show_settings, true)
    }

    fun showMenu(): Boolean {
        return getBoolean(R.string.p_widget_show_menu, true)
    }

    fun showFullTaskTitle(): Boolean {
        return getBoolean(R.string.p_widget_show_full_task_title, false)
    }

    fun showDescription(): Boolean {
        return getBoolean(R.string.p_widget_show_description, true)
    }

    fun showFullDescription(): Boolean {
        return getBoolean(R.string.p_widget_show_full_description, false)
    }

    fun showDividers(): Boolean {
        return getBoolean(R.string.p_widget_show_dividers, true)
    }

    fun showSubtasks(): Boolean {
        return getBoolean(R.string.p_widget_show_subtasks, true)
    }

    fun showStartDates(): Boolean {
        return getBoolean(R.string.p_widget_show_start_dates, true)
    }

    fun showPlaces(): Boolean {
        return getBoolean(R.string.p_widget_show_places, true)
    }

    fun showLists(): Boolean {
        return getBoolean(R.string.p_widget_show_lists, true)
    }

    fun showTags(): Boolean {
        return getBoolean(R.string.p_widget_show_tags, true)
    }

    val dueDatePosition: Int
        get() = getIntegerFromString(R.string.p_widget_due_date_position)
    var collapsed: MutableSet<Long>
        get() {
            val value = getString(R.string.p_widget_collapsed)
            val collapsed = HashSet<Long>()
            if (value?.isNotBlank() == true) {
                for (entry in Splitter.on(",").split(value)) {
                    try {
                        collapsed.add(entry.toLong())
                    } catch (e: NumberFormatException) {
                        Timber.e(e)
                    }
                }
            }
            return collapsed
        }
        set(collapsed) {
            setString(R.string.p_widget_collapsed, Joiner.on(",").join(collapsed))
        }
    val widgetSpacing: Int
        get() = getSpacing(R.string.p_widget_spacing)
    val headerSpacing: Int
        get() = getSpacing(R.string.p_widget_header_spacing)
    val headerLayout: Int
        get() = when (getIntegerFromString(R.string.p_widget_header_spacing)) {
            1 -> R.layout.widget_title_compact
            2 -> R.layout.widget_title_none
            else -> R.layout.widget_title_default
        }

    private fun getSpacing(pref: Int): Int {
        val spacing = getIntegerFromString(pref)
        if (spacing == 2) {
            return 0
        }
        val dimen = if (spacing == 1) R.dimen.widget_padding_compact else R.dimen.widget_padding
        return context.resources.getDimension(dimen).toInt()
    }

    val fontSize: Int
        get() = getInt(R.string.p_widget_font_size, 16)
    val filterId: String?
        get() = getString(R.string.p_widget_filter)
    val themeIndex: Int
        get() = getInt(R.string.p_widget_theme, 3)
    var color: Int
        get() {
            var color = getInt(R.string.p_widget_color_v2, 0)
            if (color != 0) {
                return color
            }
            val index = getInt(R.string.p_widget_color, -1)
            color = context.getColor(getLegacyColor(index, R.color.blue_500))
            setInt(R.string.p_widget_color_v2, color)
            return color
        }
        set(color) {
            setInt(R.string.p_widget_color_v2, color)
        }
    val headerOpacity: Int
        get() = getAlphaValue(R.string.p_widget_header_opacity)
    val footerOpacity: Int
        get() = getAlphaValue(R.string.p_widget_footer_opacity)
    val rowOpacity: Int
        get() = getAlphaValue(R.string.p_widget_opacity)

    fun openOnFooterClick(): Boolean {
        return getIntegerFromString(R.string.p_widget_footer_click) == 1
    }

    fun rescheduleOnDueDateClick(): Boolean {
        return getIntegerFromString(R.string.p_widget_due_date_click) == 0
    }

    private fun getAlphaValue(resId: Int): Int {
        return (getInt(resId, 100) / 100.0 * 255.0).toInt()
    }

    fun setTheme(index: Int) {
        setInt(R.string.p_widget_theme, index)
    }

    fun getKey(resId: Int): String {
        return context.getString(resId) + widgetId
    }

    fun setFilter(filterPreferenceValue: String?) {
        collapsed = HashSet()
        preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue)
    }

    var compact: Boolean
        get() = getBoolean(R.string.p_widget_compact, false)
        set(value) {
            setBoolean(R.string.p_widget_compact, value)
        }

    private fun getInt(resId: Int, defValue: Int): Int {
        return preferences.getInt(getKey(resId), defValue)
    }

    private fun getIntegerFromString(resId: Int): Int {
        return preferences.getIntegerFromString(getKey(resId), 0)
    }

    private fun getBoolean(resId: Int, defValue: Boolean): Boolean {
        return preferences.getBoolean(getKey(resId), defValue)
    }

    private fun getString(resId: Int): String? {
        return preferences.getStringValue(getKey(resId))
    }

    private fun setInt(resId: Int, value: Int) {
        preferences.setInt(getKey(resId), value)
    }

    private fun setBoolean(resId: Int, value: Boolean) {
        preferences.setBoolean(getKey(resId), value)
    }

    private fun setString(resId: Int, value: String) {
        preferences.setString(getKey(resId), value)
    }

    fun maintainExistingConfiguration() {
        val rowOpacity = getInt(R.string.p_widget_opacity, 100)
        setInt(R.string.p_widget_header_opacity, rowOpacity)
        setInt(R.string.p_widget_footer_opacity, rowOpacity)
        val showDueDate = getBoolean(R.string.p_widget_show_due_date, true)
        setString(
            R.string.p_widget_due_date_position,
            if (showDueDate) "1" else "2"
        ) // below or hidden
        setBoolean(R.string.p_widget_show_dividers, false) // no dividers
        setBoolean(R.string.p_widget_show_menu, false) // no menu
        setString(R.string.p_widget_spacing, "1") // compact
        setBoolean(R.string.p_widget_show_description, false) // no description
    }

    override var sortMode: Int
        get() = getInt(R.string.p_widget_sort, SortHelper.SORT_AUTO)
        set(sortMode) {
            setInt(R.string.p_widget_sort, sortMode)
        }
    override var groupMode: Int
        get() = getInt(R.string.p_widget_group, SortHelper.GROUP_NONE)
        set(groupMode) {
            setInt(R.string.p_widget_group, groupMode)
        }
    override var isManualSort: Boolean
        get() = getBoolean(R.string.p_widget_sort_manual, false)
        set(isManualSort) {
            setBoolean(R.string.p_widget_sort_manual, isManualSort)
        }
    override var isAstridSort: Boolean
        get() = getBoolean(R.string.p_widget_sort_astrid, false)
        set(isAstridSort) {
            setBoolean(R.string.p_widget_sort_astrid, isAstridSort)
        }
    override var sortAscending: Boolean
        get() = getBoolean(R.string.p_widget_sort_ascending, true)
        set(ascending) {
            setBoolean(R.string.p_widget_sort_ascending, ascending)
        }
    override var groupAscending: Boolean
        get() = getBoolean(R.string.p_widget_group_ascending, false)
        set(ascending) {
            setBoolean(R.string.p_widget_group_ascending, ascending)
        }
    override val showHidden: Boolean
        get() = getBoolean(R.string.p_widget_show_hidden, true)
    override val showCompleted: Boolean
        get() = getBoolean(R.string.p_widget_show_completed, false)
    override var alwaysDisplayFullDate: Boolean
        get() = preferences.alwaysDisplayFullDate
        set(noWeekday) {
            preferences.alwaysDisplayFullDate = noWeekday
        }
    override var completedTasksAtBottom: Boolean
        get() = preferences.completedTasksAtBottom
        set(value) {
            preferences.setBoolean(R.string.p_completed_tasks_at_bottom, value)
        }
    override var completedMode: Int
        get() = preferences.completedMode
        set(mode) {
            preferences.completedMode = mode
        }
    override var completedAscending: Boolean
        get() = preferences.completedAscending
        set(ascending) {
            preferences.completedAscending = ascending
        }
    override var subtaskMode: Int
        get() = preferences.subtaskMode
        set(mode) {
            preferences.subtaskMode = mode
        }
    override var subtaskAscending: Boolean
        get() = preferences.subtaskAscending
        set(ascending) {
            preferences.subtaskAscending = ascending
        }
}
