package com.todoroo.astrid.gtasks;

import android.os.Bundle;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.activity.DraggableTaskListActivity;

public class GtasksListActivity extends DraggableTaskListActivity {

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    public static final String TOKEN_LIST_ID = "listId"; //$NON-NLS-1$

    private String listId;

    @Override
    protected IntegerProperty getIndentProperty() {
        return GtasksMetadata.INDENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        listId = getIntent().getStringExtra(TOKEN_LIST_ID);

        getTouchListView().setDropListener(dropListener);
        getTouchListView().setSwipeListener(swipeListener);
    }

    private final TouchListView.DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            long targetTaskId = taskAdapter.getItemId(from);
            long destinationTaskId = taskAdapter.getItemId(to);
            gtasksTaskListUpdater.moveTo(listId, targetTaskId, destinationTaskId);
            loadTaskListContent(true);
        }
    };

    private final TouchListView.SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(listId, targetTaskId, 1);
            loadTaskListContent(true);
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(listId, targetTaskId, -1);
            loadTaskListContent(true);
        }
    };

}
