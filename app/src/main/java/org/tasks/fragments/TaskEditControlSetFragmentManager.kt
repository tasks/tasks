package org.tasks.fragments

import android.content.Context
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.LocationControlSet
import org.tasks.ui.SubtaskControlSet
import javax.inject.Inject

class TaskEditControlSetFragmentManager @Inject constructor(
    @ActivityContext context: Context,
    preferences: Preferences?
) {
    val controlSetFragments: MutableMap<String, Int> = LinkedHashMap()
    val displayOrder: List<String>
    var visibleSize = 0

    init {
        displayOrder = BeastModePreferences.constructOrderedControlList(preferences, context)
        val hideAlwaysTrigger = context.getString(R.string.TEA_ctrl_hide_section_pref)
        visibleSize = 0
        while (visibleSize < displayOrder.size) {
            if (displayOrder[visibleSize] == hideAlwaysTrigger) {
                displayOrder.removeAt(visibleSize)
                break
            }
            visibleSize++
        }
        for (resId in TASK_EDIT_CONTROL_SET_FRAGMENTS) {
            controlSetFragments[context.getString(resId)] = resId
        }
    }

    companion object {
        val TAG_DESCRIPTION = R.string.TEA_ctrl_notes_pref
        val TAG_CREATION = R.string.TEA_ctrl_creation_date
        val TAG_LIST = R.string.TEA_ctrl_google_task_list
        val TAG_PRIORITY = R.string.TEA_ctrl_importance_pref
        val TAG_DUE_DATE = R.string.TEA_ctrl_when_pref

        private val TASK_EDIT_CONTROL_SET_FRAGMENTS = intArrayOf(
            TAG_DUE_DATE,
            TimerControlSet.TAG,
            TAG_DESCRIPTION,
            CalendarControlSet.TAG,
            TAG_PRIORITY,
            StartDateControlSet.TAG,
            ReminderControlSet.TAG,
            LocationControlSet.TAG,
            FilesControlSet.TAG,
            TagsControlSet.TAG,
            RepeatControlSet.TAG,
            TAG_CREATION,
            TAG_LIST,
            SubtaskControlSet.TAG
        )
    }
}