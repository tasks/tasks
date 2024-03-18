package org.tasks.widget

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.todoroo.astrid.core.SortHelper
import com.todoroo.astrid.service.Upgrader.Companion.getLegacyColor
import org.tasks.R
import org.tasks.extensions.Context.isNightMode
import org.tasks.preferences.Preferences
import org.tasks.preferences.QueryPreferences
import org.tasks.tasklist.SectionedDataSource.Companion.HEADER_COMPLETED
import timber.log.Timber

class WidgetPreferences(
    private val context: Context,
    private val preferences: Preferences,
    private val widgetId: Int
) : QueryPreferences {

    data class WidgetHeaderSettings(
        val showHeader: Boolean,
        val showTitle: Boolean,
        val showSettings: Boolean,
        val showMenu: Boolean,
        val color: Int,
        val backgroundColor: Int,
        val headerOpacity: Int,
        val headerSpacing: Int,
        val isDark: Boolean,
    )

    data class WidgetRowSettings(
        val showFullTaskTitle: Boolean,
        val showCheckboxes: Boolean,
        val showDescription: Boolean,
        val showFullDescription: Boolean,
        val showDividers: Boolean,
        val showSubtaskChips: Boolean,
        val showStartChips: Boolean,
        val showPlaceChips: Boolean,
        val showListChips: Boolean,
        val showTagChips: Boolean,
        val vPad: Int,
        val textSize: Float,
        val showFullDate: Boolean,
        val compact: Boolean,
        val groupMode: Int,
        val dueDatePosition: Int,
        val isDark: Boolean,
    ) {
        val showDueDates get() = dueDatePosition != 2
        val endDueDate get() = dueDatePosition != 1
    }

    fun getWidgetHeaderSettings() = WidgetHeaderSettings(
        showHeader = getBoolean(R.string.p_widget_show_header, true),
        showTitle = getBoolean(R.string.p_widget_show_title, true),
        showSettings = getBoolean(R.string.p_widget_show_settings, true),
        showMenu = getBoolean(R.string.p_widget_show_menu, true),
        color = color,
        backgroundColor = backgroundColor,
        headerOpacity = getAlphaValue(R.string.p_widget_header_opacity),
        headerSpacing = getSpacing(R.string.p_widget_header_spacing),
        isDark = isDark,
    )

    fun getWidgetListSettings() = WidgetRowSettings(
        showFullTaskTitle = getBoolean(R.string.p_widget_show_full_task_title, false),
        showCheckboxes = getBoolean(R.string.p_widget_show_checkboxes, true),
        showDescription = getBoolean(R.string.p_widget_show_description, true),
        showFullDescription = getBoolean(R.string.p_widget_show_full_description, false),
        showDividers = getBoolean(R.string.p_widget_show_dividers, true),
        showSubtaskChips = getBoolean(R.string.p_widget_show_subtasks, true),
        showStartChips = getBoolean(R.string.p_widget_show_start_dates, true),
        showPlaceChips = getBoolean(R.string.p_widget_show_places, true),
        showListChips = getBoolean(R.string.p_widget_show_lists, true),
        showTagChips = getBoolean(R.string.p_widget_show_tags, true),
        vPad = getSpacing(R.string.p_widget_spacing),
        textSize = getInt(R.string.p_widget_font_size, 16).toFloat(),
        showFullDate = preferences.alwaysDisplayFullDate,
        compact = getBoolean(R.string.p_widget_compact, false),
        groupMode = groupMode,
        dueDatePosition = dueDatePosition,
        isDark = isDark,
    )

    val dueDatePosition: Int get() = getIntegerFromString(R.string.p_widget_due_date_position)

    private val isDark: Boolean
        get() = when (themeIndex) {
            0 -> false
            3, 4 -> context.isNightMode
            else -> true
        }

    private val backgroundColor: Int
        get() = context.getColor(
            when (themeIndex) {
                1 -> android.R.color.black
                2 -> R.color.md_background_dark
                3, 4 -> R.color.widget_background_follow_system
                else -> android.R.color.white
            }
        )

    var collapsed: Set<Long>
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
    private fun getSpacing(pref: Int): Int {
        val spacing = getIntegerFromString(pref)
        if (spacing == 2) {
            return 0
        }
        val dimen = if (spacing == 1) R.dimen.widget_padding_compact else R.dimen.widget_padding
        return context.resources.getDimension(dimen).toInt()
    }

    val filterId: String?
        get() = getString(R.string.p_widget_filter)
    val themeIndex: Int
        get() = getInt(R.string.p_widget_theme, 3)
    val color: Int
        get() {
            if (themeIndex == 4) {
                return ContextCompat.getColor(
                    context,
                    if (isDark) {
                        com.google.android.material.R.color.m3_sys_color_dynamic_dark_primary
                    } else {
                        com.google.android.material.R.color.m3_sys_color_dynamic_light_primary
                    }
                )
            }
            var color = getInt(R.string.p_widget_color_v2, 0)
            if (color != 0) {
                return color
            }
            val index = getInt(R.string.p_widget_color, -1)
            color = context.getColor(getLegacyColor(index, R.color.blue_500))
            setInt(R.string.p_widget_color_v2, color)
            return color
        }
    fun setColor(color: Int) {
        setInt(R.string.p_widget_color_v2, color)
    }
    val footerOpacity: Int
        get() = getAlphaValue(R.string.p_widget_footer_opacity)
    val rowOpacity: Int
        get() = getAlphaValue(R.string.p_widget_opacity)

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
        collapsed = setOf(HEADER_COMPLETED)
        preferences.setString(getKey(R.string.p_widget_filter), filterPreferenceValue)
    }

    fun setCompact(compact: Boolean) {
        setBoolean(R.string.p_widget_compact, compact)
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
    override val alwaysDisplayFullDate: Boolean
        get() = preferences.alwaysDisplayFullDate
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
