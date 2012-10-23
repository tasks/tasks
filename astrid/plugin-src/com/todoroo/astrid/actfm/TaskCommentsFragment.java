package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.ListView;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;

public class TaskCommentsFragment extends CommentsFragment {

    public static final String EXTRA_TASK = "extra_task"; //$NON-NLS-1$

    private Task task;

    @Override
    protected void loadModelFromIntent(Intent intent) {
        if (task == null)
            task = intent.getParcelableExtra(EXTRA_TASK);
    }

    @Override
    protected boolean hasModel() {
        return task != null;
    }

    @Override
    protected String getModelName() {
        return task.getValue(Task.TITLE);
    }

    @Override
    protected Cursor getCursor() {
        if (!task.containsNonNullValue(Task.REMOTE_ID))
            return updateDao.query(Query.select(Update.PROPERTIES).where(Update.TASK_LOCAL.eq(task.getId())));
        else
            return updateDao.query(Query.select(Update.PROPERTIES).where(Criterion.or(
                    Update.TASK.eq(task.getValue(Task.REMOTE_ID)), Update.TASK_LOCAL.eq(task.getId()))));
    }

    @Override
    protected String getSourceIdentifier() {
        return (task == null) ? UpdateAdapter.FROM_RECENT_ACTIVITY_VIEW : UpdateAdapter.FROM_TASK_VIEW;
    }

    @Override
    protected void addHeaderToListView(ListView listView) {
        // Do nothing
    }

    @Override
    protected void populateListHeader(ViewGroup header) {
        // Do nothing
    }

    @Override
    protected void refreshActivity(boolean manual) {
        // TODO Auto-generated method stub
    }


    @Override
    protected void addComment() {
        // TODO Auto-generated method stub
    }

}
