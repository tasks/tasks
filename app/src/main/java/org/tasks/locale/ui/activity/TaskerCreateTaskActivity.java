package org.tasks.locale.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.dinglisch.android.tasker.TaskerPlugin;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.injection.ActivityComponent;
import org.tasks.locale.bundle.TaskCreationBundle;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

public final class TaskerCreateTaskActivity extends AbstractFragmentPluginAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final int REQUEST_SUBSCRIPTION = 10101;

  @Inject Preferences preferences;
  @Inject BillingClient billingClient;
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
            this, backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
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
      startActivityForResult(new Intent(this, PurchaseActivity.class), REQUEST_SUBSCRIPTION);
    }
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
    finish();
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
    switch (item.getItemId()) {
      case R.id.menu_save:
        save();
        return true;
      case R.id.menu_help:
        startActivity(
            new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://tasks.org/help/tasker")));
        return true;
    }
    return onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SUBSCRIPTION) {
      if (!inventory.purchasedTasker()) {
        discardButtonClick();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
