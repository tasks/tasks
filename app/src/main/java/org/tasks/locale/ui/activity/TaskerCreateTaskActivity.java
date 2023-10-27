package org.tasks.locale.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;

import net.dinglisch.android.tasker.TaskerPlugin;

import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.databinding.ActivityTaskerCreateBinding;
import org.tasks.extensions.Context;
import org.tasks.locale.bundle.TaskCreationBundle;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class TaskerCreateTaskActivity extends AbstractFragmentPluginAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  @Inject Inventory inventory;
  @Inject LocalBroadcastManager localBroadcastManager;

  private ActivityTaskerCreateBinding binding;

  private Bundle previousBundle;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityTaskerCreateBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar.toolbar;
    toolbar.setTitle(R.string.tasker_create_task);
    toolbar.setNavigationIcon(getDrawable(R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());
    toolbar.setOnMenuItemClickListener(this);
    toolbar.inflateMenu(R.menu.menu_help);

    if (savedInstanceState != null) {
      previousBundle = savedInstanceState.getParcelable(TaskCreationBundle.EXTRA_BUNDLE);
      TaskCreationBundle bundle = new TaskCreationBundle(previousBundle);
      binding.title.setText(bundle.getTitle());
    }

    if (!inventory.purchasedTasker()) {
      showPurchaseDialog();
    }
  }

  private void showPurchaseDialog() {
    startActivity(new Intent(this, PurchaseActivity.class));
  }

  @Override
  public void onPostCreateWithPreviousResult(
      final Bundle previousBundle, final String previousBlurb) {
    this.previousBundle = previousBundle;
    TaskCreationBundle bundle = new TaskCreationBundle(previousBundle);
    binding.title.setText(bundle.getTitle());
    binding.dueDate.setText(bundle.getDueDate());
    binding.dueTime.setText(bundle.getDueTime());
    binding.priority.setText(bundle.getPriority());
    binding.description.setText(bundle.getDescription());
  }

  @Override
  public boolean isBundleValid(final Bundle bundle) {
    return TaskCreationBundle.isBundleValid(bundle);
  }

  @Override
  protected Bundle getResultBundle() {
    TaskCreationBundle bundle = new TaskCreationBundle();
    bundle.setTitle(binding.title.getText().toString().trim());
    bundle.setDueDate(binding.dueDate.getText().toString().trim());
    bundle.setDueTime(binding.dueTime.getText().toString().trim());
    bundle.setPriority(binding.priority.getText().toString().trim());
    bundle.setDescription(binding.description.getText().toString().trim());
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
    return binding.title.getText().toString().trim();
  }

  @SuppressLint("MissingSuperCall")
  @Override
  public void onBackPressed() {
    discardButtonClick();
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
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_help) {
      Context.INSTANCE.openUri(this, R.string.url_tasker);
      return true;
    }
    return onOptionsItemSelected(item);
  }
}
