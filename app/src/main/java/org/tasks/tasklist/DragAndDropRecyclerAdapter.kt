package org.tasks.tasklist

import android.graphics.Canvas
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.Callback
import androidx.recyclerview.widget.ItemTouchHelper.Callback.makeMovementFlags
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.adapter.TaskAdapter
import com.todoroo.astrid.utility.Flags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.tasks.activities.DragAndDropDiffer
import org.tasks.data.TaskContainer
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskListViewModel.UiItem
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class DragAndDropRecyclerAdapter(
    private val adapter: TaskAdapter,
    private val recyclerView: RecyclerView,
    viewHolderFactory: ViewHolderFactory,
    private val taskList: TaskListFragment,
    tasks: SectionedDataSource,
    preferences: Preferences,
    private val toggleCollapsed: (Long) -> Unit,
) : TaskListRecyclerAdapter(adapter, viewHolderFactory, taskList, preferences), DragAndDropDiffer<UiItem, SectionedDataSource> {
    private val itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback()).apply {
        attachToRecyclerView(recyclerView)
    }
    override val channel = Channel<SectionedDataSource>(Channel.UNLIMITED)
    override val updates: Queue<Pair<SectionedDataSource, DiffUtil.DiffResult?>> = LinkedList()
    override var dragging = false
    override val scope: CoroutineScope =
            CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + Job())
    override var items = initializeDiffer(tasks)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        if (viewType == 1) {
            val headerSection = items.getSection(position)
            (holder as HeaderViewHolder).bind(taskList.getFilter(), preferences.groupMode, headerSection)
        } else {
            super.onBindViewHolder(holder, position)
        }
    }

    override val sortMode: Int
        get() = items.groupMode

    override val subtaskSortMode: Int
        get() = items.subtaskMode

    override fun getItemViewType(position: Int) = if (items.isHeader(position)) 1 else 0

    override fun submitList(list: SectionedDataSource) {
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = if (viewType == 1) {
        viewHolderFactory.newHeaderViewHolder(parent, this::toggleGroup)
    } else {
        super.onCreateViewHolder(parent, viewType)
    }

    private fun toggleGroup(group: Long) {
        toggleCollapsed(group)
    }

    override fun dragAndDropEnabled() = taskList.getFilter().supportsSubtasks()

    override fun isHeader(position: Int): Boolean = items.isHeader(position)

    override fun nearestHeader(position: Int) = items.getNearestHeader(position)

    override fun getItem(position: Int) = items.getItem(position)

    override fun diff(last: SectionedDataSource, next: SectionedDataSource) =
        DiffUtil.calculateDiff(DiffCallback(last, next, adapter), next.size < LONG_LIST_SIZE)

    override fun drainQueue() {
        val recyclerViewState = recyclerView.layoutManager!!.onSaveInstanceState()
        super.drainQueue()
        recyclerView.layoutManager!!.onRestoreInstanceState(recyclerViewState)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.dispose()
        itemTouchHelper.attachToRecyclerView(null)
    }

    override fun getTaskCount() = items.taskCount

    override fun getItemCount() = items.size

    private inner class ItemTouchHelperCallback : Callback() {
        private var from = -1
        private var to = -1
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ACTION_STATE_DRAG) {
                taskList.startActionMode()
                (viewHolder as TaskViewHolder?)!!.moving = true
                dragging = true
                val position = viewHolder!!.bindingAdapterPosition
                updateIndents(viewHolder as TaskViewHolder?, position, position)
            }
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return when {
                !dragAndDropEnabled() -> NO_MOVEMENT
                adapter.isHeader(viewHolder.bindingAdapterPosition) -> NO_MOVEMENT
                adapter.numSelected > 0 -> NO_MOVEMENT
                else -> ALLOW_DRAGGING
            }
        }  

        override fun onMove(
                recyclerView: RecyclerView,
                src: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder): Boolean {
            taskList.finishActionMode()
            val fromPosition = src.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            val source = src as TaskViewHolder
            val isHeader = isHeader(toPosition)
            if (!isHeader && !adapter.canMove(source.task, fromPosition, (target as TaskViewHolder).task, toPosition)) {
                return false
            }
            if (from == -1) {
                source.selected = false
                from = fromPosition
            }
            to = toPosition
            notifyItemMoved(fromPosition, toPosition)
            if (isHeader) {
                val offset = if (fromPosition < toPosition) -1 else 1
                items.moveSection(toPosition, offset)
            }
            updateIndents(source, from, to)
            return true
        }

        private fun updateIndents(source: TaskViewHolder?, from: Int, to: Int) {
            val task = source!!.task
            val previousIsHeader = to > 0 && isHeader(to - 1)
            source.minIndent = if (to == 0 || to == itemCount - 1 || previousIsHeader) 0 else adapter.minIndent(if (from <= to) to + 1 else to, task)
            source.maxIndent = if (to == 0 || previousIsHeader) 0 else adapter.maxIndent(if (from >= to) to - 1 else to, task)
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
            val vh = viewHolder as TaskViewHolder
            val task = vh.task
            val shiftSize = vh.shiftSize
            if (actionState == ACTION_STATE_DRAG) {
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
                    if (targetIndent != task.indent) {
                        if (from == -1) {
                            taskList.finishActionMode()
                            vh.selected = false
                        }
                    }
                    if (targetIndent < minIndent) {
                        task.targetIndent = minIndent
                    } else task.targetIndent = min(targetIndent, maxIndent)
                }
                dX = (task.targetIndent - task.indent) * shiftSize
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            val vh = viewHolder as TaskViewHolder
            vh.moving = false
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
                    vh.task.indent = targetIndent
                    vh.task.targetIndent = targetIndent
                    vh.indent = targetIndent
                    moved(from, to, targetIndent)
                } else if (task.indent != targetIndent) {
                    val position = vh.bindingAdapterPosition
                    vh.task.indent = targetIndent
                    vh.task.targetIndent = targetIndent
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

        private fun moved(fromOrig: Int, to: Int, indent: Int) {
            val from = if (fromOrig == to) {
                to
            } else if (fromOrig > to && isHeader(fromOrig)) {
                from - 1
            } else {
                from
            }
            runBlocking {
                // too much state change happens here, need to rewrite a bunch of stuff
                adapter.moved(from, to, indent)
            }
            val task: TaskContainer = items.removeAt(from)
            items.add(if (from < to) to - 1 else to, task)
            taskList.loadTaskListContent()
        }
    }

    companion object {
        private const val LONG_LIST_SIZE = 500
        private val NO_MOVEMENT = makeMovementFlags(0, 0)
        private val ALLOW_DRAGGING =  makeMovementFlags(UP or DOWN or LEFT or RIGHT, 0)
    }
}