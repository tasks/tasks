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
import org.tasks.compose.edit.SubtaskRow
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.Task
import org.tasks.filters.SubtaskFilter
import org.tasks.preferences.Preferences
import org.tasks.tasklist.SectionedDataSource
import org.tasks.themes.ColorProvider
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import javax.inject.Inject

@AndroidEntryPoint
class SubtaskControlSet : TaskEditControlFragment() {
    @Inject lateinit var activity: Activity
    @Inject lateinit var taskCompleter: TaskCompleter
    @Inject lateinit var googleTaskDao: GoogleTaskDao
    @Inject lateinit var taskCreator: TaskCreator
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var checkBoxProvider: CheckBoxProvider
    @Inject lateinit var chipProvider: ChipProvider
    @Inject lateinit var colorProvider: ColorProvider
    @Inject lateinit var preferences: Preferences

    private lateinit var listViewModel: TaskListViewModel
    private val mainViewModel: MainActivityViewModel by activityViewModels()

    override fun createView(savedInstanceState: Bundle?) {
        viewModel.task.takeIf { it.id > 0 }?.let {
            listViewModel.setFilter(SubtaskFilter(it.id))
        }
    }

    override fun bind(parent: ViewGroup?): View =
        (parent as ComposeView).apply {
            listViewModel = ViewModelProvider(requireParentFragment())[TaskListViewModel::class.java]
            setContent {
                TasksTheme {
                    SubtaskRow(
                        originalFilter = viewModel.originalList,
                        filter = viewModel.selectedList.collectAsStateWithLifecycle().value,
                        hasParent = viewModel.hasParent,
                        desaturate = preferences.desaturateDarkMode,
                        existingSubtasks = if (viewModel.isNew) {
                            TaskListViewModel.TasksResults.Results(SectionedDataSource())
                        } else {
                            listViewModel.state.collectAsStateWithLifecycle().value.tasks
                        },
                        newSubtasks = viewModel.newSubtasks.collectAsStateWithLifecycle().value,
                        openSubtask = this@SubtaskControlSet::openSubtask,
                        completeExistingSubtask = this@SubtaskControlSet::complete,
                        toggleSubtask = this@SubtaskControlSet::toggleSubtask,
                        addSubtask = this@SubtaskControlSet::addSubtask,
                        completeNewSubtask = {
                            viewModel.newSubtasks.value =
                                ArrayList(viewModel.newSubtasks.value).apply {
                                    val modified = it.copy(
                                        completionDate = if (it.isCompleted) 0 else currentTimeMillis()
                                    )
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

    private fun addSubtask() = lifecycleScope.launch {
        val task = taskCreator.createWithValues("")
        viewModel.newSubtasks.value = viewModel.newSubtasks.value.plus(task)
    }

    private fun openSubtask(task: Task) = lifecycleScope.launch {
        mainViewModel.setTask(task)
    }

    private fun toggleSubtask(taskId: Long, collapsed: Boolean) = lifecycleScope.launch {
        taskDao.setCollapsed(taskId, collapsed)
    }

    private fun complete(task: Task, completed: Boolean) = lifecycleScope.launch {
        taskCompleter.setComplete(task, completed)
    }

    companion object {
        val TAG = R.string.TEA_ctrl_subtask_pref
    }
}
