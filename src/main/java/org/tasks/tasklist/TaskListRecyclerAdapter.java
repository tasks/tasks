package org.tasks.tasklist;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskDuplicator;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.ui.MenuColorizer;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

public class TaskListRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> implements ViewHolder.ViewHolderCallbacks {

    private static final String EXTRA_SELECTED_TASK_IDS = "extra_selected_task_ids";

    private final MultiSelector multiSelector = new MultiSelector();

    private final Activity activity;
    private final TaskAdapter adapter;
    private final ViewHolderFactory viewHolderFactory;
    private final TaskListFragment taskList;
    private final TaskDeleter taskDeleter;
    private final TaskDuplicator taskDuplicator;
    private final Tracker tracker;
    private final DialogBuilder dialogBuilder;
    private final ItemTouchHelper itemTouchHelper;

    private ActionMode mode = null;

    public TaskListRecyclerAdapter(Activity activity, TaskAdapter adapter,
                                   ViewHolderFactory viewHolderFactory,
                                   TaskListFragment taskList, TaskDeleter taskDeleter,
                                   TaskDuplicator taskDuplicator, Tracker tracker,
                                   DialogBuilder dialogBuilder) {
        this.activity = activity;
        this.adapter = adapter;
        this.viewHolderFactory = viewHolderFactory;
        this.taskList = taskList;
        this.taskDeleter = taskDeleter;
        this.taskDuplicator = taskDuplicator;
        this.tracker = tracker;
        this.dialogBuilder = dialogBuilder;
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
    }

    public void applyToRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public Bundle getSaveState() {
        Bundle information = new Bundle();
        List<Long> selectedTaskIds = transform(multiSelector.getSelectedPositions(), adapter::getTaskId);
        information.putLongArray(EXTRA_SELECTED_TASK_IDS, Longs.toArray(selectedTaskIds));
        return information;
    }

    public void restoreSaveState(Bundle savedState) {
        long[] longArray = savedState.getLongArray(EXTRA_SELECTED_TASK_IDS);
        if (longArray.length > 0) {
            mode = ((TaskListActivity) activity).startSupportActionMode(actionModeCallback);
            multiSelector.setSelectable(true);

            for (int position : adapter.getTaskPositions(Longs.asList(longArray))) {
                multiSelector.setSelected(position, 0L, true);
            }

            updateModeTitle();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(activity)
                .inflate(R.layout.task_adapter_row_simple, parent, false);
        return viewHolderFactory.newViewHolder(view, this, multiSelector);
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
        if (multiSelector.tapSelection(viewHolder)) {
            viewHolder.setMoving(false);
            updateActionMode();
        } else {
            Task task = viewHolder.task;
            taskList.onTaskListItemClicked(task.getId());
        }
    }

    @Override
    public boolean onLongPress(ViewHolder viewHolder) {
        multiSelector.setSelectable(true);
        if (adapter.isManuallySorted()) {
            viewHolder.setMoving(true);
            Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
            return false;
        }

        multiSelector.tapSelection(viewHolder);
        updateActionMode();
        return true;
    }

    private void updateActionMode() {
        if (mode == null) {
            mode = ((TaskListActivity) activity).startSupportActionMode(actionModeCallback);
        }
        afterSelect();
    }

    private void afterSelect() {
        if (multiSelector.getSelectedPositions().isEmpty()) {
            if (mode != null) {
                mode.finish();
            }
        } else {
            updateModeTitle();
        }
    }

    private List<Task> getTasks() {
        return newArrayList(transform(multiSelector.getSelectedPositions(), adapter::getTask));
    }

    private void deleteSelectedItems() {
        tracker.reportEvent(Tracking.Events.MULTISELECT_DELETE);
        List<Task> tasks = getTasks();
        int result = taskDeleter.delete(tasks);
        taskList.onTaskDelete(tasks);
        for (int position : Ordering.natural().reverse().sortedCopy(multiSelector.getSelectedPositions())) {
            notifyItemRemoved(position);
        }
        taskList.makeSnackbar(activity.getString(R.string.delete_multiple_tasks_confirmation, Integer.toString(result))).show();
    }

    private void copySelectedItems() {
        tracker.reportEvent(Tracking.Events.MULTISELECT_CLONE);
        List<Task> duplicates = taskDuplicator.duplicate(getTasks());
        taskList.onTaskCreated(duplicates);
        taskList.makeSnackbar(activity.getString(R.string.copy_multiple_tasks_confirmation, Integer.toString(duplicates.size()))).show();
    }

    private void updateModeTitle() {
        if (mode != null) {
            mode.setTitle(Integer.toString(multiSelector.getSelectedPositions().size()));
        }
    }

    private ModalMultiSelectorCallback actionModeCallback = new ModalMultiSelectorCallback(multiSelector) {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_multi_select, menu);
            MenuColorizer.colorMenu(activity, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete:
                    dialogBuilder.newMessageDialog(R.string.delete_selected_tasks)
                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                deleteSelectedItems();
                                mode.finish();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                case R.id.copy_tasks:
                    dialogBuilder.newMessageDialog(R.string.copy_selected_tasks)
                            .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> {
                                copySelectedItems();
                                mode.finish();
                            }))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            multiSelector.clearSelections();
            TaskListRecyclerAdapter.this.mode = null;
        }
    };

    private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private int from = -1;
        private int to = -1;

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (!adapter.isManuallySorted() || mode != null) {
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
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.setMoving(false);
            if (multiSelector.isSelectable()) {
                if (from == -1) {
                    multiSelector.setSelected(vh, true);
                    updateActionMode();
                } else {
                    multiSelector.clearSelections();
                    multiSelector.setSelectable(false);
                    if (from >= 0 && from != to) {
                        if (from < to) {
                            to++;
                        }
                        adapter.moved(from, to);
                    }
                }
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
