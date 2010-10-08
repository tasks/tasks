package com.todoroo.astrid.widget;

import android.app.ExpandableListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.service.StatisticsService;

@SuppressWarnings("nls")
abstract public class WidgetConfigActivity extends ExpandableListActivity {

    static final String PREF_COLOR = "widget-color-";
    static final String PREF_ENABLE_CALENDAR = "widget-enableCalendar-";
    static final String PREF_ENCOURAGEMENTS = "widget-enableEncouragements-";
    static final String PREF_TITLE = "widget-title-";
    static final String PREF_SQL = "widget-sql-";
    static final String PREF_VALUES = "widget-values-";


    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FilterAdapter adapter = null;

    @SuppressWarnings("nls")
    public String[] COLORS = new String[] {
            "Black",
            "White",
            "Blue",
            "Red"
    };

    public WidgetConfigActivity() {
        super();
    }

    abstract public void updateWidget();

    /** whether to show 'hide encouragements' */
    public boolean showEncouragementSetting() {
        return false;
    }

    /** whether to show color selection */
    public boolean showColorSelectionSetting() {
        return false;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.widget_config_activity);

        setTitle(R.string.PPW_configure_title);

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

        if(!showColorSelectionSetting())
            findViewById(R.id.colors).setVisibility(View.GONE);

        if(!showEncouragementSetting())
            findViewById(R.id.PPW_disable_encouragements).setVisibility(View.GONE);

        // set up ui
        adapter = new FilterAdapter(this, getExpandableListView(),
                R.layout.filter_adapter_row, true);
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);

        StatisticsService.reportEvent("widget-config"); //$NON-NLS-1$
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            Spinner colorSpinner = (Spinner) findViewById(R.id.PPW_color);
            int colorPos = colorSpinner.getSelectedItemPosition();
            String color = COLORS[colorPos];

            boolean enableCalendar = false;

            CheckBox chk_disableEncouragements = (CheckBox) findViewById(R.id.PPW_disable_encouragements);
            boolean disableEncouragements = chk_disableEncouragements.isChecked();
            if(!showEncouragementSetting())
                disableEncouragements = true;

            // Save configuration options
            saveConfiguration(adapter.getSelection(), color, enableCalendar, disableEncouragements);

            updateWidget();

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
        StatisticsService.sessionStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatisticsService.sessionStop(this);
    }

    private void saveConfiguration(FilterListItem filterListItem, String color, boolean enableCalendar, boolean disableEncouragements){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        String sql = null, contentValuesString = null, title = null;

        if(filterListItem != null && filterListItem instanceof Filter) {
            sql = ((Filter)filterListItem).sqlQuery;
            ContentValues values = ((Filter)filterListItem).valuesForNewTasks;
            if(values != null)
                contentValuesString = AndroidUtilities.contentValuesToSerializedString(values);
            title = ((Filter)filterListItem).title;
        }

        Preferences.setString(WidgetConfigActivity.PREF_TITLE + mAppWidgetId, title);
        Preferences.setString(WidgetConfigActivity.PREF_SQL + mAppWidgetId, sql);
        Preferences.setString(WidgetConfigActivity.PREF_VALUES + mAppWidgetId, contentValuesString);

        Preferences.setString(WidgetConfigActivity.PREF_COLOR + mAppWidgetId, color);
        Preferences.setBoolean(WidgetConfigActivity.PREF_ENABLE_CALENDAR + mAppWidgetId, enableCalendar);
        Preferences.setBoolean(WidgetConfigActivity.PREF_ENCOURAGEMENTS + mAppWidgetId, !disableEncouragements);
    }

}
