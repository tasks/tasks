/**
 * TODO: make this lightweight, don't extend the entire TaskListActivity
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.data.Task;

/**
 * @author joshuagross
 *
 * Create a new task based on incoming links from the "share" menu
 */
public final class ShareLinkActivity extends TaskListActivity {
    public ShareLinkActivity () {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent callerIntent = getIntent();

        String subject = callerIntent.getStringExtra(Intent.EXTRA_SUBJECT);
        if(subject == null)
            subject = ""; //$NON-NLS-1$
        Task task = quickAddTask(subject, false);
        task.setValue(Task.NOTES, callerIntent.getStringExtra(Intent.EXTRA_TEXT));
        taskService.save(task);
        Intent intent = new Intent(this, TaskEditActivity.class);
        intent.putExtra(TaskEditActivity.TOKEN_ID, task.getId());
        startActivityForResult(intent, ACTIVITY_EDIT_TASK);
    }
}
