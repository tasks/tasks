package org.tasks.locale.ui.activity;

import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.textfield.TextInputEditText;
import javax.inject.Inject;
import net.dinglisch.android.tasker.TaskerPlugin;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.TaskCreationBundle;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

public final class TaskerCreateTaskActivity extends AbstractFragmentPluginAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final String FRAG_TAG_PURCHASE = "frag_tag_purchase";

  @Inject Preferences preferences;
  @Inject Inventory inventory;
  @Inject LocalBroadcastManager localBroadcastManager;

  @BindView(R.id.title)
  TextInputEditText title;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.due_date)
  TextInputEditText dueDate;

  @BindView(R.id.due_time)
  TextInputEditText dueTime;

  @BindView(R.id.priority)
  TextInputEditText priority;

  @BindView(R.id.description)
  TextInputEditText description;

  private Bundle previousBundle;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_tasker_create);

    ButterKnife.bind(this);

    toolbar.setTitle(R.string.tasker_create_task);
    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            this,
            backButtonSavesTask
                ? R.drawable.ic_outline_clear_24px
                : R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(
        v -> {
          if (backButtonSavesTask) {
            discardButtonClick();
          } else {
            save();
          }
        });
    toolbar.setOnMenuItemClickListener(this);
    toolbar.inflateMenu(R.menu.menu_tasker_create_task);
    MenuColorizer.colorToolbar(this, toolbar);

    if (savedInstanceState != null) {
      previousBundle = savedInstanceState.getParcelable(TaskCreationBundle.EXTRA_BUNDLE);
      TaskCreationBundle bundle = new TaskCreationBundle(previousBundle);
      title.setText(bundle.getTitle());
    }

    if (!inventory.purchasedTasker()) {
      showPurchaseDialog();
    }
  }

  private void showPurchaseDialog() {
    newPurchaseDialog().show(getSupportFragmentManager(), FRAG_TAG_PURCHASE);
  }

  @Override
  public void onPostCreateWithPreviousResult(
      final Bundle previousBundle, final String previousBlurb) {
    this.previousBundle = previousBundle;
    TaskCreationBundle bundle = new TaskCreationBundle(previousBundle);
    title.setText(bundle.getTitle());
    dueDate.setText(bundle.getDueDate());
    dueTime.setText(bundle.getDueTime());
    priority.setText(bundle.getPriority());
    description.setText(bundle.getDescription());
  }

  @Override
  public boolean isBundleValid(final Bundle bundle) {
    return TaskCreationBundle.isBundleValid(bundle);
  }

  @Override
  protected Bundle getResultBundle() {
    TaskCreationBundle bundle = new TaskCreationBundle();
    bundle.setTitle(title.getText().toString().trim());
    bundle.setDueDate(dueDate.getText().toString().trim());
    bundle.setDueTime(dueTime.getText().toString().trim());
    bundle.setPriority(priority.getText().toString().trim());
    bundle.setDescription(description.getText().toString().trim());
    Bundle resultBundle = bundle.build();
    if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this)) {
      TaskerPlugin.Setting.setVariableReplaceKeys(
          resultBundle,
          new String[] {
            TaskCreationBundle.EXTRA_TITLE,
            TaskCreationBundle.EXTRA_DUE_DATE,
            TaskCreationBundle.EXTRA_DUE_TIME,
            TaskCreationBundle.EXTRA_PRIORITY,
            TaskCreationBundle.EXTRA_DESCRIPTION
          });
    }
    return resultBundle;
  }

  @Override
  public String getResultBlurb(final Bundle bundle) {
    return title.getText().toString().trim();
  }

  @Override
  public void onBackPressed() {
    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    if (backButtonSavesTask) {
      save();
    } else {
      discardButtonClick();
    }
  }

  private void save() {
    if (!inventory.purchasedTasker()) {
      showPurchaseDialog();
    } else {
      finish();
    }
  }

  private void discardButtonClick() {
    mIsCancelled = true;
    finish();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(TaskCreationBundle.EXTRA_BUNDLE, previousBundle);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_help) {
      startActivity(
          new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://tasks.org/help/tasker")));
      return true;
    }
    return onOptionsItemSelected(item);
  }
}
