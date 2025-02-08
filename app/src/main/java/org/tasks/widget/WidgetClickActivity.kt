package org.tasks.widget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.MainActivity.Companion.FINISH_AFFINITY
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.data.entity.Task
import org.tasks.dialogs.BaseDateTimePicker.OnDismissHandler
import org.tasks.dialogs.DateTimePicker.Companion.newDateTimePicker
import org.tasks.filters.Filter
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WidgetClickActivity : AppCompatActivity(), OnDismissHandler {
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var firebase: Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        if (action.isNullOrEmpty()) {
            return
        }
        when (action) {
            COMPLETE_TASK -> {
                val task = task
                Timber.tag("$action task=$task")
                lifecycleScope.launch(NonCancellable) {
                    taskCompleter.setComplete(task, !task.isCompleted)
                    firebase.completeTask("widget")
                }
                finish()
            }
            EDIT_TASK -> {
                val filter = intent.getParcelableExtra<Filter?>(EXTRA_FILTER)
                val task = task
                Timber.tag("$action task=$task filter=$filter")
                startActivity(
                    TaskIntents
                        .getEditTaskIntent(this, filter, task)
                        .putExtra(FINISH_AFFINITY, true)
                )
                finish()
            }
            TOGGLE_SUBTASKS -> {
                val task = task
                val collapsed = intent.getBooleanExtra(EXTRA_COLLAPSED, false)
                Timber.d("$action collapsed=$collapsed task=$task")
                lifecycleScope.launch(NonCancellable) {
                    taskDao.setCollapsed(task.id, collapsed)
                }
                finish()
            }
            RESCHEDULE_TASK -> {
                val task = task
                Timber.d("$action task=$task")
                val fragmentManager = supportFragmentManager
                if (fragmentManager.findFragmentByTag(FRAG_TAG_DATE_TIME_PICKER) == null) {
                    newDateTimePicker(
                            preferences.getBoolean(R.string.p_auto_dismiss_datetime_widget, false),
                            task)
                            .show(fragmentManager, FRAG_TAG_DATE_TIME_PICKER)
                }
            }
            TOGGLE_GROUP -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET, -1)
                val group = intent.getLongExtra(EXTRA_GROUP, -1)
                val setCollapsed = intent.getBooleanExtra(EXTRA_COLLAPSED, false)
                Timber.d("$action widgetId=$widgetId group=$group collapsed=$setCollapsed")
                val widgetPreferences =
                    WidgetPreferences(applicationContext, preferences, widgetId)
                val collapsed = widgetPreferences.collapsed.toMutableSet()
                if (setCollapsed) {
                    collapsed.add(group)
                } else {
                    collapsed.remove(group)
                }
                widgetPreferences.collapsed = collapsed
                localBroadcastManager.broadcastRefresh()
                finish()
            }
            else -> {
                Timber.e("Unknown action $action")
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
