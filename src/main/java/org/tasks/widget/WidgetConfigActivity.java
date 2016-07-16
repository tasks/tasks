package org.tasks.widget;

import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.ThemePickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

import timber.log.Timber;

public class WidgetConfigActivity extends InjectingAppCompatActivity implements WidgetConfigDialog.WidgetConfigCallback, ThemePickerDialog.ThemePickerCallback, PurchaseHelperCallback {

    private static final String FRAG_TAG_WIDGET_CONFIG = "frag_tag_widget_config";

    private static final int REQUEST_PURCHASE = 10109;

    public static final int DEFAULT_OPACITY = 255;

    public static final String PREF_WIDGET_ID = "widget-id-";
    public static final String PREF_SHOW_DUE_DATE = "widget-show-due-date-";
    public static final String PREF_HIDE_CHECKBOXES = "widget-hide-checkboxes-";
    public static final String PREF_THEME = "widget-theme-v2-";
    public static final String PREF_COLOR = "widget-color-";
    public static final String PREF_HIDE_HEADER = "widget-hide-header-";
    public static final String PREF_WIDGET_OPACITY = "widget-opacity-v2-";

    @Inject Tracker tracker;
    @Inject DialogBuilder dialogBuilder;
    @Inject PurchaseHelper purchaseHelper;

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
    public void themePicked(ThemePickerDialog.ColorPalette palette, int index) {
        if (palette == ThemePickerDialog.ColorPalette.WIDGET_BACKGROUND) {
            widgetConfigDialog.setThemeIndex(index);
        } else {
            widgetConfigDialog.setColorIndex(index);
        }
    }

    @Override
    public void initiateThemePurchase() {
        purchaseHelper.purchase(dialogBuilder, this, getString(R.string.sku_themes), getString(R.string.p_purchased_themes), REQUEST_PURCHASE, this);
    }

    @Override
    public void purchaseCompleted(boolean success, final String sku) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getString(R.string.sku_themes).equals(sku)) {
                    showThemeSelection();
                } else {
                    Timber.d("Unhandled sku: %s", sku);
                }
            }
        });
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(this, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected void showThemeSelection() {
        widgetConfigDialog.showThemeSelection();
    }
}
