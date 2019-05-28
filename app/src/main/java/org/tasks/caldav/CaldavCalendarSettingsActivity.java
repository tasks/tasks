package org.tasks.caldav;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import at.bitfire.dav4android.exception.HttpException;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rey.material.widget.ProgressView;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskDeleter;
import java.net.ConnectException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.CreateCalendarViewModel;
import org.tasks.activities.DeleteCalendarViewModel;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.analytics.Tracking.Events;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.DisplayableException;
import org.tasks.ui.MenuColorizer;

public class CaldavCalendarSettingsActivity extends ThemedInjectingAppCompatActivity
    implements OnMenuItemClickListener {

  public static final String EXTRA_CALDAV_CALENDAR = "extra_caldav_calendar";
  public static final String EXTRA_CALDAV_ACCOUNT = "extra_caldav_account";
  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final int REQUEST_COLOR_PICKER = 10109;
  @Inject @ForApplication Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject Tracker tracker;
  @Inject CaldavDao caldavDao;
  @Inject SyncAdapters syncAdapters;
  @Inject TaskDeleter taskDeleter;
  @Inject CaldavClient client;

  @BindView(R.id.root_layout)
  LinearLayout root;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.color)
  TextInputEditText color;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.progress_bar)
  ProgressView progressView;

  private CaldavCalendar caldavCalendar;
  private CaldavAccount caldavAccount;
  private int selectedTheme = -1;
  private CreateCalendarViewModel createCalendarViewModel;
  private DeleteCalendarViewModel deleteCalendarViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_caldav_calendar_settings);

    ButterKnife.bind(this);

    createCalendarViewModel = ViewModelProviders.of(this).get(CreateCalendarViewModel.class);
    deleteCalendarViewModel = ViewModelProviders.of(this).get(DeleteCalendarViewModel.class);

    Intent intent = getIntent();
    caldavCalendar = intent.getParcelableExtra(EXTRA_CALDAV_CALENDAR);
    if (caldavCalendar == null) {
      caldavAccount = intent.getParcelableExtra(EXTRA_CALDAV_ACCOUNT);
    } else {
      caldavAccount = caldavDao.getAccountByUuid(caldavCalendar.getAccount());
      nameLayout.setVisibility(View.GONE);
    }
    caldavAccount =
        caldavCalendar == null
            ? intent.getParcelableExtra(EXTRA_CALDAV_ACCOUNT)
            : caldavDao.getAccountByUuid(caldavCalendar.getAccount());

    if (savedInstanceState == null) {
      if (caldavCalendar != null) {
        name.setText(caldavCalendar.getName());
        selectedTheme = caldavCalendar.getColor();
      }
    } else {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
    }

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setTitle(
        caldavCalendar == null ? getString(R.string.new_list) : caldavCalendar.getName());
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            this,
            backButtonSavesTask
                ? R.drawable.ic_outline_clear_24px
                : R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(
        v -> {
          if (backButtonSavesTask) {
            discard();
          } else {
            save();
          }
        });
    if (caldavCalendar != null) {
      toolbar.inflateMenu(R.menu.menu_caldav_calendar_settings);
    }
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(this, toolbar);

    color.setInputType(InputType.TYPE_NULL);

    updateTheme();

    if (caldavCalendar == null) {
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    }

    createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed);
    deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_THEME, selectedTheme);
  }

  @OnTextChanged(R.id.name)
  void onNameChanged(CharSequence text) {
    nameLayout.setError(null);
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
    if (requestInProgress()) {
      return;
    }

    String name = getNewName();

    boolean failed = false;

    if (isEmpty(name)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      failed = true;
    } else {
      CaldavCalendar calendarByName = caldavDao.getCalendar(caldavAccount.getUuid(), name);
      if (calendarByName != null && !calendarByName.equals(caldavCalendar)) {
        nameLayout.setError(getString(R.string.duplicate_name));
        failed = true;
      }
    }

    if (failed) {
      return;
    }

    if (caldavCalendar == null) {
      showProgressIndicator();
      createCalendarViewModel.createCalendar(client, caldavAccount, name);
    } else if (hasChanges()) {
      updateAccount();
    } else {
      finish();
    }
  }

  private void showProgressIndicator() {
    progressView.setVisibility(View.VISIBLE);
  }

  private void hideProgressIndicator() {
    progressView.setVisibility(View.GONE);
  }

  private boolean requestInProgress() {
    return progressView.getVisibility() == View.VISIBLE;
  }

  private void requestFailed(Throwable t) {
    hideProgressIndicator();

    if (t instanceof HttpException) {
      showSnackbar(t.getMessage());
    } else if (t instanceof DisplayableException) {
      showSnackbar(((DisplayableException) t).getResId());
    } else if (t instanceof ConnectException) {
      showSnackbar(R.string.network_error);
    } else {
      showGenericError();
    }
  }

  private void showGenericError() {
    showSnackbar(R.string.error_adding_account);
  }

  private void showSnackbar(int resId) {
    showSnackbar(getString(resId));
  }

  private void showSnackbar(String message) {
    Snackbar snackbar =
        Snackbar.make(root, message, 8000)
            .setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color));
    snackbar
        .getView()
        .setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_background));
    snackbar.show();
  }

  private void createSuccessful(String url) {
    CaldavCalendar caldavCalendar = new CaldavCalendar();
    caldavCalendar.setUuid(UUIDHelper.newUUID());
    caldavCalendar.setAccount(caldavAccount.getUuid());
    caldavCalendar.setUrl(url);
    caldavCalendar.setName(getNewName());
    caldavCalendar.setColor(selectedTheme);
    caldavCalendar.setId(caldavDao.insert(caldavCalendar));
    tracker.reportEvent(Events.CALDAV_LIST_ADDED);
    setResult(
        RESULT_OK,
        new Intent().putExtra(MainActivity.OPEN_FILTER, new CaldavFilter(caldavCalendar)));
    finish();
  }

  private void updateAccount() {
    caldavCalendar.setName(getNewName());
    caldavCalendar.setColor(selectedTheme);
    caldavDao.update(caldavCalendar);
    setResult(
        RESULT_OK,
        new Intent(TaskListFragment.ACTION_RELOAD)
            .putExtra(MainActivity.OPEN_FILTER, new CaldavFilter(caldavCalendar)));
    finish();
  }

  private boolean hasChanges() {
    if (caldavCalendar == null) {
      return !isEmpty(getNewName()) || selectedTheme != -1;
    }
    return !caldavCalendar.getName().equals(getNewName())
        || selectedTheme != caldavCalendar.getColor();
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
    super.finish();
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
    if (requestInProgress()) {
      return;
    }

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
    themeColor.applyToSystemBars(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete:
        deleteCollection();
        break;
    }
    return onOptionsItemSelected(item);
  }

  private void deleteCollection() {
    if (requestInProgress()) {
      return;
    }

    dialogBuilder
        .newMessageDialog(R.string.delete_tag_confirmation, caldavCalendar.getName())
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> {
              showProgressIndicator();
              deleteCalendarViewModel.deleteCalendar(client, caldavAccount, caldavCalendar);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void onDeleted(boolean deleted) {
    if (deleted) {
      taskDeleter.delete(caldavCalendar);
      tracker.reportEvent(Events.CALDAV_LIST_DELETED);
      setResult(RESULT_OK, new Intent(TaskListFragment.ACTION_DELETED));
      finish();
    }
  }
}
