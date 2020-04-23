package com.todoroo.astrid.core

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.tasks.Callback2

class CustomFilterItemTouchHelper(private val onMove: Callback2<Int, Int>, private val onClear: Runnable) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder.adapterPosition > 0) makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) else 0
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            (viewHolder as CriterionViewHolder).setMoving(true);
        }
    }

    override fun onMove(
            recyclerView: RecyclerView, src: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val toPosition = target.adapterPosition
        if (toPosition == 0) {
            return false
        }
        onMove.call(src.adapterPosition, toPosition);
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        (viewHolder as CriterionViewHolder).setMoving(false)

        onClear.run()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}