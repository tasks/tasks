package org.tasks.tasklist

import android.view.ViewGroup
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.adapter.TaskAdapterDataSource
import com.todoroo.astrid.api.AstridOrderingFilter
import com.todoroo.astrid.core.SortHelper
import org.tasks.data.TaskContainer
import org.tasks.preferences.Preferences

abstract class TaskListRecyclerAdapter internal constructor(
        private val adapter: TaskAdapter,
        internal val viewHolderFactory: ViewHolderFactory,
        private val taskList: TaskListFragment,
        internal val preferences: Preferences)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ListUpdateCallback, TaskAdapterDataSource {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
            = viewHolderFactory.newViewHolder(parent, taskList)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val filter = taskList.getFilter()
        val groupsEnabled = filter.supportsSorting()
                && !(filter.supportsManualSort() && preferences.isManualSort)
                && !(filter is AstridOrderingFilter && preferences.isAstridSort)
        val task = getItem(position)
        if (task != null) {
            (holder as TaskViewHolder)
                    .bindView(task, filter, if (groupsEnabled) preferences.groupMode else SortHelper.GROUP_NONE)
            holder.moving = false
            val indent = adapter.getIndent(task)
            task.indent = indent
            task.targetIndent = indent
            holder.indent = indent
            holder.selected = adapter.isSelected(task)
        }
    }

    fun toggle(taskViewHolder: TaskViewHolder) {
        adapter.toggleSelection(taskViewHolder.task)
        notifyItemChanged(taskViewHolder.bindingAdapterPosition)
        if (adapter.getSelected().isEmpty()) {
            taskList.finishActionMode()
        } else {
            taskList.updateModeTitle()
        }
    }

    abstract fun dragAndDropEnabled(): Boolean

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