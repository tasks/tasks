package org.tasks.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.todoroo.astrid.activity.MainActivityViewModel
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.compose.edit.SubtaskRow
import org.tasks.data.entity.Task
import org.tasks.filters.SubtaskFilter
import org.tasks.preferences.Preferences
import org.tasks.tasklist.SectionedDataSource
import org.tasks.tasklist.TasksResults
import org.tasks.themes.ColorProvider
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@AndroidEntryPoint
class SubtaskControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var checkBoxProvider: CheckBoxProvider
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var firebase: Firebase

    private lateinit var listViewModel: TaskListViewModel
    private val mainViewModel: MainActivityViewModel by activityViewModels()

    override fun createView(savedInstanceState: Bundle?) {
        viewModel.viewState.value.task.takeIf { it.id > 0 }?.let {
            listViewModel.setFilter(SubtaskFilter(it.id))
        }
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            listViewModel = ViewModelProvider(requireParentFragment())[TaskListViewModel::class.java]
            setContent {
                val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
                val originalState = viewModel.originalState.collectAsStateWithLifecycle().value
                SubtaskRow(
                    originalFilter = originalState.list,
                    filter = viewState.list,
                    hasParent = viewState.hasParent,
                    existingSubtasks = if (viewModel.viewState.collectAsStateWithLifecycle().value.isNew) {
                        TasksResults.Results(SectionedDataSource())
                    } else {
                        listViewModel.state.collectAsStateWithLifecycle().value.tasks
                    },
                    newSubtasks = viewState.newSubtasks,
                    openSubtask = this@SubtaskControlSet::openSubtask,
                    completeExistingSubtask = this@SubtaskControlSet::complete,
                    toggleSubtask = this@SubtaskControlSet::toggleSubtask,
                    addSubtask = {
                        lifecycleScope.launch {
                            viewModel.setSubtasks(
                                viewState.newSubtasks.plus(taskCreator.createWithValues(""))
                            )
                        }
                    },
                    completeNewSubtask = {
                        viewModel.setSubtasks(
                            viewState.newSubtasks.toMutableList().apply {
                                val modified = it.copy(
                                    completionDate = if (it.isCompleted) 0 else currentTimeMillis()
                                )
                                set(indexOf(it), modified)
                            }
                        )
                    },
                    deleteSubtask = { viewModel.setSubtasks(viewState.newSubtasks - it) },
                )
            }
        }

    override fun controlId() = TAG

    private fun openSubtask(task: Task) = lifecycleScope.launch {
        mainViewModel.setTask(task)
    }

    private fun toggleSubtask(taskId: Long, collapsed: Boolean) = lifecycleScope.launch {
        taskDao.setCollapsed(taskId, collapsed)
    }

    private fun complete(task: Task, completed: Boolean) = lifecycleScope.launch {
        taskCompleter.setComplete(task, completed)
        firebase.completeTask("edit_screen_subtask")
    }

    companion object {
        val TAG = R.string.TEA_ctrl_subtask_pref
    }
}
