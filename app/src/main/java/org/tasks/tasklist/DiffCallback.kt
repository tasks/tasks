package org.tasks.tasklist

import androidx.recyclerview.widget.DiffUtil
import com.todoroo.astrid.adapter.TaskAdapter
import org.tasks.data.TaskContainer

internal class DiffCallback(private val old: List<TaskContainer>, private val new: List<TaskContainer>, @Deprecated("") private val adapter: TaskAdapter) : DiffUtil.Callback() {

    override fun getOldListSize() = old.size

    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return old[oldPosition].id == new[newPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val oldItem = old[oldPosition]
        val newItem = new[newPosition]
        return oldItem == newItem && oldItem.getIndent() == adapter.getIndent(newItem)
    }
}