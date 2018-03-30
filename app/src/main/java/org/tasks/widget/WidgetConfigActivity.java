package org.tasks.widget;

import static org.tasks.dialogs.SeekBarDialog.newSeekBarDialog;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.SeekBarDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.Locale;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.themes.WidgetTheme;

public class WidgetConfigActivity extends InjectingPreferenceActivity
    implements SeekBarDialog.SeekBarCallback {

  private static final String FRAG_TAG_OPACITY_SEEKBAR = "frag_tag_opacity_seekbar";
  private static final String FRAG_TAG_FONT_SIZE_SEEKBAR = "frag_tag_font_size_seekbar";

  private static final int REQUEST_FILTER = 1005;
  private static final int REQUEST_THEME_SELECTION = 1006;
  private static final int REQUEST_COLOR_SELECTION = 1007;
  private static final int REQUEST_OPACITY = 1008;
  private static final int REQUEST_FONT_SIZE = 1009;

  @Inject Tracker tracker;
  @Inject DialogBuilder dialogBuilder;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Preferences preferences;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject ThemeCache themeCache;
  @Inject Locale locale;

  private WidgetPreferences widgetPreferences;
  private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_widget);

    appWidgetId =
        getIntent()
            .getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

    // If they gave us an intent without the widget id, just bail.
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish();
      return;
    }
    widgetPreferences = new WidgetPreferences(this, preferences, appWidgetId);
    Intent data = new Intent();
    data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
    setResult(RESULT_OK, data);

    setupCheckbox(R.string.p_widget_show_due_date);
    setupCheckbox(R.string.p_widget_show_checkboxes);
    CheckBoxPreference showHeader = setupCheckbox(R.string.p_widget_show_header);
    CheckBoxPreference showSettings = setupCheckbox(R.string.p_widget_show_settings);
    showSettings.setDependency(showHeader.getKey());

    findPreference(R.string.p_widget_filter)
        .setOnPreferenceClickListener(
            preference -> {
              Intent intent = new Intent(WidgetConfigActivity.this, FilterSelectionActivity.class);
              intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, getFilter());
              intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
              startActivityForResult(intent, REQUEST_FILTER);
              return false;
            });

    findPreference(R.string.p_widget_theme)
        .setOnPreferenceClickListener(
            preference -> {
              Intent intent = new Intent(WidgetConfigActivity.this, ColorPickerActivity.class);
              intent.putExtra(
                  ColorPickerActivity.EXTRA_PALETTE,
                  ColorPickerActivity.ColorPalette.WIDGET_BACKGROUND);
              intent.putExtra(
                  ColorPickerActivity.EXTRA_THEME_INDEX, widgetPreferences.getThemeIndex());
              startActivityForResult(intent, REQUEST_THEME_SELECTION);
              return false;
            });

    Preference colorPreference = findPreference(R.string.p_widget_color);
    colorPreference.setDependency(showHeader.getKey());
    colorPreference.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(WidgetConfigActivity.this, ColorPickerActivity.class);
          intent.putExtra(
              ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
          intent.putExtra(ColorPickerActivity.EXTRA_THEME_INDEX, widgetPreferences.getColorIndex());
          startActivityForResult(intent, REQUEST_COLOR_SELECTION);
          return false;
        });

    findPreference(R.string.p_widget_opacity)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                      R.layout.dialog_opacity_seekbar,
                      0,
                      100,
                      widgetPreferences.getOpacity(),
                      REQUEST_OPACITY)
                  .show(getFragmentManager(), FRAG_TAG_OPACITY_SEEKBAR);
              return false;
            });

    findPreference(R.string.p_widget_font_size)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                      R.layout.dialog_font_size_seekbar,
                      10,
                      22,
                      widgetPreferences.getFontSize(),
                      REQUEST_FONT_SIZE)
                  .show(getFragmentManager(), FRAG_TAG_FONT_SIZE_SEEKBAR);
              return false;
            });

    updateFilter();
    updateOpacity();
    updateFontSize();
    updateTheme();
    updateColor();
  }

  private CheckBoxPreference setupCheckbox(int resId) {
    CheckBoxPreference preference = (CheckBoxPreference) findPreference(resId);
    String key = getString(resId) + appWidgetId;
    preference.setKey(key);
    preference.setChecked(preferences.getBoolean(key, true));
    return preference;
  }

  private void updateOpacity() {
    int opacity = widgetPreferences.getOpacity();
    findPreference(R.string.p_widget_opacity).setSummary(locale.formatPercentage(opacity));
  }

  private void updateFontSize() {
    int fontSize = widgetPreferences.getFontSize();
    findPreference(R.string.p_widget_font_size).setSummary(locale.formatNumber(fontSize));
  }

  private void updateFilter() {
    findPreference(R.string.p_widget_filter).setSummary(getFilter().listingTitle);
  }

  private Filter getFilter() {
    return defaultFilterProvider.getFilterFromPreference(widgetPreferences.getFilterId());
  }

  private void updateTheme() {
    WidgetTheme widgetTheme = themeCache.getWidgetTheme(widgetPreferences.getThemeIndex());
    findPreference(R.string.p_widget_theme).setSummary(widgetTheme.getName());
  }

  private void updateColor() {
    ThemeColor themeColor = themeCache.getThemeColor(widgetPreferences.getColorIndex());
    findPreference(R.string.p_widget_color).setSummary(themeColor.getName());
  }

  @Override
  protected void onPause() {
    super.onPause();

    localBroadcastManager.broadcastRefresh();
    // force update after setting preferences
    Intent intent = new Intent(this, TasksWidget.class);
    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
    sendBroadcast(intent);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_FILTER) {
      if (resultCode == RESULT_OK) {
        Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        widgetPreferences.setFilter(defaultFilterProvider.getFilterPreferenceValue(filter));
        updateFilter();
      }
    } else if (requestCode == REQUEST_THEME_SELECTION) {
      if (resultCode == RESULT_OK) {
        widgetPreferences.setTheme(data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0));
        updateTheme();
      }
    } else if (requestCode == REQUEST_COLOR_SELECTION) {
      if (resultCode == RESULT_OK) {
        widgetPreferences.setColor(data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0));
        updateColor();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void valueSelected(int value, int requestCode) {
    if (requestCode == REQUEST_OPACITY) {
      widgetPreferences.setOpacity(value);
      updateOpacity();
    } else if (requestCode == REQUEST_FONT_SIZE) {
      widgetPreferences.setFontSize(value);
      updateFontSize();
    }
  }
}
