package com.todoroo.astrid.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.commonsware.cwac.tlv.TouchListView;
import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

/**
 * Activity for working with draggable task lists, like Google Tasks lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DraggableTaskListActivity extends TaskListActivity {

    // --- gtasks temp stuff
    private final String listId = "17816916813445155620:0:0"; //$NON-NLS-1$
    Filter filter = new Filter("Tim's Tasks", "Tim's Tasks", new QueryTemplate().join( //$NON-NLS-1$ //$NON-NLS-2$
            Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                    MetadataCriteria.withKey("gtasks"), //$NON-NLS-1$
                    TaskCriteria.isVisible(),
                    TaskCriteria.notDeleted(),
                    Metadata.VALUE2.eq(listId))).orderBy(
                            Order.asc(Functions.cast(Metadata.VALUE5, "INTEGER"))), //$NON-NLS-1$
            null);

    // --- end

        @Override
    public void onCreate(Bundle icicle) {
        getIntent().putExtra(TOKEN_FILTER, filter);
        super.onCreate(icicle);

        TouchListView tlv = (TouchListView) getListView();
        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }

    @Override
    protected View getListBody(ViewGroup root) {
        return getLayoutInflater().inflate(R.layout.task_list_body_draggable, root, false);
    }

    private final TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            /*String item = adapter.getItem(from);

            adapter.remove(item);
            adapter.insert(item, to);*/
        }
    };

    private final TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {
            // new GtasksIndentAction.GtasksIncreaseIndentAction().indent(adapter.getItemId(which));
            // adapter.notifyDataSetChanged();
            // adapter.remove(adapter.getItem(which));
        }
    };

}
