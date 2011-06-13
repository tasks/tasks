package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.data.Task;

/**
 * Activity for working with draggable task lists, like Google Tasks lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DraggableTaskListActivity extends TaskListActivity {

    // --- task list

    /**
     * If database has an indent property for determining how rows are indented,
     * return it here so we can read it from the cursor and use it
     */
    protected IntegerProperty getIndentProperty() {
        return null;
    }

    public TouchListView getTouchListView() {
        TouchListView tlv = (TouchListView) getListView();
        return tlv;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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
        sqlQueryTemplate.set(SortHelper.adjustQueryForFlagsAndSort(filter.sqlQuery,
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

        getTouchListView().setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
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
                view.findViewById(R.id.indent).getLayoutParams().width = indent * 20;
            }
        }

        @Override
        protected void addListeners(final View container) {
            // super.addListeners(container);
            ViewHolder viewHolder = (ViewHolder)container.getTag();
            viewHolder.completeBox.setOnClickListener(completeBoxListener);

            // context menu listener
            View taskText = container.findViewById(R.id.taskText);
            taskText.setTag(viewHolder);
            taskText.setOnCreateContextMenuListener(listener);

            // tap listener
            taskText.setOnClickListener(listener);
        }
    }

}
