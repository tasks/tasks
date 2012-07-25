/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.data.Task;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class ShortcutActivity extends Activity {

    // --- constants

    /** token for passing a task id through extras for viewing a single task */
    public static final String TOKEN_SINGLE_TASK = "id"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s title through extras */
    public static final String TOKEN_FILTER_TITLE = "title"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s sql through extras */
    public static final String TOKEN_FILTER_SQL = "sql"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras as string */
    @Deprecated
    public static final String TOKEN_FILTER_VALUES = "v4nt"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras as exploded ContentValues */
    public static final String TOKEN_FILTER_VALUES_ITEM = "v4ntp_"; //$NON-NLS-1$

    /** token for passing a ComponentNameto launch */
    public static final String TOKEN_CUSTOM_CLASS = "class"; //$NON-NLS-1$

    /** token for passing a image url*/
    public static final String TOKEN_IMAGE_URL = "imageUrl"; //$NON-NLS-1$

    /** List of the above constants for searching */
    private static final String[] CUSTOM_EXTRAS = {
        TOKEN_SINGLE_TASK,
        TOKEN_FILTER_TITLE,
        TOKEN_FILTER_SQL,
        TOKEN_FILTER_VALUES,
        TOKEN_FILTER_VALUES_ITEM,
        TOKEN_CUSTOM_CLASS,
        TOKEN_IMAGE_URL
    };

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

    private void launchTaskList(Intent intent) {
        Bundle extras = intent.getExtras();

        Intent taskListIntent = new Intent(this, TaskListActivity.class);

        if(extras != null && extras.containsKey(TaskListActivity.TOKEN_SOURCE))
                taskListIntent.putExtra(TaskListActivity.TOKEN_SOURCE, extras.getInt(TaskListActivity.TOKEN_SOURCE));

        if(extras != null && extras.containsKey(TOKEN_CUSTOM_CLASS)) {
            taskListIntent.putExtras(intent.getExtras());
        }

        if(extras != null && extras.containsKey(TOKEN_FILTER_SQL)) {
            // launched from desktop shortcut, must create a fake filter
            String title = extras.getString(TOKEN_FILTER_TITLE);
            String sql = extras.getString(TOKEN_FILTER_SQL);
            ContentValues values = null;
            if(extras.containsKey(TOKEN_FILTER_VALUES))
                values = AndroidUtilities.contentValuesFromString(extras.getString(TOKEN_FILTER_VALUES));
            else {
                values = new ContentValues();
                for(String key : extras.keySet()) {
                    if(!key.startsWith(TOKEN_FILTER_VALUES_ITEM))
                        continue;

                    Object value = extras.get(key);
                    key = key.substring(TOKEN_FILTER_VALUES_ITEM.length());

                    // assume one of the big 4...
                    if(value instanceof String)
                        values.put(key, (String) value);
                    else if(value instanceof Integer)
                        values.put(key, (Integer) value);
                    else if(value instanceof Double)
                        values.put(key, (Double) value);
                    else if(value instanceof Long)
                        values.put(key, (Long) value);
                    else
                        throw new IllegalStateException("Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
                }
            }

            Filter filter;
            if (extras.containsKey(TOKEN_CUSTOM_CLASS)) {
                if (extras.containsKey(TOKEN_IMAGE_URL)) {
                    filter = new FilterWithUpdate(title, title, sql, values);
                    ((FilterWithUpdate) filter).imageUrl = extras.getString(TOKEN_IMAGE_URL);
                }
                else
                    filter = new FilterWithCustomIntent(title, title, sql, values);

                Bundle customExtras = new Bundle();
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    if (AndroidUtilities.indexOf(CUSTOM_EXTRAS, key) < 0)
                        AndroidUtilities.putInto(customExtras, key, extras.get(key), false);
                }

                ((FilterWithCustomIntent) filter).customExtras = customExtras; // Something
                ComponentName customTaskList = ComponentName.unflattenFromString(extras.getString(TOKEN_CUSTOM_CLASS));
                ((FilterWithCustomIntent) filter).customTaskList = customTaskList;
            } else {
                filter = new Filter(title, title, sql, values);
            }
            taskListIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        } else if(extras != null && extras.containsKey(TOKEN_SINGLE_TASK)) {
            Filter filter = new Filter(getString(R.string.TLA_custom), getString(R.string.TLA_custom),
                    new QueryTemplate().where(Task.ID.eq(extras.getLong(TOKEN_SINGLE_TASK, -1))), null);

            taskListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            taskListIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
            startActivity(taskListIntent);
        }

        startActivity(taskListIntent);
        finish();
    }

    public static Intent createIntent(Filter filter) {
        Intent shortcutIntent = new Intent(ContextManager.getContext(),
                ShortcutActivity.class);

        if(filter instanceof FilterWithCustomIntent) {
            FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
            if(customFilter.customExtras != null)
                shortcutIntent.putExtras(customFilter.customExtras);
            shortcutIntent.putExtra(TOKEN_CUSTOM_CLASS, customFilter.customTaskList.flattenToString());
            if (filter instanceof FilterWithUpdate) {
                FilterWithUpdate filterWithUpdate = (FilterWithUpdate) filter;
                if (filterWithUpdate.imageUrl != null)
                    shortcutIntent.putExtra(TOKEN_IMAGE_URL, filterWithUpdate.imageUrl);
            }
        }

        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(ShortcutActivity.TOKEN_FILTER_TITLE,
                filter.title);
        shortcutIntent.putExtra(ShortcutActivity.TOKEN_FILTER_SQL,
                filter.getSqlQuery());
        if (filter.valuesForNewTasks != null) {
            for (Entry<String, Object> item : filter.valuesForNewTasks.valueSet()) {
                String key = TOKEN_FILTER_VALUES_ITEM + item.getKey();
                Object value = item.getValue();
                putExtra(shortcutIntent, key, value);
            }
        }
        return shortcutIntent;
    }

    private static void putExtra(Intent intent, String key, Object value) {
        // assume one of the big 4...
        if (value instanceof String)
            intent.putExtra(key, (String) value);
        else if (value instanceof Integer)
            intent.putExtra(key, (Integer) value);
        else if (value instanceof Double)
            intent.putExtra(key, (Double) value);
        else if (value instanceof Long)
            intent.putExtra(key, (Long) value);
        else
            throw new IllegalStateException(
                    "Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
    }
}
