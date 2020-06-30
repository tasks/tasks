package org.tasks.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.HideUntilControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import dagger.hilt.android.qualifiers.ActivityContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.preferences.Preferences
import org.tasks.ui.*
import java.util.*
import javax.inject.Inject

class TaskEditControlSetFragmentManager @Inject constructor(
        @ActivityContext context: Context,
        preferences: Preferences?) {

    private val controlSetFragments: MutableMap<String, Int> = LinkedHashMap()
    private val displayOrder: List<String>
    var visibleSize = 0

    fun getFragmentsInPersistOrder(fragmentManager: FragmentManager): List<TaskEditControlFragment> {
        return controlSetFragments.keys
                .mapNotNull { fragmentManager.findFragmentByTag(it) as TaskEditControlFragment? }
    }

    fun getOrCreateFragments(
            taskEditFragment: TaskEditFragment, task: Task): List<TaskEditControlFragment> {
        val arguments = Bundle()
        arguments.putParcelable(TaskEditControlFragment.EXTRA_TASK, task)
        arguments.putBoolean(TaskEditControlFragment.EXTRA_IS_NEW, task.isNew)
        val fragments: MutableList<TaskEditControlFragment> = ArrayList()
        val fragmentManager = taskEditFragment.childFragmentManager
        for (i in displayOrder.indices) {
            val tag = displayOrder[i]
            var fragment = fragmentManager.findFragmentByTag(tag) as TaskEditControlFragment?
            if (fragment == null) {
                val resId = controlSetFragments[tag]
                fragment = createFragment(resId!!)
                fragment.arguments = arguments
            }
            fragments.add(fragment)
        }
        return fragments
    }

    private fun createFragment(fragmentId: Int): TaskEditControlFragment {
        return when (fragmentId) {
            DeadlineControlSet.TAG -> DeadlineControlSet()
            PriorityControlSet.TAG -> PriorityControlSet()
            DescriptionControlSet.TAG -> DescriptionControlSet()
            CalendarControlSet.TAG -> CalendarControlSet()
            HideUntilControlSet.TAG -> HideUntilControlSet()
            ReminderControlSet.TAG -> ReminderControlSet()
            LocationControlSet.TAG -> LocationControlSet()
            FilesControlSet.TAG -> FilesControlSet()
            TimerControlSet.TAG -> TimerControlSet()
            TagsControlSet.TAG -> TagsControlSet()
            RepeatControlSet.TAG -> RepeatControlSet()
            CommentBarFragment.TAG -> CommentBarFragment()
            ListFragment.TAG -> ListFragment()
            SubtaskControlSet.TAG -> SubtaskControlSet()
            else -> throw RuntimeException("Unsupported fragment")
        }
    }

    init {
        displayOrder = BeastModePreferences.constructOrderedControlList(preferences, context)
        displayOrder.add(0, context.getString(CommentBarFragment.TAG))
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
                R.id.comment_bar,
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
                R.id.row_13
        )
        private val TASK_EDIT_CONTROL_SET_FRAGMENTS = intArrayOf(
                DeadlineControlSet.TAG,
                TimerControlSet.TAG,
                DescriptionControlSet.TAG,
                CalendarControlSet.TAG,
                PriorityControlSet.TAG,
                HideUntilControlSet.TAG,
                ReminderControlSet.TAG,
                LocationControlSet.TAG,
                FilesControlSet.TAG,
                TagsControlSet.TAG,
                RepeatControlSet.TAG,
                CommentBarFragment.TAG,
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