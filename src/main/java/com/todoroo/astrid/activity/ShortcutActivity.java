/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;

import org.tasks.injection.InjectingAppCompatActivity;

import java.util.Map.Entry;
import java.util.Set;

/**
 * This activity is launched when a user opens up a notification from the
 * tray. It launches the appropriate activity based on the passed in parameters.
 *
 * @author timsu
 *
 */
public class ShortcutActivity extends InjectingAppCompatActivity {

    // --- constants

    /** token for passing a {@link Filter}'s title through extras */
    public static final String TOKEN_FILTER_TITLE = "title"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s sql through extras */
    public static final String TOKEN_FILTER_SQL = "sql"; //$NON-NLS-1$

    /** token for passing a {@link Filter}'s values for new tasks through extras as exploded ContentValues */
    public static final String TOKEN_FILTER_VALUES_ITEM = "v4ntp_"; //$NON-NLS-1$

    /** token for passing a ComponentNameto launch */
    public static final String TOKEN_CUSTOM_CLASS = "class"; //$NON-NLS-1$

    /** List of the above constants for searching */
    private static final String[] CUSTOM_EXTRAS = {
        TOKEN_FILTER_TITLE,
        TOKEN_FILTER_SQL,
        TOKEN_FILTER_VALUES_ITEM,
        TOKEN_CUSTOM_CLASS
    };

    // --- implementation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchTaskList();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        launchTaskList();
    }

    private void launchTaskList() {
        Intent intent = getIntent();

        Bundle extras = intent.getExtras();

        Intent taskListIntent = new Intent(this, TaskListActivity.class);
        taskListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if(extras != null && extras.containsKey(TOKEN_CUSTOM_CLASS)) {
            taskListIntent.putExtras(intent.getExtras());
        }

        if(extras != null && extras.containsKey(TOKEN_FILTER_SQL)) {
            // launched from desktop shortcut, must create a fake filter
            String title = extras.getString(TOKEN_FILTER_TITLE);
            String sql = extras.getString(TOKEN_FILTER_SQL);
            sql = sql.replace("tasks.userId=0", "1"); // TODO: replace dirty hack for missing column
            ContentValues values = new ContentValues();
            for(String key : extras.keySet()) {
                if(!key.startsWith(TOKEN_FILTER_VALUES_ITEM)) {
                    continue;
                }

                Object value = extras.get(key);
                key = key.substring(TOKEN_FILTER_VALUES_ITEM.length());

                // assume one of the big 4...
                if(value instanceof String) {
                    values.put(key, (String) value);
                } else if(value instanceof Integer) {
                    values.put(key, (Integer) value);
                } else if(value instanceof Double) {
                    values.put(key, (Double) value);
                } else if(value instanceof Long) {
                    values.put(key, (Long) value);
                } else {
                    throw new IllegalStateException("Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
                }
            }

            Filter filter;
            if (extras.containsKey(TOKEN_CUSTOM_CLASS)) {
                filter = new FilterWithCustomIntent(title, sql, values);
                Bundle customExtras = new Bundle();
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    if (AndroidUtilities.indexOf(CUSTOM_EXTRAS, key) < 0) {
                        AndroidUtilities.putInto(customExtras, key, extras.get(key));
                    }
                }

                ((FilterWithCustomIntent) filter).customExtras = customExtras; // Something
                ((FilterWithCustomIntent) filter).customTaskList = ComponentName.unflattenFromString(extras.getString(TOKEN_CUSTOM_CLASS));
            } else {
                filter = new Filter(title, sql, values);
            }
            taskListIntent.putExtra(TaskListFragment.TOKEN_FILTER, filter);
        }

        startActivity(taskListIntent);
        finish();
    }

    public static Intent createIntent(Context context, Filter filter) {
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);

        if(filter instanceof FilterWithCustomIntent) {
            FilterWithCustomIntent customFilter = ((FilterWithCustomIntent)filter);
            if(customFilter.customExtras != null) {
                shortcutIntent.putExtras(customFilter.customExtras);
            }
            shortcutIntent.putExtra(TOKEN_CUSTOM_CLASS, customFilter.customTaskList.flattenToString());
        }

        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(ShortcutActivity.TOKEN_FILTER_TITLE,
                filter.listingTitle);
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
        if (value instanceof String) {
            intent.putExtra(key, (String) value);
        } else if (value instanceof Integer) {
            intent.putExtra(key, (Integer) value);
        } else if (value instanceof Double) {
            intent.putExtra(key, (Double) value);
        } else if (value instanceof Long) {
            intent.putExtra(key, (Long) value);
        } else {
            throw new IllegalStateException(
                    "Unsupported bundle type " + value.getClass()); //$NON-NLS-1$
        }
    }
}
