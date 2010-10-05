package com.todoroo.astrid.gtasks;

import android.os.Bundle;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.DraggableTaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

public class GtasksListActivity extends DraggableTaskListActivity {

    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    // --- gtasks temp stuff
    private final String listId = "17816916813445155620:0:0"; //$NON-NLS-1$
    Filter builtInFilter = new Filter("Tim's Tasks", "Tim's Tasks", new QueryTemplate().join( //$NON-NLS-1$ //$NON-NLS-2$
            Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                    MetadataCriteria.withKey("gtasks"), //$NON-NLS-1$
                    TaskCriteria.isVisible(),
                    TaskCriteria.notDeleted(),
                    Metadata.VALUE2.eq(listId))).orderBy(
                            Order.asc(Functions.cast(Metadata.VALUE5, "INTEGER"))), //$NON-NLS-1$
            null);

    // --- end

    @Override
    protected IntegerProperty getIndentProperty() {
        return GtasksMetadata.INDENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        getIntent().putExtra(TOKEN_FILTER, builtInFilter);
        super.onCreate(icicle);

        getTouchListView().setDropListener(dropListener);
        getTouchListView().setSwipeListener(swipeListener);
    }

    private final TouchListView.DropListener dropListener = new DropListener() {
        @Override
        public void drop(int from, int to) {
            // meep
        }
    };

    private final TouchListView.SwipeListener swipeListener = new SwipeListener() {
        @Override
        public void swipeRight(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(listId, targetTaskId, 1);
            taskAdapter.notifyDataSetChanged();
        }

        @Override
        public void swipeLeft(int which) {
            long targetTaskId = taskAdapter.getItemId(which);
            gtasksTaskListUpdater.indent(listId, targetTaskId, -1);
            taskAdapter.notifyDataSetChanged();
        }
    };

}
