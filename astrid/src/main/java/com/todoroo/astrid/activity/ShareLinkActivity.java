/**
 * TODO: make this lightweight, don't extend the entire TaskListActivity
 */
package com.todoroo.astrid.activity;

import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskService;

import javax.inject.Inject;

/**
 * @author joshuagross
 *
 * Create a new task based on incoming links from the "share" menu
 */
public final class ShareLinkActivity extends TaskListActivity {

    @Inject TaskService taskService;
    @Inject TaskCreator taskCreator;

    private String subject;
    private boolean handled;

    private static final String TOKEN_LINK_HANDLED = "linkHandled"; //$NON-NLS-1$

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent callerIntent = getIntent();

        subject = callerIntent.getStringExtra(Intent.EXTRA_SUBJECT);
        if(subject == null) {
            subject = ""; //$NON-NLS-1$
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!handled) {
            Intent callerIntent = getIntent();

            Task task = taskCreator.basicQuickAddTask(subject);
            if (task != null) {
                task.setNotes(callerIntent.getStringExtra(Intent.EXTRA_TEXT));
                taskService.save(task);
                handled = true;
                onTaskListItemClicked(task.getId());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TOKEN_LINK_HANDLED, handled);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        handled = savedInstanceState.getBoolean(TOKEN_LINK_HANDLED);
    }
}
