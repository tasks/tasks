package org.tasks.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.entity.Task
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.dialogs.BaseDateTimePicker.OnDismissHandler
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import javax.inject.Inject

@AndroidEntryPoint
class WidgetClickActivity : AppCompatActivity(), OnDismissHandler {
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        if (action.isNullOrEmpty()) {
            return
        }
        when (action) {
            COMPLETE_TASK -> {
                lifecycleScope.launch(NonCancellable) {
                    taskCompleter.setComplete(task, !task.isCompleted)
                }
                finish()
            }
            EDIT_TASK -> {
                startActivity(
                        TaskIntents.getEditTaskIntent(
                                this,
                                intent.getParcelableExtra(EXTRA_FILTER),
                                intent.getParcelableExtra(EXTRA_TASK)))
                finish()
            }
            TOGGLE_SUBTASKS -> {
                lifecycleScope.launch(NonCancellable) {
                    taskDao.setCollapsed(task.id, intent.getBooleanExtra(EXTRA_COLLAPSED, false))
                }
                finish()
            }
            RESCHEDULE_TASK -> {
                val fragmentManager = supportFragmentManager
                if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_TIME_PICKER) == null) {
                    newDateTimePicker(
                            preferences.getBoolean(R.string.p_auto_dismiss_datetime_widget, false),
                            task)
                            .show(fragmentManager, FRAG_TAG_DATE_TIME_PICKER)
                }
            }
            TOGGLE_GROUP -> {
                val widgetPreferences = WidgetPreferences(
                        applicationContext,
                        preferences,
                        intent.getIntExtra(EXTRA_WIDGET, -1)
                )
                val collapsed = widgetPreferences.collapsed.toMutableSet()
                val group = intent.getLongExtra(EXTRA_GROUP, -1)
                if (intent.getBooleanExtra(EXTRA_COLLAPSED, false)) {
                    collapsed.add(group)
                } else {
                    collapsed.remove(group)
                }
                widgetPreferences.collapsed = collapsed
                localBroadcastManager.broadcastRefresh()
                finish()
            }
        }
    }

    val task: Task
        get() = intent.getParcelableExtra(EXTRA_TASK)!!

    override fun onDismiss() {
        finish()
    }

    companion object {
        const val COMPLETE_TASK = "COMPLETE_TASK"
        const val EDIT_TASK = "EDIT_TASK"
        const val TOGGLE_SUBTASKS = "TOGGLE_SUBTASKS"
        const val RESCHEDULE_TASK = "RESCHEDULE_TASK"
        const val TOGGLE_GROUP = "TOGGLE_GROUP"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_TASK = "extra_task" // $NON-NLS-1$
        const val EXTRA_COLLAPSED = "extra_collapsed"
        const val EXTRA_GROUP = "extra_group"
        const val EXTRA_WIDGET = "extra_widget"
        private const val FRAG_TAG_DATE_TIME_PICKER = "frag_tag_date_time_picker"
    }
}