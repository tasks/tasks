/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.activity.TaskListActivity;

/**
 * Legacy task shortcut, takes users to the updated {@link TaskListFragment}.
 * This activity is around so users with existing desktop icons will
 * be able to still launch Astrid.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskList extends Activity {

    // --- implementation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);

        launchTaskList(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        launchTaskList(intent);
    }

    /**
     * intent: ignored for now
     * @param intent
     */
    private void launchTaskList(Intent intent) {
        Intent taskListIntent = new Intent(this, TaskListActivity.class);
        startActivity(taskListIntent);
        finish();
    }
}
