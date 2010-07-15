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
package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class ShortcutActivity extends Activity {

    // --- constants

    /** token for passing a {@link Filter}'s title through extras */
    public static final String TOKEN_FILTER_TITLE = "title"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s sql through extras */
    public static final String TOKEN_FILTER_SQL = "sql"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras */
    public static final String TOKEN_FILTER_VALUES = "v4nt"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras (keys) */
    public static final String TOKEN_FILTER_VALUES_KEYS = "v4ntk"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras (values) */
    public static final String TOKEN_FILTER_VALUES_VALUES = "v4ntv"; //$NON-NLS-1$

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
        Bundle extras = intent.getExtras();

        if(extras != null && extras.containsKey(TOKEN_FILTER_SQL)) {
            // launched from desktop shortcut, must create a fake filter
            String title = extras.getString(TOKEN_FILTER_TITLE);
            String sql = extras.getString(TOKEN_FILTER_SQL);
            ContentValues values = null;
            if(extras.containsKey(TOKEN_FILTER_VALUES))
                values = AndroidUtilities.contentValuesFromString(extras.getString(TOKEN_FILTER_VALUES));

            Filter filter = new Filter("", title, new QueryTemplate(), values); //$NON-NLS-1$
            filter.sqlQuery = sql;

            Intent taskListIntent = new Intent(this, TaskListActivity.class);
            taskListIntent.putExtra(TaskListActivity.TOKEN_FILTER, filter);
            startActivity(taskListIntent);
        }
        finish();
    }
}
