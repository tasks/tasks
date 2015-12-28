/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.widget;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;

import org.tasks.R;
import org.tasks.filters.FilterCounter;
import org.tasks.filters.FilterProvider;
import org.tasks.injection.InjectingListActivity;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.widget.WidgetHelper;

import javax.inject.Inject;

public class WidgetConfigActivity extends InjectingListActivity {

    public static final String PREF_TITLE = "widget-title-";
    public static final String PREF_SQL = "widget-sql-";
    public static final String PREF_VALUES = "widget-values-";
    public static final String PREF_CUSTOM_INTENT = "widget-intent-";
    public static final String PREF_CUSTOM_EXTRAS = "widget-extras-";
    public static final String PREF_TAG_ID = "widget-tag-id-";
    public static final String PREF_SHOW_DUE_DATE = "widget-show-due-date-";
    public static final String PREF_HIDE_CHECKBOXES = "widget-hide-checkboxes-";
    public static final String PREF_DARK_THEME = "widget-dark-theme-";
    public static final String PREF_HIDE_HEADER = "widget-hide-header-";
    public static final String PREF_WIDGET_TRANSPARENT = "widget-transparent-";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FilterAdapter adapter = null;

    @Inject WidgetHelper widgetHelper;
    @Inject FilterCounter filterCounter;
    @Inject ActivityPreferences preferences;
    @Inject FilterProvider filterProvider;

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(mAppWidgetId, widgetHelper.createScrollableWidget(this, mAppWidgetId));
        TasksWidget.updateScrollableWidgets(this, new int[]{mAppWidgetId});
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        preferences.applyLightStatusBarColor();
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
        adapter = new FilterAdapter(filterProvider, filterCounter, this, getListView(), false);
        setListAdapter(adapter);

        Button button = (Button) findViewById(R.id.ok);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save configuration options
                CheckBox hideDueDate = (CheckBox) findViewById(R.id.hideDueDate);
                CheckBox darkTheme = (CheckBox) findViewById(R.id.darkTheme);
                CheckBox hideCheckboxes = (CheckBox) findViewById(R.id.hideCheckboxes);
                CheckBox hideHeader = (CheckBox) findViewById(R.id.hideHeader);
                CheckBox transparent = (CheckBox) findViewById(R.id.transparentBackground);
                saveConfiguration(adapter.getSelection(), !hideDueDate.isChecked(),
                        darkTheme.isChecked(), hideCheckboxes.isChecked(), hideHeader.isChecked(),
                        transparent.isChecked());

                updateWidget();

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        FilterListItem item = adapter.getItem(position);
        adapter.setSelection(item);
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

    private void saveConfiguration(FilterListItem filterListItem, boolean showDueDate,
                                   boolean darkTheme, boolean hideCheckboxes, boolean hideHeader,
                                   boolean transparent){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        String sql = null;
        String contentValuesString = null;
        String title = null;

        if(filterListItem != null && filterListItem instanceof Filter) {
            sql = ((Filter)filterListItem).getSqlQuery();
            ContentValues values = ((Filter)filterListItem).valuesForNewTasks;
            if(values != null) {
                contentValuesString = AndroidUtilities.contentValuesToSerializedString(values);
            }
            title = ((Filter)filterListItem).listingTitle;
        }

        preferences.setString(WidgetConfigActivity.PREF_TITLE + mAppWidgetId, title);
        preferences.setString(WidgetConfigActivity.PREF_SQL + mAppWidgetId, sql);
        preferences.setString(WidgetConfigActivity.PREF_VALUES + mAppWidgetId, contentValuesString);
        preferences.setBoolean(WidgetConfigActivity.PREF_SHOW_DUE_DATE + mAppWidgetId, showDueDate);
        preferences.setBoolean(WidgetConfigActivity.PREF_DARK_THEME + mAppWidgetId, darkTheme);
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_CHECKBOXES + mAppWidgetId, hideCheckboxes);
        preferences.setBoolean(WidgetConfigActivity.PREF_HIDE_HEADER + mAppWidgetId, hideHeader);
        preferences.setBoolean(WidgetConfigActivity.PREF_WIDGET_TRANSPARENT + mAppWidgetId, transparent);

        if(filterListItem instanceof FilterWithCustomIntent) {
            String flattenedName = ((FilterWithCustomIntent)filterListItem).customTaskList.flattenToString();
            preferences.setString(WidgetConfigActivity.PREF_CUSTOM_INTENT + mAppWidgetId,
                    flattenedName);
            String flattenedExtras = AndroidUtilities.bundleToSerializedString(((FilterWithCustomIntent)filterListItem).customExtras);
            if (flattenedExtras != null) {
                preferences.setString(WidgetConfigActivity.PREF_CUSTOM_EXTRAS + mAppWidgetId,
                        flattenedExtras);
            }
        }
    }
}
