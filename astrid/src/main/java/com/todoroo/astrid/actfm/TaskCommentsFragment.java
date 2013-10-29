package com.todoroo.astrid.actfm;

import android.content.Intent;
import android.database.Cursor;
import android.widget.ListView;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;

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
        return taskService.getActivityForTask(task);
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
}
