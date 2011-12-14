package com.todoroo.astrid.gtasks;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.MenuInflater;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.DraggableTaskListActivity;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncOnSaveService;

public class GtasksListActivity extends DraggableTaskListActivity {

    protected static final int MENU_CLEAR_COMPLETED_ID = MENU_ADDON_INTENT_ID + 1;

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    @Autowired private GtasksSyncOnSaveService gtasksSyncOnSaveService;

    @Autowired private GtasksMetadataService gtasksMetadataService;

    @Override
    protected IntegerProperty getIndentProperty() {
        return GtasksMetadata.INDENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getTouchListView().setDropListener(dropListener);
        getTouchListView().setSwipeListener(swipeListener);

        if(!Preferences.getBoolean(GtasksPreferenceService.PREF_SHOWN_LIST_HELP, false)) {
            Preferences.setBoolean(GtasksPreferenceService.PREF_SHOWN_LIST_HELP, true);
            DialogUtilities.okDialog(getActivity(),
                    getString(R.string.gtasks_help_title),
                    android.R.drawable.ic_dialog_info,
                    getString(R.string.gtasks_help_body), null);
        }

        taskAdapter.addOnCompletedTaskListener(new OnCompletedTaskListener() {
            @Override
            public void onCompletedTask(Task item, boolean newState) {
                setCompletedForItemAndSubtasks(item, newState);
            }
        });
    }

    private final TouchListView.DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);

            if(to == getListView().getCount() - 1)
                gtasksTaskListUpdater.moveTo(targetTaskId, -1);
            else
                gtasksTaskListUpdater.moveTo(targetTaskId, destinationTaskId);
            gtasksSyncOnSaveService.triggerMoveForMetadata(gtasksMetadataService.getTaskMetadata(targetTaskId));
            loadTaskListContent(true);
        }
    };

    private final TouchListView.SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(targetTaskId, 1);
            gtasksSyncOnSaveService.triggerMoveForMetadata(gtasksMetadataService.getTaskMetadata(targetTaskId));
            loadTaskListContent(true);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(targetTaskId, -1);
            gtasksSyncOnSaveService.triggerMoveForMetadata(gtasksMetadataService.getTaskMetadata(targetTaskId));
            loadTaskListContent(true);
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem item = menu.add(Menu.NONE, MENU_CLEAR_COMPLETED_ID, Menu.FIRST, this.getString(R.string.gtasks_GTA_clear_completed));
        item.setIcon(android.R.drawable.ic_input_delete); // Needs new icon
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_CLEAR_COMPLETED_ID) {
            clearCompletedTasks();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void clearCompletedTasks() {

        final ProgressDialog pd = new ProgressDialog(getActivity());
        final TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.sqlQuery, null, Task.ID, Task.COMPLETION_DATE);
        pd.setMessage(this.getString(R.string.gtasks_GTA_clearing));
        pd.show();

        new Thread() {
            @Override
            public void run() {
                String listId = null;
                try {
                    for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                        Task t = new Task(tasks);
                        if (t.isCompleted()) {
                            if (listId == null) {
                                listId = gtasksMetadataService.getTaskMetadata(t.getId()).getValue(GtasksMetadata.LIST_ID);
                            }
                            t.setValue(Task.DELETION_DATE, DateUtilities.now());
                            taskService.save(t);
                        }
                    }
                } finally {
                    tasks.close();
                    DialogUtilities.dismissDialog(getActivity(), pd);
                }
                if (listId != null) {
                    gtasksTaskListUpdater.correctMetadataForList(listId);
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        loadTaskListContent(true);
                    }
                });
            }
        }.start();
    }

    private void setCompletedForItemAndSubtasks(Task item, boolean completedState) {
        final TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.sqlQuery, null, Task.ID, Task.COMPLETION_DATE);
        final long itemId = item.getId();
        final boolean completed = completedState;

        new Thread() {
            @Override
            public void run() {
                try {
                    for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                        Task curr = new Task(tasks);
                        if (curr.getId() == itemId) {
                            int itemIndent = gtasksMetadataService.getTaskMetadata(curr.getId()).getValue(GtasksMetadata.INDENT);
                            tasks.moveToNext();
                            while (!tasks.isAfterLast()) {
                                Task next = new Task(tasks);
                                int currIndent = gtasksMetadataService.getTaskMetadata(next.getId()).getValue(GtasksMetadata.INDENT);
                                if (currIndent > itemIndent) {
                                    if (completed)
                                        next.setValue(Task.COMPLETION_DATE, DateUtilities.now());
                                    else
                                        next.setValue(Task.COMPLETION_DATE, 0L);
                                    taskService.save(next);
                                } else break;

                                tasks.moveToNext();
                            }
                            break;
                        }
                    }
                } finally {
                    tasks.close();
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        loadTaskListContent(true);
                    }
                });
            }
        }.start();
    }
}
