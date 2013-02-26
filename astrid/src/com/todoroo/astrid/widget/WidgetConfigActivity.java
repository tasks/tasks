/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.ThemeService;

@SuppressWarnings("nls")
abstract public class WidgetConfigActivity extends ListActivity {

    static final String PREF_TITLE = "widget-title-";
    static final String PREF_SQL = "widget-sql-";
    static final String PREF_VALUES = "widget-values-";
    static final String PREF_CUSTOM_INTENT = "widget-intent-";
    static final String PREF_CUSTOM_EXTRAS = "widget-extras-";
    static final String PREF_TAG_ID = "widget-tag-id-";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FilterAdapter adapter = null;

    public WidgetConfigActivity() {
        super();
    }

    abstract public void updateWidget();

    @Override
    public void onCreate(Bundle icicle) {
        ThemeService.applyTheme(this);
        ThemeService.setForceFilterInvert(true);
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
        adapter = new FilterAdapter(this, getListView(),
                R.layout.filter_adapter_row, true, true);
        adapter.filterStyle = R.style.TextAppearance_FLA_Filter_Widget;
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);

        StatisticsService.reportEvent(StatisticsConstants.WIDGET_CONFIG);
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            // Save configuration options
            saveConfiguration(adapter.getSelection());

            updateWidget();

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };



    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Filter item = adapter.getItem(position);
        adapter.setSelection(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsService.sessionStart(this);
        adapter.registerRecevier();
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsService.sessionPause();
        adapter.unregisterRecevier();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
        ThemeService.setForceFilterInvert(false);
    }

    private void saveConfiguration(FilterListItem filterListItem){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        String sql = null, contentValuesString = null, title = null;

        if(filterListItem != null && filterListItem instanceof Filter) {
            sql = ((Filter)filterListItem).getSqlQuery();
            ContentValues values = ((Filter)filterListItem).valuesForNewTasks;
            if(values != null)
                contentValuesString = AndroidUtilities.contentValuesToSerializedString(values);
            title = ((Filter)filterListItem).title;
        }

        Preferences.setString(WidgetConfigActivity.PREF_TITLE + mAppWidgetId, title);
        Preferences.setString(WidgetConfigActivity.PREF_SQL + mAppWidgetId, sql);
        Preferences.setString(WidgetConfigActivity.PREF_VALUES + mAppWidgetId, contentValuesString);

        if(filterListItem instanceof FilterWithCustomIntent) {
            String flattenedName = ((FilterWithCustomIntent)filterListItem).customTaskList.flattenToString();
            Preferences.setString(WidgetConfigActivity.PREF_CUSTOM_INTENT + mAppWidgetId,
                    flattenedName);
            String flattenedExtras = AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent)filterListItem).customExtras);
            if (flattenedExtras != null)
                Preferences.setString(WidgetConfigActivity.PREF_CUSTOM_EXTRAS + mAppWidgetId,
                        flattenedExtras);
        }
    }

}
