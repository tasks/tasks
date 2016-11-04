package org.tasks.tasklist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> implements ViewHolder.ViewHolderCallbacks {

    private final Context context;
    private final TaskAdapter adapter;
    private final ViewHolderFactory viewHolderFactory;
    private final TaskListFragment taskList;
    private final ItemTouchHelper itemTouchHelper;

    public TaskListRecyclerAdapter(Context context, TaskAdapter adapter,
                                   ViewHolderFactory viewHolderFactory, TaskListFragment taskList) {
        this.context = context;
        this.adapter = adapter;
        this.viewHolderFactory = viewHolderFactory;
        this.taskList = taskList;
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
    }

    public void applyToRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(context)
                .inflate(R.layout.task_adapter_row_simple, parent, false);
        return viewHolderFactory.newViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Cursor cursor = adapter.getCursor();
        cursor.moveToPosition(position);
        holder.bindView((TodorooCursor<Task>) cursor);
        holder.setIndent(adapter.getIndent(holder.task));
    }

    @Override
    public int getItemCount() {
        return adapter.getCount();
    }

    @Override
    public void onCompletedTask(Task task, boolean newState) {
        adapter.onCompletedTask(task, newState);
    }

    @Override
    public void onClick(ViewHolder viewHolder) {
        Task task = viewHolder.task;
        if (!task.isDeleted()) {
            taskList.onTaskListItemClicked(task.getId());
        }
    }

    private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private int from = -1;
        private int to = -1;

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (!adapter.isManuallySorted()) {
                return makeMovementFlags(0, 0);
            }

            ViewHolder vh = (ViewHolder) viewHolder;
            int indentFlags = 0;
            if (vh.isIndented()) {
                indentFlags |= ItemTouchHelper.LEFT;
            }
            int position = vh.getAdapterPosition();
            if (position > 0 && adapter.canIndent(position, vh.task)) {
                indentFlags |= ItemTouchHelper.RIGHT;
            }
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, indentFlags);
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            int fromPosition = source.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            if (from == -1) {
                from = fromPosition;
            }
            to = toPosition;
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return .2f;
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                float shiftSize = ((ViewHolder) viewHolder).getShiftSize();
                dX = Math.max(-shiftSize, Math.min(shiftSize, dX));
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            if (from >= 0 && from != to) {
                if (from < to) {
                    to++;
                }
                adapter.moved(from, to);
            }
            from = -1;
            to = -1;
            Flags.checkAndClear(Flags.TLFP_NO_INTERCEPT_TOUCH);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.indented(
                    viewHolder.getAdapterPosition(),
                    direction == ItemTouchHelper.RIGHT ? 1 : -1);
        }
    }
}
