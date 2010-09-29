package com.todoroo.astrid.widget;

import android.app.ExpandableListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.andlib.utility.Preferences;

/**
 * Configure options for the Power Pack widget.  Select a color, filter to use, enable/disable encouragements.
 *
 * @author jwong
 *
 */
public class ConfigurePowerWidgetActivity extends ExpandableListActivity {


    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    FilterAdapter adapter = null;

    String[] colors = new String[]{
            "Black",
            "Blue",
            "Red",
            "White"
    };

    public ConfigurePowerWidgetActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED);

        // Set the view layout resource to use.
        setContentView(R.layout.power_widget_configure);

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

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, colors);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner colorSpinner = (Spinner) findViewById(R.id.PPW_color);
        colorSpinner.setAdapter(colorAdapter);

        // set up ui
        adapter = new FilterAdapter(this, getExpandableListView(),
                R.layout.filter_adapter_row, true);
        setListAdapter(adapter);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(mOnClickListener);

        FlurryAgent.onEvent("power-widget-config"); //$NON-NLS-1$
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = ConfigurePowerWidgetActivity.this;


            Spinner colorSpinner = (Spinner) findViewById(R.id.PPW_color);
            int colorPos = colorSpinner.getSelectedItemPosition();
            String color = colors[colorPos];

            // removed calendar option
//            CheckBox chk_enableCalendar = (CheckBox) findViewById(R.id.PPW_enable_calendar);
//            boolean enableCalendar = chk_enableCalendar.isChecked();
            boolean enableCalendar = false;

            CheckBox chk_disableEncouragements = (CheckBox) findViewById(R.id.PPW_disable_encouragements);
            boolean disableEncouragements = chk_disableEncouragements.isChecked();

            // Save configuration options
            saveConfiguration(adapter.getSelection(), color, enableCalendar, !disableEncouragements);

            // Push widget update to surface with newly set prefix
            PowerWidget.updateAppWidget(context, mAppWidgetId);

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

    private void saveConfiguration(FilterListItem filterListItem, String color, boolean enableCalendar, boolean enableEncouragements){
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

        Preferences.setString(PowerWidget.PREF_TITLE + mAppWidgetId, title);
        Preferences.setString(PowerWidget.PREF_SQL + mAppWidgetId, sql);
        Preferences.setString(PowerWidget.PREF_VALUES + mAppWidgetId, contentValuesString);

        Preferences.setString(PowerWidget.PREF_COLOR + mAppWidgetId, color);
        Preferences.setBoolean(PowerWidget.PREF_ENABLE_CALENDAR + mAppWidgetId, enableCalendar);
        Preferences.setBoolean(PowerWidget.PREF_ENCOURAGEMENTS + mAppWidgetId, enableEncouragements);
    }
}
