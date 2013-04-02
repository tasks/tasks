package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.FetchHistory;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.TaskService;

public class TaskCommentsFragment extends CommentsFragment {

    public static final String EXTRA_TASK = "extra_task"; //$NON-NLS-1$

    @Autowired
    private TaskDao taskDao;

    private Task task;

    @Autowired
    private TaskService taskService;

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
    protected void refetchModel() {
        if (task != null) {
            task = taskDao.fetch(task.getId(), Task.PROPERTIES);
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
        return taskService.getActivityAndHistoryForTask(task);
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
    protected boolean canLoadMoreHistory() {
        return hasModel() && task.getValue(Task.HISTORY_HAS_MORE) > 0;
    }

    @Override
    protected void loadMoreHistory(int offset, SyncMessageCallback callback) {
        new FetchHistory<Task>(taskDao, Task.HISTORY_FETCH_DATE, Task.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TASKS,
                task.getUuid(), task.getValue(Task.TITLE), 0, offset, callback).execute();
    }

    @Override
    protected void performFetch(boolean manual, SyncMessageCallback done) {
        if (task != null) {
            ActFmSyncThread.getInstance().enqueueMessage(new BriefMe<UserActivity>(UserActivity.class, null, task.getValue(Task.USER_ACTIVITIES_PUSHED_AT), BriefMe.TASK_ID_KEY, task.getUuid()), done);
            new FetchHistory<Task>(taskDao, Task.HISTORY_FETCH_DATE, Task.HISTORY_HAS_MORE, NameMaps.TABLE_ID_TASKS,
                    task.getUuid(), task.getValue(Task.TITLE), task.getValue(Task.HISTORY_FETCH_DATE), 0, done).execute();
        }
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
