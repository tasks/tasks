package org.tasks.widget;

import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Theme;

import javax.inject.Inject;

public abstract class BaseWidgetConfigActivity extends InjectingAppCompatActivity implements WidgetConfigDialog.WidgetConfigCallback, ThemePickerDialog.ThemePickerCallback {

    private static final String FRAG_TAG_WIDGET_CONFIG = "frag_tag_widget_config";

    public static final int DEFAULT_OPACITY = 100;

    public static final String PREF_WIDGET_ID = "widget-id-";
    public static final String PREF_SHOW_DUE_DATE = "widget-show-due-date-";
    public static final String PREF_HIDE_CHECKBOXES = "widget-hide-checkboxes-";
    @Deprecated public static final String PREF_DARK_THEME = "widget-dark-theme-";
    public static final String PREF_THEME = "widget-theme-";
    public static final String PREF_HIDE_HEADER = "widget-hide-header-";
    public static final String PREF_WIDGET_OPACITY = "widget-opacity-";

    @Inject Tracker tracker;
    @Inject DialogBuilder dialogBuilder;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetConfigDialog widgetConfigDialog;

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
            FragmentManager fragmentManager = getFragmentManager();
            widgetConfigDialog = (WidgetConfigDialog) fragmentManager.findFragmentByTag(FRAG_TAG_WIDGET_CONFIG);
            if (widgetConfigDialog == null) {
                widgetConfigDialog = WidgetConfigDialog.newWidgetConfigDialog(appWidgetId);
                widgetConfigDialog.show(fragmentManager, FRAG_TAG_WIDGET_CONFIG);
            }
        }
    }

    @Override
    public void ok() {
        tracker.reportEvent(Tracking.Events.WIDGET_ADD, getString(R.string.app_name));
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public void themePicked(Theme theme) {
        widgetConfigDialog.setTheme(theme);
    }

    protected void showThemeSelection() {
        widgetConfigDialog.showThemeSelection();
    }
}
