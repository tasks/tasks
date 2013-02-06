package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.History;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
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
        Query taskQuery = queryForTask(task, UpdateAdapter.USER_TABLE_ALIAS, UpdateAdapter.USER_ACTIVITY_PROPERTIES, UpdateAdapter.USER_PROPERTIES);
        int length = UpdateAdapter.USER_ACTIVITY_PROPERTIES.length + UpdateAdapter.USER_PROPERTIES.length;

        Property<?>[] paddingArray = new Property<?>[Math.max(0, length - UpdateAdapter.HISTORY_PROPERTIES.length)];
        for (int i = 0; i < paddingArray.length; i++) {
            paddingArray[i] = UpdateAdapter.PADDING_PROPERTY;
        }

        Query historyQuery = Query.select(AndroidUtilities.addToArray(UpdateAdapter.HISTORY_PROPERTIES, paddingArray)).from(History.TABLE)
                .where(Criterion.and(History.TABLE_ID.eq(NameMaps.TABLE_ID_TASKS), History.TARGET_ID.eq(task.getUuid())))
                .from(History.TABLE);

        Query resultQuery = taskQuery.union(historyQuery).orderBy(Order.desc("1")); //$NON-NLS-1$

        return userActivityDao.query(resultQuery);
    }

    private static Query queryForTask(Task task, String userTableAlias, Property<?>[] activityProperties, Property<?>[] userProperties) {
        Query result = Query.select(AndroidUtilities.addToArray(activityProperties, userProperties))
                .where(Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT), UserActivity.TARGET_ID.eq(task.getUuid())));
        if (!TextUtils.isEmpty(userTableAlias))
            result = result.join(Join.left(User.TABLE.as(userTableAlias), UserActivity.USER_UUID.eq(Field.field(userTableAlias + "." + User.UUID.name)))); //$NON-NLS-1$
        return result;
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
        done.run();
//        actFmSyncService.fetchUpdatesForTask(task, manual, done);
    }

    @Override
    protected UserActivity createUpdate() {
        UserActivity update = new UserActivity();
        update.setValue(UserActivity.MESSAGE, addCommentField.getText().toString());
        update.setValue(UserActivity.ACTION, UserActivity.ACTION_TASK_COMMENT);
        update.setValue(UserActivity.USER_UUID, Task.USER_ID_SELF);
        update.setValue(UserActivity.TARGET_ID, task.getUuid());
        update.setValue(UserActivity.TARGET_NAME, task.getValue(Task.TITLE));
        update.setValue(UserActivity.CREATED_AT, DateUtilities.now());
        return update;
    }

    @Override
    protected String commentAddStatistic() {
        return StatisticsConstants.ACTFM_TASK_COMMENT;
    }

}
