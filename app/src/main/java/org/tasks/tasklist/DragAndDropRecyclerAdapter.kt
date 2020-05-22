package org.tasks.tasklist

import android.graphics.Canvas
import androidx.core.util.Pair
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.utility.Flags
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.tasks.data.TaskContainer
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DragAndDropRecyclerAdapter(
        private val adapter: TaskAdapter,
        private val recyclerView: RecyclerView,
        viewHolderFactory: ViewHolderFactory,
        private val taskList: TaskListFragment,
        private var list: MutableList<TaskContainer>,
        taskDao: TaskDao) : TaskListRecyclerAdapter(adapter, viewHolderFactory, taskList, taskDao) {
    private val publishSubject = PublishSubject.create<List<TaskContainer>>()
    private val disposables = CompositeDisposable()
    private val updates: Queue<Pair<MutableList<TaskContainer>, DiffUtil.DiffResult>> = LinkedList()
    private var dragging = false
    override fun dragAndDropEnabled() = adapter.supportsParentingOrManualSort()

    override fun getItem(position: Int) = list[position]

    override fun submitList(list: List<TaskContainer>) = publishSubject.onNext(list)

    private fun calculateDiff(
            last: Pair<MutableList<TaskContainer>, DiffUtil.DiffResult>, next: MutableList<TaskContainer>): Pair<MutableList<TaskContainer>, DiffUtil.DiffResult> {
        AndroidUtilities.assertNotMainThread()
        val cb = DiffCallback(last.first, next, adapter)
        val result = DiffUtil.calculateDiff(cb, next.size < LONG_LIST_SIZE)
        return Pair.create(next, result)
    }

    private fun applyDiff(update: Pair<MutableList<TaskContainer>, DiffUtil.DiffResult>) {
        AndroidUtilities.assertMainThread()
        updates.add(update)
        if (!dragging) {
            drainQueue()
        }
    }

    private fun drainQueue() {
        AndroidUtilities.assertMainThread()
        val recyclerViewState = recyclerView.layoutManager!!.onSaveInstanceState()
        var update = updates.poll()
        while (update != null) {
            list = update.first!!
            update.second!!.dispatchUpdatesTo((this as ListUpdateCallback))
            update = updates.poll()
        }
        recyclerView.layoutManager!!.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) = disposables.dispose()

    override fun getItemCount(): Int {
        return list.size
    }

    private inner class ItemTouchHelperCallback : ItemTouchHelper.Callback() {
        private var from = -1
        private var to = -1
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                taskList.startActionMode()
                (viewHolder as ViewHolder?)!!.isMoving = true
                dragging = true
                val position = viewHolder!!.adapterPosition
                updateIndents(viewHolder as ViewHolder?, position, position)
            }
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) = if (adapter.supportsParentingOrManualSort() && adapter.numSelected == 0) {
            makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0)
        } else {
            makeMovementFlags(0, 0)
        }

        override fun onMove(
                recyclerView: RecyclerView,
                src: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder): Boolean {
            taskList.finishActionMode()
            val fromPosition = src.adapterPosition
            val toPosition = target.adapterPosition
            val source = src as ViewHolder
            if (!adapter.canMove(source.task, fromPosition, (target as ViewHolder).task, toPosition)) {
                return false
            }
            if (from == -1) {
                source.setSelected(false)
                from = fromPosition
            }
            to = toPosition
            notifyItemMoved(fromPosition, toPosition)
            updateIndents(source, from, to)
            return true
        }

        private fun updateIndents(source: ViewHolder?, from: Int, to: Int) {
            val task = source!!.task
            source.minIndent = if (to == 0 || to == itemCount - 1) 0 else adapter.minIndent(if (from <= to) to + 1 else to, task)
            source.maxIndent = if (to == 0) 0 else adapter.maxIndent(if (from >= to) to - 1 else to, task)
        }

        override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dXOrig: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean) {
            var dX = dXOrig
            val vh = viewHolder as ViewHolder
            val task = vh.task
            val shiftSize = vh.shiftSize
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                val currentIndent = viewHolder.indent
                val maxIndent = vh.maxIndent
                val minIndent = vh.minIndent
                if (isCurrentlyActive) {
                    val dxAdjusted: Float = if (dX > 0) {
                        min(dX, (maxIndent - currentIndent) * shiftSize)
                    } else {
                        max((currentIndent - minIndent) * -shiftSize, dX)
                    }
                    val targetIndent = currentIndent + java.lang.Float.valueOf(dxAdjusted / shiftSize).toInt()
                    if (targetIndent != task.getIndent()) {
                        if (from == -1) {
                            taskList.finishActionMode()
                            vh.setSelected(false)
                        }
                    }
                    if (targetIndent < minIndent) {
                        task.targetIndent = minIndent
                    } else task.targetIndent = Math.min(targetIndent, maxIndent)
                }
                dX = (task.targetIndent - task.getIndent()) * shiftSize
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val vh = viewHolder as ViewHolder
            vh.isMoving = false
            dragging = false
            drainQueue()
            if (taskList.isActionModeActive) {
                toggle(vh)
            } else {
                val task = vh.task
                val targetIndent = task.targetIndent
                if (from >= 0 && from != to) {
                    if (from < to) {
                        to++
                    }
                    vh.task.setIndent(targetIndent)
                    vh.indent = targetIndent
                    moved(from, to, targetIndent)
                } else if (task.getIndent() != targetIndent) {
                    val position = vh.adapterPosition
                    vh.task.setIndent(targetIndent)
                    vh.indent = targetIndent
                    moved(position, position, targetIndent)
                }
            }
            from = -1
            to = -1
            Flags.clear(Flags.TLFP_NO_INTERCEPT_TOUCH)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            throw UnsupportedOperationException()
        }

        private fun moved(from: Int, to: Int, indent: Int) {
            adapter.moved(from, to, indent)
            val task: TaskContainer = list.removeAt(from)
            list.add(if (from < to) to - 1 else to, task)
            taskList.loadTaskListContent()
        }
    }

    companion object {
        private const val LONG_LIST_SIZE = 500
    }

    init {
        ItemTouchHelper(ItemTouchHelperCallback()).attachToRecyclerView(recyclerView)
        val initial = Pair.create<MutableList<TaskContainer>, DiffUtil.DiffResult>(list, null)
        disposables.add(
                publishSubject
                        .observeOn(Schedulers.computation())
                        .scan(initial, { last: Pair<MutableList<TaskContainer>, DiffUtil.DiffResult>, next: List<TaskContainer> ->
                            calculateDiff(last, next.toMutableList())
                        })
                        .skip(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { update: Pair<MutableList<TaskContainer>, DiffUtil.DiffResult> -> applyDiff(update) })
    }
}