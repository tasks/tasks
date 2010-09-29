package com.todoroo.astrid.widget;

import android.app.ExpandableListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.andlib.utility.Preferences;

public class WidgetConfigActivity extends ExpandableListActivity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FilterAdapter adapter = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.widget_config_activity);

        setTitle(R.string.WCA_title);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // set up ui
        adapter = new FilterAdapter(this, getExpandableListView(),
                R.layout.filter_adapter_row, true);
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);

        FlurryAgent.onEvent("widget-config"); //$NON-NLS-1$
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            // Save configuration options
            saveConfiguration(adapter.getSelection());

            // Push widget update to surface with newly set prefix
            TasksWidget.updateWidget(WidgetConfigActivity.this, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
        FilterListItem item = (FilterListItem) adapter.getChild(groupPosition,
                childPosition);
        if(item instanceof Filter) {
            adapter.setSelection(item);
        }
        return true;
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        if(item instanceof Filter)
            adapter.setSelection(item);
        else if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, true);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        FilterListItem item = (FilterListItem) adapter.getGroup(groupPosition);
        if(item instanceof Filter)
            adapter.setSelection(item);
        else if(item instanceof FilterCategory)
            adapter.saveExpansionSetting((FilterCategory) item, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.registerRecevier();
    }

    @Override
    protected void onPause() {
        super.onPause();
        adapter.unregisterRecevier();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    private void saveConfiguration(FilterListItem filterListItem) {
        String sql = null, contentValuesString = null, title = null;

        if(filterListItem != null && filterListItem instanceof Filter) {
            sql = ((Filter)filterListItem).sqlQuery;
            ContentValues values = ((Filter)filterListItem).valuesForNewTasks;
            if(values != null)
                contentValuesString = AndroidUtilities.contentValuesToSerializedString(values);
            title = ((Filter)filterListItem).title;
        }

        Preferences.setString(TasksWidget.PREF_TITLE + mAppWidgetId, title);
        Preferences.setString(TasksWidget.PREF_SQL + mAppWidgetId, sql);
        Preferences.setString(TasksWidget.PREF_VALUES + mAppWidgetId, contentValuesString);

        FlurryAgent.onEvent("widget-config"); //$NON-NLS-1$
    }
}
