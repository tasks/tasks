package com.todoroo.astrid.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.tasks.R

class CustomFilterItemTouchHelper(
        private val context: Context,
        private val onMove: (Int, Int) -> Unit,
        private val onDelete: (Int) -> Unit,
        private val onClear: () -> Unit) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder.adapterPosition > 0) makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) else 0
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            (viewHolder as CriterionViewHolder).setMoving(true)
        }
    }

    override fun onMove(
            recyclerView: RecyclerView, src: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val toPosition = target.adapterPosition
        if (toPosition == 0) {
            return false
        }
        onMove(src.adapterPosition, toPosition)
        return true
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 3
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return .6f
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView
        val background = ColorDrawable(context.getColor(R.color.overdue))
        if (dX > 0) {
            background.setBounds(0, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
        } else if (dX < 0) {
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }
        background.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        (viewHolder as CriterionViewHolder).setMoving(false)

        onClear()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onDelete(viewHolder.adapterPosition)
    }
}