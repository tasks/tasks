package org.tasks.tasklist

import androidx.recyclerview.widget.DiffUtil
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.core.SortHelper.SORT_START
import timber.log.Timber

internal class DiffCallback(
    private val old: SectionedDataSource,
    private val new: SectionedDataSource,
    @Deprecated("") private val adapter: TaskAdapter
) : DiffUtil.Callback() {

    private val refreshDates = when (old.groupMode) {
        SORT_DUE -> new.groupMode == SORT_START
        SORT_START -> new.groupMode == SORT_DUE
        else -> false
    }

    override fun getOldListSize() = old.size

    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val wasHeader = old.isHeader(oldPosition)
        val isHeader = new.isHeader(newPosition)
        if (wasHeader != isHeader) {
            return false
        }
        return try {
            if (isHeader) {
                old.groupMode == new.groupMode && old.getHeaderValue(oldPosition) == new.getHeaderValue(newPosition)
            } else {
                old.getItem(oldPosition).id == new.getItem(newPosition).id
            }
        } catch (e: IndexOutOfBoundsException) {
            Timber.w(e, "areItemsTheSame: old=$oldPosition/${old.size} new=$newPosition/${new.size}")
            false
        }
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return try {
            if (new.isHeader(newPosition)) {
                old.getSection(oldPosition).collapsed == new.getSection(newPosition).collapsed
            } else {
                val oldItem = old.getItem(oldPosition)
                val newItem = new.getItem(newPosition)
                !refreshDates && oldItem == newItem && oldItem.indent == adapter.getIndent(newItem)
            }
        } catch (e: IndexOutOfBoundsException) {
            Timber.w(e, "areContentsTheSame: old=$oldPosition/${old.size} new=$newPosition/${new.size}")
            false
        }
    }
}