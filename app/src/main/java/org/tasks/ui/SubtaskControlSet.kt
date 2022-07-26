package org.tasks.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration.Companion.LineThrough
import androidx.compose.ui.text.style.TextDecoration.Companion.None
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.todoroo.andlib.sql.Criterion
import com.todoroo.andlib.sql.Join
import com.todoroo.andlib.sql.QueryTemplate
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.compose.*
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.data.TaskContainer
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

    @Composable
    override fun Body() {
        Column {
            val filter = viewModel.selectedList.collectAsStateLifecycleAware().value
            val googleTask = googleTaskDao.watchGoogleTask(viewModel.task.id)
                .collectAsStateLifecycleAware(initial = null).value
            val isGoogleTaskChild =
                filter is GtasksFilter && googleTask != null && googleTask.parent > 0 && googleTask.listId == filter.remoteId
            if (isGoogleTaskChild) {
                DisabledText(
                    text = stringResource(id = R.string.subtasks_multilevel_google_task),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                val subtasks = listViewModel.tasks.observeAsState(initial = emptyList()).value
                val newSubtasks = viewModel.newSubtasks.collectAsStateLifecycleAware().value
                Spacer(modifier = Modifier.height(height = 8.dp))
                ExistingSubtasks(subtasks = subtasks, multiLevelSubtasks = filter !is GtasksFilter)
                NewSubtasks(
                    subtasks = newSubtasks,
                    onComplete = {
                        val copy = ArrayList(viewModel.newSubtasks.value)
                        copy[copy.indexOf(it)] =
                            it.clone().apply { completionDate = if (isCompleted) 0 else now() }
                        viewModel.newSubtasks.value = copy
                    },
                    onDelete = {
                        val copy = ArrayList(viewModel.newSubtasks.value)
                        copy.remove(it)
                        viewModel.newSubtasks.value = copy
                    }
                )
                DisabledText(
                    text = stringResource(id = R.string.TEA_add_subtask),
                    modifier = Modifier
                        .clickable { addSubtask() }
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    override val icon = R.drawable.ic_subdirectory_arrow_right_black_24dp

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

    @Composable
    override fun Icon() {
        TaskEditIcon(
            id = icon,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
                .alpha(ContentAlpha.medium),
        )
    }

    @Composable
    fun NewSubtasks(
        subtasks: List<Task>,
        onComplete: (Task) -> Unit,
        onDelete: (Task) -> Unit,
    ) {
        subtasks.forEach { subtask ->
            NewSubtaskRow(
                subtask = subtask,
                onComplete = onComplete,
                onDelete = onDelete,
            )
        }
    }

    @Composable
    fun ExistingSubtasks(subtasks: List<TaskContainer>, multiLevelSubtasks: Boolean) {
        subtasks.forEach { task ->
            SubtaskRow(
                task = task,
                indent = if (multiLevelSubtasks) task.indent else 0,
                onRowClick = { openSubtask(task.task) },
                onCompleteClick = { complete(task.task, !task.isCompleted) },
                onToggleSubtaskClick = { toggleSubtask(task.id, !task.isCollapsed) }
            )
        }
    }

    @Composable
    fun NewSubtaskRow(
        subtask: Task,
        onComplete: (Task) -> Unit,
        onDelete: (Task) -> Unit,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckBox(
                task = subtask,
                onCompleteClick = { onComplete(subtask) },
                modifier = Modifier.align(Alignment.Top),
                desaturate = preferences.desaturateDarkMode,
            )
            var text by remember { mutableStateOf(subtask.title ?: "") }
            val focusRequester = remember { FocusRequester() }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    subtask.title = it
                },
                cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
                modifier = Modifier
                    .weight(1f)
                    .focusable(enabled = true)
                    .focusRequester(focusRequester)
                    .alpha(if (subtask.isCompleted) ContentAlpha.disabled else ContentAlpha.high),
                textStyle = MaterialTheme.typography.body1.copy(
                    textDecoration = if (subtask.isCompleted) LineThrough else None,
                    color = MaterialTheme.colors.onSurface,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            addSubtask()
                        }
                    }
                ),
                singleLine = true,
                maxLines = Int.MAX_VALUE,
            )
            ClearButton { onDelete(subtask) }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }

    @Composable
    fun SubtaskRow(
        task: TaskContainer, indent: Int,
        onRowClick: () -> Unit,
        onCompleteClick: () -> Unit,
        onToggleSubtaskClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onRowClick() }
                .padding(end = 16.dp)
        ) {
            Spacer(modifier = Modifier.width((indent * 20).dp))
            CheckBox(
                task = task.task,
                onCompleteClick = onCompleteClick,
                desaturate = preferences.desaturateDarkMode
            )
            Text(
                text = task.title,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (task.isCompleted || task.isHidden) ContentAlpha.disabled else ContentAlpha.high),
                style = MaterialTheme.typography.body1.copy(
                    textDecoration = if (task.isCompleted) LineThrough else None
                )
            )
            if (task.hasChildren()) {
                SubtaskChip(
                    task = task,
                    compact = true,
                    onClick = onToggleSubtaskClick,
                )
            }
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

