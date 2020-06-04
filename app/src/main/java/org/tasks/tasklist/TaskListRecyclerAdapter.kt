package org.tasks.tasklist

import android.view.ViewGroup
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.adapter.TaskAdapterDataSource
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.dao.TaskDao
import org.tasks.data.TaskContainer
import org.tasks.intents.TaskIntents
import org.tasks.preferences.Preferences
import org.tasks.tasklist.TaskViewHolder.ViewHolderCallbacks

abstract class TaskListRecyclerAdapter internal constructor(
        private val adapter: TaskAdapter,
        internal val viewHolderFactory: ViewHolderFactory,
        private val taskList: TaskListFragment,
        private val taskDao: TaskDao,
        internal val preferences: Preferences)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ViewHolderCallbacks, ListUpdateCallback, TaskAdapterDataSource {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = viewHolderFactory.newViewHolder(parent, this)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sortMode = preferences.sortMode
        val task = getItem(position)
        if (task != null) {
            (holder as TaskViewHolder).bindView(task, taskList.getFilter(), sortMode)
            holder.moving = false
            val indent = adapter.getIndent(task)
            task.setIndent(indent)
            holder.indent = indent
            holder.selected = adapter.isSelected(task)
        }
    }

    override fun onCompletedTask(task: TaskContainer, newState: Boolean) {
        adapter.onCompletedTask(task, newState)
        taskList.loadTaskListContent()
    }

    override fun onClick(taskViewHolder: TaskViewHolder) {
        if (taskList.isActionModeActive) {
            toggle(taskViewHolder)
        } else {
            taskList.onTaskListItemClicked(taskViewHolder.task.getTask())
        }
    }

    override fun onClick(filter: Filter) {
        if (!taskList.isActionModeActive) {
            val context = taskList.activity
            context?.startActivity(TaskIntents.getTaskListIntent(context, filter))
        }
    }

    override fun onLongPress(taskViewHolder: TaskViewHolder): Boolean {
        if (!dragAndDropEnabled()) {
            taskList.startActionMode()
        }
        if (taskList.isActionModeActive && !taskViewHolder.moving) {
            toggle(taskViewHolder)
        }
        return true
    }

    override fun onChangeDueDate(task: TaskContainer) {
        taskList.showDateTimePicker(task)
    }

    override fun toggleSubtasks(task: TaskContainer, collapsed: Boolean) {
        taskDao.setCollapsed(task.id, collapsed)
        taskList.broadcastRefresh()
    }

    fun toggle(taskViewHolder: TaskViewHolder) {
        adapter.toggleSelection(taskViewHolder.task)
        notifyItemChanged(taskViewHolder.adapterPosition)
        if (adapter.getSelected().isEmpty()) {
            taskList.finishActionMode()
        } else {
            taskList.updateModeTitle()
        }
    }

    protected abstract fun dragAndDropEnabled(): Boolean

    abstract fun submitList(list: List<TaskContainer>)

    override fun onInserted(position: Int, count: Int) {
        notifyItemRangeInserted(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        notifyItemRangeRemoved(position, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        notifyItemRangeChanged(position, count, payload)
    }
}