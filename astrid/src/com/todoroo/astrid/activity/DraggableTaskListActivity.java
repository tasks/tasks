package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.adapter.TaskAdapter;
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
    Filter builtInFilter = new Filter("Tim's Tasks", "Tim's Tasks", new QueryTemplate().join( //$NON-NLS-1$ //$NON-NLS-2$
            Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                    MetadataCriteria.withKey("gtasks"), //$NON-NLS-1$
                    TaskCriteria.isVisible(),
                    TaskCriteria.notDeleted(),
                    Metadata.VALUE2.eq(listId))).orderBy(
                            Order.asc(Functions.cast(Metadata.VALUE5, "INTEGER"))), //$NON-NLS-1$
            null);

    // --- end


    // --- task list

    public static final IntegerProperty INDENT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    public static final IntegerProperty ORDER = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public IntegerProperty getIndentProperty() {
        return INDENT;
    }

    @Override
    public void onCreate(Bundle icicle) {
        getIntent().putExtra(TOKEN_FILTER, builtInFilter);
        super.onCreate(icicle);

        TouchListView tlv = (TouchListView) getListView();
        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }

    @Override
    protected View getListBody(ViewGroup root) {
        return getLayoutInflater().inflate(R.layout.task_list_body_draggable, root, false);
    }

    // --- task adapter

    /**
     * Fill in the Task List with current items
     * @param withCustomId force task with given custom id to be part of list
     */
    @Override
    protected void setUpTaskList() {
        sqlQueryTemplate.set(SortSelectionActivity.adjustQueryForFlagsAndSort(filter.sqlQuery,
                sortFlags, sortSort));

        ((TextView)findViewById(R.id.listLabel)).setText(filter.title);

        // perform query
        TodorooCursor<Task> currentCursor = taskService.fetchFiltered(
                sqlQueryTemplate.get(), null, getProperties());
        startManagingCursor(currentCursor);

        // set up list adapters
        taskAdapter = new DraggableTaskAdapter(this, R.layout.task_adapter_draggable_row, currentCursor, sqlQueryTemplate,
                false, null);

        setListAdapter(taskAdapter);
        getListView().setOnScrollListener(this);
        registerForContextMenu(getListView());

        loadTaskListContent(false);
    }

    public Property<?>[] getProperties() {
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(TaskAdapter.PROPERTIES));
        if(getIndentProperty() != null)
            properties.add(getIndentProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(ListActivity activity, int resource,
                Cursor c, AtomicReference<String> query, boolean autoRequery,
                OnCompletedTaskListener onCompletedTaskListener) {
            super(activity, resource, c, query, autoRequery,
                    onCompletedTaskListener);
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if(getIndentProperty() != null) {
                int indent = viewHolder.task.getValue(getIndentProperty());
                view.findViewById(R.id.indent).getLayoutParams().width = indent * 15;
            }
        }

        @Override
        protected void addListeners(final View container) {
            // super.addListeners(container);
            ViewHolder viewHolder = (ViewHolder)container.getTag();
            viewHolder.completeBox.setOnClickListener(completeBoxListener);

            // context menu listener
            //container.findViewById(R.id.task_row).setOnCreateContextMenuListener(listener);

            // tap listener
            //container.findViewById(R.id.task_row).setOnClickListener(listener);
        }
    }

    // --- drag and swipe handlers

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
