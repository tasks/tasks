package org.tasks.preferences;

import static org.tasks.dialogs.SeekBarDialog.newSeekBarDialog;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.dialogs.SeekBarDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.locale.Locale;

public class AppearancePreferences extends InjectingPreferenceActivity
    implements SeekBarDialog.SeekBarCallback {

  public static final String EXTRA_RESTART = "extra_restart";
  private static final String EXTRA_FILTERS_CHANGED = "extra_filters_changed";
  private static final int REQUEST_CUSTOMIZE = 1004;
  private static final int REQUEST_DEFAULT_LIST = 1005;
  private static final int REQUEST_ROW_PADDING = 1006;
  private static final int REQUEST_FONT_SIZE = 1007;
  private static final String FRAG_TAG_ROW_PADDING_SEEKBAR = "frag_tag_row_padding_seekbar";
  private static final String FRAG_TAG_FONT_SIZE_SEEKBAR = "frag_tag_font_size_seekbar";
  private static final String EXTRA_BUNDLE = "extra_bundle";
  @Inject Preferences preferences;
  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject Tracker tracker;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Locale locale;

  private Bundle result;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_BUNDLE);

    addPreferencesFromResource(R.xml.preferences_appearance);

    setExtraOnChange(R.string.p_fontSize, EXTRA_RESTART);
    setExtraOnChange(R.string.p_rowPadding, EXTRA_RESTART);
    setExtraOnChange(R.string.p_fullTaskTitle, EXTRA_RESTART);
    setExtraOnChange(R.string.p_show_today_filter, EXTRA_FILTERS_CHANGED);
    setExtraOnChange(R.string.p_show_recently_modified_filter, EXTRA_FILTERS_CHANGED);
    setExtraOnChange(R.string.p_show_not_in_list_filter, EXTRA_FILTERS_CHANGED);
    findPreference(getString(R.string.customize_edit_screen))
        .setOnPreferenceClickListener(
            preference -> {
              startActivityForResult(
                  new Intent(AppearancePreferences.this, BeastModePreferences.class),
                  REQUEST_CUSTOMIZE);
              return true;
            });

    findPreference(R.string.p_fontSize)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                      R.layout.dialog_font_size_seekbar,
                      10,
                      48,
                      preferences.getFontSize(),
                      REQUEST_FONT_SIZE)
                  .show(getFragmentManager(), FRAG_TAG_FONT_SIZE_SEEKBAR);
              return false;
            });
    updateFontSize();

    findPreference(R.string.p_rowPadding)
        .setOnPreferenceClickListener(
            preference -> {
              newSeekBarDialog(
                      R.layout.dialog_font_size_seekbar,
                      0,
                      16,
                      preferences.getRowPadding(),
                      REQUEST_ROW_PADDING)
                  .show(getFragmentManager(), FRAG_TAG_ROW_PADDING_SEEKBAR);
              return false;
            });
    updateRowPadding();
    Preference defaultList = findPreference(getString(R.string.p_default_list));
    Filter filter = defaultFilterProvider.getDefaultFilter();
    defaultList.setSummary(filter.listingTitle);
    defaultList.setOnPreferenceClickListener(
        preference -> {
          Intent intent = new Intent(AppearancePreferences.this, FilterSelectionActivity.class);
          intent.putExtra(
              FilterSelectionActivity.EXTRA_FILTER, defaultFilterProvider.getDefaultFilter());
          intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
          startActivityForResult(intent, REQUEST_DEFAULT_LIST);
          return true;
        });
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBundle(EXTRA_BUNDLE, result);
  }

  @Override
  public void finish() {
    Intent data = new Intent();
    data.putExtras(result);
    setResult(RESULT_OK, data);
    super.finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CUSTOMIZE) {
      if (resultCode == RESULT_OK) {
        result.putBoolean(EXTRA_RESTART, true);
      }
    } else if (requestCode == REQUEST_DEFAULT_LIST) {
      if (resultCode == RESULT_OK) {
        Filter filter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        defaultFilterProvider.setDefaultFilter(filter);
        findPreference(getString(R.string.p_default_list)).setSummary(filter.listingTitle);
        localBroadcastManager.broadcastRefresh();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void setExtraOnChange(final int resId, final String extra) {
    findPreference(getString(resId))
        .setOnPreferenceChangeListener(
            (preference, newValue) -> {
              tracker.reportEvent(Tracking.Events.SET_PREFERENCE, resId, newValue.toString());
              result.putBoolean(extra, true);
              return true;
            });
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void valueSelected(int value, int requestCode) {
    int resId = 0;
    if (requestCode == REQUEST_ROW_PADDING) {
      preferences.setInt(R.string.p_rowPadding, value);
      updateRowPadding();
      resId = R.string.p_rowPadding;
    } else if (requestCode == REQUEST_FONT_SIZE) {
      preferences.setInt(R.string.p_fontSize, value);
      updateFontSize();
      resId = R.string.p_fontSize;
    }
    if (resId > 0) {
      result.putBoolean(EXTRA_RESTART, true);
      tracker.reportEvent(Tracking.Events.SET_PREFERENCE, resId, Integer.toString(value));
    }
  }

  private void updateFontSize() {
    findPreference(R.string.p_fontSize).setSummary(locale.formatNumber(preferences.getFontSize()));
  }

  private void updateRowPadding() {
    findPreference(R.string.p_rowPadding)
        .setSummary(locale.formatNumber(preferences.getRowPadding()));
  }
}
