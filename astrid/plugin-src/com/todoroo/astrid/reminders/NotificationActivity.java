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

import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class NotificationActivity extends Activity {

    // --- constants

    /** task id from notification */
    public static final String TOKEN_ID = "id";

    /** task title */
    public static final String TOKEN_TITLE = "title";

    // --- implementation

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
        long id = intent.getLongExtra(TOKEN_ID, -1);
        if(id == -1)
            return;

        Intent taskListIntent = new Intent(this, TaskListActivity.class);
        Filter itemFilter = new Filter(ReminderPlugin.IDENTIFIER,
                "Notification",
                "Notification",
                new QueryTemplate().where(TaskCriteria.byId(id)),
                null);

        taskListIntent.putExtra(TaskListActivity.TOKEN_FILTER, itemFilter);
        startActivity(taskListIntent);

        finish();
    }
}
