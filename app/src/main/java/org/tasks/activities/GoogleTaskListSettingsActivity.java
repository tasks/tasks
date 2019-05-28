package org.tasks.activities;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.google.android.material.textfield.TextInputEditText;
import com.google.api.services.tasks.model.TaskList;
import com.rey.material.widget.ProgressView;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.MenuColorizer;
import timber.log.Timber;

public class GoogleTaskListSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String EXTRA_ACCOUNT = "extra_account";
  public static final String EXTRA_STORE_DATA = "extra_store_data";
  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final int REQUEST_COLOR_PICKER = 10109;
  @Inject @ForApplication Context context;
  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject GtasksListService gtasksListService;
  @Inject Tracker tracker;
  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject TaskDeleter taskDeleter;
  @Inject GtasksInvoker gtasksInvoker;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.color)
  TextInputEditText color;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.progress_bar)
  ProgressView progressView;

  private boolean isNewList;
  private GoogleTaskList gtasksList;
  private int selectedTheme;
  private CreateListViewModel createListViewModel;
  private RenameListViewModel renameListViewModel;
  private DeleteListViewModel deleteListViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_google_task_list_settings);
    ButterKnife.bind(this);

    createListViewModel = ViewModelProviders.of(this).get(CreateListViewModel.class);
    renameListViewModel = ViewModelProviders.of(this).get(RenameListViewModel.class);
    deleteListViewModel = ViewModelProviders.of(this).get(DeleteListViewModel.class);

    Intent intent = getIntent();
    gtasksList = intent.getParcelableExtra(EXTRA_STORE_DATA);
    if (gtasksList == null) {
      isNewList = true;
      gtasksList = new GoogleTaskList();
      GoogleTaskAccount account = intent.getParcelableExtra(EXTRA_ACCOUNT);
      gtasksList.setAccount(account.getAccount());
    }

    if (savedInstanceState == null) {
      selectedTheme = gtasksList.getColor();
    } else {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
    }

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setTitle(isNewList ? getString(R.string.new_list) : gtasksList.getTitle());
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
    toolbar.inflateMenu(R.menu.menu_tag_settings);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.showOverflowMenu();

    color.setInputType(InputType.TYPE_NULL);

    MenuColorizer.colorToolbar(this, toolbar);

    if (isNewList) {
      toolbar.getMenu().findItem(R.id.delete).setVisible(false);
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    } else {
      name.setText(gtasksList.getTitle());
    }

    updateTheme();

    if (createListViewModel.inProgress()
        || renameListViewModel.inProgress()
        || deleteListViewModel.inProgress()) {
      showProgressIndicator();
    }
    createListViewModel.observe(this, this::onListCreated, this::requestFailed);
    renameListViewModel.observe(this, this::onListRenamed, this::requestFailed);
    deleteListViewModel.observe(this, this::onListDeleted, this::requestFailed);
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
    Intent intent = new Intent(GoogleTaskListSettingsActivity.this, ColorPickerActivity.class);
    intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
    intent.putExtra(ColorPickerActivity.EXTRA_THEME_INDEX, selectedTheme);
    intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
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

    String newName = getNewName();

    if (isEmpty(newName)) {
      Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_LONG).show();
      return;
    }

    if (isNewList) {
      showProgressIndicator();
      createListViewModel.createList(gtasksInvoker, gtasksList.getAccount(), newName);
    } else if (nameChanged()) {
      showProgressIndicator();
      renameListViewModel.renameList(gtasksInvoker, gtasksList, newName);
    } else {
      if (colorChanged()) {
        gtasksList.setColor(selectedTheme);
        googleTaskListDao.insertOrReplace(gtasksList);
        setResult(
            RESULT_OK,
            new Intent(TaskListFragment.ACTION_RELOAD)
                .putExtra(MainActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
      }
      finish();
    }
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

  private void deleteTag() {
    if (requestInProgress()) {
      return;
    }

    dialogBuilder
        .newMessageDialog(R.string.delete_tag_confirmation, gtasksList.getTitle())
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> {
              showProgressIndicator();
              deleteListViewModel.deleteList(gtasksInvoker, gtasksList);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete:
        deleteTag();
        break;
    }
    return onOptionsItemSelected(item);
  }

  private void discard() {
    if (requestInProgress()) {
      return;
    }

    if (hasChanges()) {
      dialogBuilder
          .newMessageDialog(R.string.discard_changes)
          .setPositiveButton(R.string.keep_editing, null)
          .setNegativeButton(R.string.discard, (dialog, which) -> finish())
          .show();
    } else {
      finish();
    }
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  private boolean hasChanges() {
    if (isNewList) {
      return selectedTheme >= 0 || !isEmpty(getNewName());
    }
    return colorChanged() || nameChanged();
  }

  private boolean colorChanged() {
    return selectedTheme != gtasksList.getColor();
  }

  private boolean nameChanged() {
    return !getNewName().equals(gtasksList.getTitle());
  }

  private void onListCreated(TaskList taskList) {
    tracker.reportEvent(Tracking.Events.GTASK_NEW_LIST);
    gtasksList.setRemoteId(taskList.getId());
    gtasksList.setTitle(taskList.getTitle());
    gtasksList.setColor(selectedTheme);
    gtasksList.setId(googleTaskListDao.insertOrReplace(gtasksList));
    setResult(
        RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
    finish();
  }

  private void onListDeleted(boolean deleted) {
    if (deleted) {
      tracker.reportEvent(Tracking.Events.GTASK_DELETE_LIST);
      taskDeleter.delete(gtasksList);
      setResult(RESULT_OK, new Intent(TaskListFragment.ACTION_DELETED));
      finish();
    }
  }

  private void onListRenamed(TaskList taskList) {
    tracker.reportEvent(Tracking.Events.GTASK_RENAME_LIST);
    gtasksList.setTitle(taskList.getTitle());
    gtasksList.setColor(selectedTheme);
    googleTaskListDao.insertOrReplace(gtasksList);
    setResult(
        RESULT_OK,
        new Intent(TaskListFragment.ACTION_RELOAD)
            .putExtra(MainActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        tracker.reportEvent(Tracking.Events.GTASK_SET_COLOR, Integer.toString(index));
        selectedTheme = index;
        updateTheme();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void requestFailed(Throwable error) {
    Timber.e(error);
    hideProgressIndicator();
    Toast.makeText(this, R.string.gtasks_GLA_errorIOAuth, Toast.LENGTH_LONG).show();
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
}
