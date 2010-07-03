/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.reminders;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.activities.TaskList;
import com.todoroo.astrid.activity.TaskListActivity;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class NotificationActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchTaskList(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        launchTaskList(intent);
    }

    private void launchTaskList(Intent intent) {
        Intent taskListIntent = new Intent(this, TaskListActivity.class);
        taskListIntent.putExtra(TaskList.VARIABLES_TAG, intent.getExtras());
        startActivity(taskListIntent);

        finish();
    }
}
