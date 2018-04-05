package org.tasks.caldav;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.CaldavFilter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class CaldavCalendarSettingsActivity extends ThemedInjectingAppCompatActivity {

  public static final String EXTRA_CALDAV_DATA = "caldavData"; // $NON-NLS-1$
  public static final String ACTION_RELOAD = "accountRenamed";
  public static final String ACTION_DELETED = "accountDeleted";
  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final int REQUEST_COLOR_PICKER = 10109;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject Tracker tracker;
  @Inject CaldavDao caldavDao;
  @Inject SyncAdapters syncAdapters;

  @BindView(R.id.root_layout)
  LinearLayout root;

  @BindView(R.id.color)
  TextInputEditText color;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  private CaldavCalendar caldavCalendar;
  private int selectedTheme;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_caldav_calendar_settings);

    ButterKnife.bind(this);

    caldavCalendar = getIntent().getParcelableExtra(EXTRA_CALDAV_DATA);

    if (savedInstanceState == null) {
      selectedTheme = caldavCalendar.getColor();
    } else {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
    }

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setTitle(caldavCalendar.getName());
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            this, backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
    toolbar.setNavigationOnClickListener(
        v -> {
          if (backButtonSavesTask) {
            discard();
          } else {
            save();
          }
        });

    color.setInputType(InputType.TYPE_NULL);

    updateTheme();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_THEME, selectedTheme);
  }

  @OnFocusChange(R.id.color)
  void onFocusChange(boolean focused) {
    if (focused) {
      color.clearFocus();
      showThemePicker();
    }
  }

  @OnClick(R.id.color)
  protected void showThemePicker() {
    Intent intent = new Intent(CaldavCalendarSettingsActivity.this, ColorPickerActivity.class);
    intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
    intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
    intent.putExtra(ColorPickerActivity.EXTRA_THEME_INDEX, selectedTheme);
    startActivityForResult(intent, REQUEST_COLOR_PICKER);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private void save() {
    if (hasChanges()) {
      updateAccount();
    } else {
      finish();
    }
  }

  private void updateAccount() {
    caldavCalendar.setColor(selectedTheme);
    caldavDao.update(caldavCalendar);
    setResult(
        RESULT_OK,
        new Intent(ACTION_RELOAD)
            .putExtra(TaskListActivity.OPEN_FILTER, new CaldavFilter(caldavCalendar)));
    finish();
  }

  private boolean hasChanges() {
    return selectedTheme != caldavCalendar.getColor();
  }

  @Override
  public void onBackPressed() {
    if (preferences.backButtonSavesTask()) {
      save();
    } else {
      discard();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        tracker.reportEvent(Tracking.Events.SET_TAG_COLOR, Integer.toString(index));
        selectedTheme = index;
        updateTheme();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void discard() {
    if (!hasChanges()) {
      finish();
    } else {
      dialogBuilder
          .newMessageDialog(R.string.discard_changes)
          .setPositiveButton(R.string.discard, (dialog, which) -> finish())
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  private void updateTheme() {
    ThemeColor themeColor;
    if (selectedTheme < 0) {
      themeColor = this.themeColor;
      color.setText(R.string.none);
    } else {
      themeColor = themeCache.getThemeColor(selectedTheme);
      color.setText(themeColor.getName());
    }
    themeColor.apply(toolbar);
    themeColor.applyToStatusBar(this);
  }
}
