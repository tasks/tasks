package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import android.database.Cursor;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

import com.commonsware.cwac.tlv.TouchListView;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.data.Task;

/**
 * Activity for working with draggable task lists, like Google Tasks lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class DraggableTaskListFragment extends TaskListFragment {

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
    protected View getListBody(ViewGroup root) {
        return getActivity().getLayoutInflater().inflate(R.layout.task_list_body_draggable, root, false);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new DraggableTaskAdapter(this, R.layout.task_adapter_draggable_row,
                cursor, sqlQueryTemplate, false, null);
    }

    @Override
    protected void setUpUiComponents() {
        super.setUpUiComponents();

        getTouchListView().setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
    }

    // --- task adapter

    @Override
    public Property<?>[] taskProperties() {
        ArrayList<Property<?>> properties = new ArrayList<Property<?>>(Arrays.asList(TaskAdapter.PROPERTIES));
        if(getIndentProperty() != null)
            properties.add(getIndentProperty());
        return properties.toArray(new Property<?>[properties.size()]);
    }

    private final class DraggableTaskAdapter extends TaskAdapter {

        private DraggableTaskAdapter(TaskListFragment activity, int resource,
                Cursor c, AtomicReference<String> query, boolean autoRequery,
                OnCompletedTaskListener onCompletedTaskListener) {
            super(activity, resource, c, query, autoRequery,
                    onCompletedTaskListener);

            applyListeners = APPLY_LISTENERS_ROW_BODY;
        }

        @Override
        protected ViewHolder getTagFromCheckBox(View v) {
            return (ViewHolder)((View)v.getParent()).getTag();
        }

        @Override
        public synchronized void setFieldContentsAndVisibility(View view) {
            super.setFieldContentsAndVisibility(view);

            ViewHolder viewHolder = (ViewHolder) view.getTag();
            if(getIndentProperty() != null) {
                int indent = viewHolder.task.getValue(getIndentProperty());
                view.findViewById(R.id.indent).getLayoutParams().width =
                    (int) (displayMetrics.density * (indent * 20));
            }
        }
    }

}
