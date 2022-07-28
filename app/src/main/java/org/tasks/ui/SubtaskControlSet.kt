package org.tasks.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.compose.edit.SubtaskRow
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskDao.TaskCriteria.activeAndVisible
import org.tasks.preferences.Preferences
import org.tasks.themes.ColorProvider
import javax.inject.Inject

@AndroidEntryPoint
class SubtaskControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var checkBoxProvider: CheckBoxProvider
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var eventBus: MainActivityEventBus
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var preferences: Preferences

    private val listViewModel: TaskListViewModel by viewModels()
    private val refreshReceiver = RefreshReceiver()

    override fun createView(savedInstanceState: Bundle?) {
        viewModel.task.takeIf { it.id > 0 }?.let {
            listViewModel.setFilter(Filter("subtasks", getQueryTemplate(it)))
        }
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            setContent {
                MdcTheme {
                    SubtaskRow(
                        filter = viewModel.selectedList.collectAsStateLifecycleAware().value,
                        googleTask = googleTaskDao.watchGoogleTask(viewModel.task.id).collectAsStateLifecycleAware(initial = null).value,
                        desaturate = preferences.desaturateDarkMode,
                        existingSubtasks = listViewModel.tasks.observeAsState(initial = emptyList()).value,
                        newSubtasks = viewModel.newSubtasks.collectAsStateLifecycleAware().value,
                        openSubtask = this@SubtaskControlSet::openSubtask,
                        completeExistingSubtask = this@SubtaskControlSet::complete,
                        toggleSubtask = this@SubtaskControlSet::toggleSubtask,
                        addSubtask = this@SubtaskControlSet::addSubtask,
                        completeNewSubtask = {
                            viewModel.newSubtasks.value =
                                ArrayList(viewModel.newSubtasks.value).apply {
                                    val modified = it.clone().apply {
                                        completionDate =
                                            if (isCompleted) 0 else now()
                                    }
                                    set(indexOf(it), modified)
                                }
                        },
                        deleteSubtask = {
                            viewModel.newSubtasks.value =
                                ArrayList(viewModel.newSubtasks.value).apply {
                                    remove(it)
                                }
                        }
                    )
                }
            }
        }

    override fun controlId() = TAG

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerRefreshReceiver(refreshReceiver)
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    private fun addSubtask() = lifecycleScope.launch {
        val task = taskCreator.createWithValues("")
        viewModel.newSubtasks.value = viewModel.newSubtasks.value.plus(task)
    }

    private fun openSubtask(task: Task) = lifecycleScope.launch {
        eventBus.emit(MainActivityEvent.OpenTask(task))
    }

    private fun toggleSubtask(taskId: Long, collapsed: Boolean) = lifecycleScope.launch {
        taskDao.setCollapsed(taskId, collapsed)
        localBroadcastManager.broadcastRefresh()
    }

    private fun complete(task: Task, completed: Boolean) = lifecycleScope.launch {
        taskCompleter.setComplete(task, completed)
    }

    private inner class RefreshReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            listViewModel.invalidate()
        }
    }

    companion object {
        const val TAG = R.string.TEA_ctrl_subtask_pref
        private fun getQueryTemplate(task: Task): QueryTemplate = QueryTemplate()
            .join(
                Join.left(
                    GoogleTask.TABLE,
                    Criterion.and(
                        GoogleTask.PARENT.eq(task.id),
                        GoogleTask.TASK.eq(Task.ID),
                        GoogleTask.DELETED.eq(0)
                    )
                )
            )
            .where(
                Criterion.and(
                    activeAndVisible(),
                    Criterion.or(Task.PARENT.eq(task.id), GoogleTask.TASK.gt(0))
                )
            )
    }
}
