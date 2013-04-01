package com.todoroo.astrid.actfm;

import java.util.concurrent.atomic.AtomicReference;

import android.database.Cursor;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.adapter.TaskAdapter.OnCompletedTaskListener;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.WaitingOnMeDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.WaitingOnMe;

public class WaitingOnMeFragment extends TaskListFragment {

    @Autowired
    private WaitingOnMeDao waitingOnMeDao;

    @Autowired
    private TaskDao taskDao;

    @Override
    public Property<?>[] taskProperties() {
        return AndroidUtilities.addToArray(Property.class, super.taskProperties(), WaitingOnMe.READ_AT, WaitingOnMe.ACKNOWLEDGED);
    }

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new WaitingOnMeTaskAdapter(this, getTaskRowResource(),
                cursor, sqlQueryTemplate, false,
                new OnCompletedTaskListener() {
                    @Override
                    public void onCompletedTask(Task item, boolean newState) {
                        if (newState == true)
                            onTaskCompleted(item);
                    }
                });
    }

    private static class WaitingOnMeTaskAdapter extends TaskAdapter {

        public WaitingOnMeTaskAdapter(TaskListFragment fragment, int resource,
                Cursor c, AtomicReference<String> query, boolean autoRequery,
                OnCompletedTaskListener onCompletedTaskListener) {
            super(fragment, resource, c, query, autoRequery, onCompletedTaskListener);
        }

        @Override
        protected void setTaskAppearance(ViewHolder viewHolder, Task task) {
            super.setTaskAppearance(viewHolder, task);

            TextView nameView = viewHolder.nameView;
            if (task.getValue(WaitingOnMe.READ_AT) == 0 && task.getValue(WaitingOnMe.ACKNOWLEDGED) == 0)
                nameView.setTypeface(null, Typeface.BOLD);
            else
                nameView.setTypeface(null, 0);
        }

    }

    @Override
    public void onTaskListItemClicked(long taskId, boolean editable) {
        super.onTaskListItemClicked(taskId, editable);
        String uuid = taskDao.uuidFromLocalId(taskId);
        if (!TextUtils.isEmpty(uuid)) {
            TodorooCursor<WaitingOnMe> womCursor = waitingOnMeDao.query(Query.select(WaitingOnMe.ID)
                    .where(Criterion.and(WaitingOnMe.TASK_UUID.eq(uuid), WaitingOnMe.READ_AT.eq(0))));
            try {
                if (womCursor.getCount() > 0) {
                    womCursor.moveToFirst();
                    WaitingOnMe wom = new WaitingOnMe(womCursor);
                    wom.setValue(WaitingOnMe.READ_AT, DateUtilities.now());
                    waitingOnMeDao.saveExisting(wom);
                }
            } finally {
                womCursor.close();
            }
        }
    }

}
