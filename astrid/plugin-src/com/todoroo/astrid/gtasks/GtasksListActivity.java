package com.todoroo.astrid.gtasks;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

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
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncOnSaveService;

public class GtasksListActivity extends DraggableTaskListActivity {

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
            DialogUtilities.okDialog(this,
                    getString(R.string.gtasks_help_title),
                    android.R.drawable.ic_dialog_info,
                    getString(R.string.gtasks_help_body), null);
        }
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.add(Menu.NONE, MENU_SORT_ID, Menu.FIRST, "Clear completed");
        item.setIcon(android.R.drawable.ic_input_delete); // Needs new icon
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, final MenuItem item) {
        if (item.getItemId() == MENU_SORT_ID) {
            clearCompletedTasks();
            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }

    private void clearCompletedTasks() {

        final ProgressDialog pd = new ProgressDialog(this); //progressDialog(this, this.getString(R.string.gtasks_GLA_clearing))
        final TodorooCursor<Task> tasks = taskService.fetchFiltered(filter.sqlQuery, null, Task.ID, Task.COMPLETION_DATE);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage(this.getString(R.string.gtasks_GLA_clearing));pd.setMax(tasks.getCount());
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
                        pd.incrementProgressBy(1);
                    }
                } finally {
                    tasks.close();
                    pd.dismiss();
                }
                if (listId != null) {
                    gtasksTaskListUpdater.correctMetadataForList(listId);
                }
                GtasksListActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        loadTaskListContent(true);
                    }
                });
            }
        }.start();
    }
}
