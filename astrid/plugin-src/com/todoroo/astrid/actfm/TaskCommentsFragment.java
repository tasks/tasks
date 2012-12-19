package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.StatisticsConstants;

public class TaskCommentsFragment extends CommentsFragment {

    public static final String EXTRA_TASK = "extra_task"; //$NON-NLS-1$

    @Autowired
    private TaskDao taskDao;

    private Task task;

    public TaskCommentsFragment() {
        super();
    }

    @Override
    protected void loadModelFromIntent(Intent intent) {
        if (task == null) {
            long taskId = intent.getLongExtra(EXTRA_TASK, 0L);
            task = taskDao.fetch(taskId, Task.PROPERTIES);
        }
    }

    @Override
    protected boolean hasModel() {
        return task != null;
    }

    @Override
    protected int getLayout() {
        return R.layout.tag_updates_fragment;
    }

    @Override
    protected String getModelName() {
        return task.getValue(Task.TITLE);
    }

    @Override
    protected Cursor getCursor() {
        if (!task.containsNonNullValue(Task.REMOTE_ID))
            return updateDao.query(Query.select(Update.PROPERTIES).where(Update.TASK_LOCAL.eq(task.getId())).orderBy(Order.desc(Update.CREATION_DATE)));
        else
            return updateDao.query(Query.select(Update.PROPERTIES).where(Criterion.or(
                    Update.TASK.eq(task.getValue(Task.REMOTE_ID)), Update.TASK_LOCAL.eq(task.getId()))).orderBy(Order.desc(Update.CREATION_DATE)));
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
    protected void performFetch(boolean manual, Runnable done) {
        actFmSyncService.fetchUpdatesForTask(task, manual, done);
    }

    @Override
    protected Update createUpdate() {
        Update update = new Update();
        update.setValue(Update.MESSAGE, addCommentField.getText().toString());
        update.setValue(Update.ACTION_CODE, UpdateAdapter.UPDATE_TASK_COMMENT);
        update.setValue(Update.USER_ID, 0L);
        if (task.containsNonNullValue(Task.REMOTE_ID))
            update.setValue(Update.TASK, task.getValue(Task.REMOTE_ID));
        update.setValue(Update.TASK_LOCAL, task.getId());
        update.setValue(Update.CREATION_DATE, DateUtilities.now());
        update.setValue(Update.TARGET_NAME, task.getValue(Task.TITLE));
        return update;
    }

    @Override
    protected String commentAddStatistic() {
        return StatisticsConstants.ACTFM_TASK_COMMENT;
    }

}
