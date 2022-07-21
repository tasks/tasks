package org.tasks.fragments

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.ui.*
import javax.inject.Inject

class TaskEditControlSetFragmentManager @Inject constructor(
        @ActivityContext context: Context,
        preferences: Preferences?) {

    private val controlSetFragments: MutableMap<String, Int> = LinkedHashMap()
    private val displayOrder: List<String>
    var visibleSize = 0

    fun getOrCreateFragments(fragmentManager: FragmentManager): List<TaskEditControlFragment> {
        val fragments: MutableList<TaskEditControlFragment> = ArrayList()
        for (i in displayOrder.indices) {
            val tag = displayOrder[i]
            var fragment = fragmentManager.findFragmentByTag(tag) as TaskEditControlFragment?
            if (fragment == null) {
                val resId = controlSetFragments[tag]
                fragment = createFragment(resId!!)
            }
            fragments.add(fragment)
        }
        return fragments
    }

    private fun createFragment(fragmentId: Int): TaskEditControlFragment = when (fragmentId) {
        DeadlineControlSet.TAG -> DeadlineControlSet()
        PriorityControlSet.TAG -> PriorityControlSet()
        DescriptionControlSet.TAG -> DescriptionControlSet()
        CalendarControlSet.TAG -> CalendarControlSet()
        StartDateControlSet.TAG -> StartDateControlSet()
        ReminderControlSet.TAG -> ReminderControlSet()
        LocationControlSet.TAG -> LocationControlSet()
        FilesControlSet.TAG -> FilesControlSet()
        TimerControlSet.TAG -> TimerControlSet()
        TagsControlSet.TAG -> TagsControlSet()
        RepeatControlSet.TAG -> RepeatControlSet()
        ListFragment.TAG -> ListFragment()
        SubtaskControlSet.TAG -> SubtaskControlSet()
        CreationDateControlSet.TAG -> CreationDateControlSet()
        else -> throw RuntimeException("Unsupported fragment")
    }

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
        val TASK_EDIT_CONTROL_FRAGMENT_ROWS = intArrayOf(
                R.id.row_1,
                R.id.row_2,
                R.id.row_3,
                R.id.row_4,
                R.id.row_5,
                R.id.row_6,
                R.id.row_7,
                R.id.row_8,
                R.id.row_9,
                R.id.row_10,
                R.id.row_11,
                R.id.row_12,
                R.id.row_13,
                R.id.row_14
        )
        private val TASK_EDIT_CONTROL_SET_FRAGMENTS = intArrayOf(
                DeadlineControlSet.TAG,
                TimerControlSet.TAG,
                DescriptionControlSet.TAG,
                CalendarControlSet.TAG,
                PriorityControlSet.TAG,
                StartDateControlSet.TAG,
                ReminderControlSet.TAG,
                LocationControlSet.TAG,
                FilesControlSet.TAG,
                TagsControlSet.TAG,
                RepeatControlSet.TAG,
                CreationDateControlSet.TAG,
                ListFragment.TAG,
                SubtaskControlSet.TAG
        )

        init {
            if (BuildConfig.DEBUG
                    && TASK_EDIT_CONTROL_FRAGMENT_ROWS.size != TASK_EDIT_CONTROL_SET_FRAGMENTS.size) {
                throw AssertionError()
            }
        }
    }
}