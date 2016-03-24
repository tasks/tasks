package org.tasks.widget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class WidgetConfigActivity extends InjectingAppCompatActivity implements WidgetConfigDialog.WidgetConfigCallback {

    private static final String FRAG_TAG_WIDGET_CONFIG = "frag_tag_widget_config";

    public static final int DEFAULT_OPACITY = 100;

    public static final String PREF_WIDGET_ID = "widget-id-";
    public static final String PREF_SHOW_DUE_DATE = "widget-show-due-date-";
    public static final String PREF_HIDE_CHECKBOXES = "widget-hide-checkboxes-";
    public static final String PREF_DARK_THEME = "widget-dark-theme-";
    public static final String PREF_HIDE_HEADER = "widget-hide-header-";
    public static final String PREF_WIDGET_OPACITY = "widget-opacity-";

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Inject ActivityPreferences preferences;
    @Inject Tracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        } else {
            preferences.applyDialogTheme();

            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = fragmentManager.findFragmentByTag(FRAG_TAG_WIDGET_CONFIG);
            if (fragment == null) {
                WidgetConfigDialog widgetConfigDialog = WidgetConfigDialog.newWidgetConfigDialog(appWidgetId);
                widgetConfigDialog.show(fragmentManager, FRAG_TAG_WIDGET_CONFIG);
            }
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void ok() {
        tracker.reportEvent(Tracking.Events.WIDGET_ADD_SCROLLABLE);
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
    }

    @Override
    public void done() {
        finish();
    }
}
