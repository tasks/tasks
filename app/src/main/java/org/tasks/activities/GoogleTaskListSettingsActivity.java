package org.tasks.activities;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.api.services.tasks.model.TaskList;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.service.TaskDeleter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ApplicationContext;
import timber.log.Timber;

public class GoogleTaskListSettingsActivity extends BaseListSettingsActivity {

  public static final String EXTRA_ACCOUNT = "extra_account";
  public static final String EXTRA_STORE_DATA = "extra_store_data";
  @Inject @ApplicationContext Context context;
  @Inject GoogleTaskListDao googleTaskListDao;
  @Inject GtasksListService gtasksListService;
  @Inject TaskDeleter taskDeleter;
  @Inject GtasksInvoker gtasksInvoker;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.progress_bar)
  ProgressBar progressView;

  private boolean isNewList;
  private GoogleTaskList gtasksList;
  private CreateListViewModel createListViewModel;
  private RenameListViewModel renameListViewModel;
  private DeleteListViewModel deleteListViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Intent intent = getIntent();
    gtasksList = intent.getParcelableExtra(EXTRA_STORE_DATA);

    super.onCreate(savedInstanceState);

    ViewModelProvider provider = new ViewModelProvider(this);
    createListViewModel = provider.get(CreateListViewModel.class);
    renameListViewModel = provider.get(RenameListViewModel.class);
    deleteListViewModel = provider.get(DeleteListViewModel.class);

    if (gtasksList == null) {
      isNewList = true;
      gtasksList = new GoogleTaskList();
      GoogleTaskAccount account = intent.getParcelableExtra(EXTRA_ACCOUNT);
      gtasksList.setAccount(account.getAccount());
    }

    if (savedInstanceState == null) {
      selectedColor = gtasksList.getColor();
      selectedIcon = gtasksList.getIcon();
    }

    if (isNewList) {
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    } else {
      name.setText(gtasksList.getTitle());
    }

    if (createListViewModel.inProgress()
        || renameListViewModel.inProgress()
        || deleteListViewModel.inProgress()) {
      showProgressIndicator();
    }
    createListViewModel.observe(this, this::onListCreated, this::requestFailed);
    renameListViewModel.observe(this, this::onListRenamed, this::requestFailed);
    deleteListViewModel.observe(this, this::onListDeleted, this::requestFailed);

    updateTheme();
  }

  @Override
  protected boolean isNew() {
    return gtasksList == null;
  }

  @Override
  protected String getToolbarTitle() {
    return isNew() ? getString(R.string.new_list) : gtasksList.getTitle();
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
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void save() {
    if (requestInProgress()) {
      return;
    }

    String newName = getNewName();

    if (isNullOrEmpty(newName)) {
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
      if (colorChanged() || iconChanged()) {
        gtasksList.setColor(selectedColor);
        gtasksList.setIcon(selectedIcon);
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
  protected int getLayout() {
    return R.layout.activity_google_task_list_settings;
  }

  @Override
  protected void promptDelete() {
    if (!requestInProgress()) {
      super.promptDelete();
    }
  }

  @Override
  protected void delete() {
    showProgressIndicator();
    deleteListViewModel.deleteList(gtasksInvoker, gtasksList);
  }

  @Override
  protected void discard() {
    if (!requestInProgress()) {
      super.discard();
    }
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  @Override
  protected boolean hasChanges() {
    if (isNewList) {
      return selectedColor >= 0 || !isNullOrEmpty(getNewName());
    }
    return colorChanged() || nameChanged() || iconChanged();
  }

  private boolean colorChanged() {
    return selectedColor != gtasksList.getColor();
  }

  private boolean iconChanged() {
    return selectedIcon != gtasksList.getIcon();
  }

  private boolean nameChanged() {
    return !getNewName().equals(gtasksList.getTitle());
  }

  private void onListCreated(TaskList taskList) {
    gtasksList.setRemoteId(taskList.getId());
    gtasksList.setTitle(taskList.getTitle());
    gtasksList.setColor(selectedColor);
    gtasksList.setIcon(selectedIcon);
    gtasksList.setId(googleTaskListDao.insertOrReplace(gtasksList));
    setResult(
        RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
    finish();
  }

  private void onListDeleted(boolean deleted) {
    if (deleted) {
      taskDeleter.delete(gtasksList);
      setResult(RESULT_OK, new Intent(TaskListFragment.ACTION_DELETED));
      finish();
    }
  }

  private void onListRenamed(TaskList taskList) {
    gtasksList.setTitle(taskList.getTitle());
    gtasksList.setColor(selectedColor);
    gtasksList.setIcon(selectedIcon);
    googleTaskListDao.insertOrReplace(gtasksList);
    setResult(
        RESULT_OK,
        new Intent(TaskListFragment.ACTION_RELOAD)
            .putExtra(MainActivity.OPEN_FILTER, new GtasksFilter(gtasksList)));
    finish();
  }

  private void requestFailed(Throwable error) {
    Timber.e(error);
    hideProgressIndicator();
    Toast.makeText(this, R.string.gtasks_GLA_errorIOAuth, Toast.LENGTH_LONG).show();
  }
}
