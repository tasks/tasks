package org.tasks.tasklist

import androidx.recyclerview.widget.DiffUtil
import com.todoroo.astrid.adapter.TaskAdapter

internal class DiffCallback(private val old: SectionedDataSource, private val new: SectionedDataSource, @Deprecated("") private val adapter: TaskAdapter) : DiffUtil.Callback() {

    override fun getOldListSize() = old.size

    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val wasHeader = old.isHeader(oldPosition)
        val isHeader = new.isHeader(newPosition)
        if (wasHeader != isHeader) {
            return false
        }
        return if (isHeader) {
            old.sortMode == new.sortMode && old.getHeaderValue(oldPosition) == new.getHeaderValue(newPosition)
        } else {
            old.getItem(oldPosition).id == new.getItem(newPosition).id
        }
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        if (new.isHeader(newPosition)) {
            return old.getSection(oldPosition).collapsed == new.getSection(newPosition).collapsed
        }
        val oldItem = old.getItem(oldPosition)
        val newItem = new.getItem(newPosition)
        return oldItem == newItem && oldItem.getIndent() == adapter.getIndent(newItem)
    }
}