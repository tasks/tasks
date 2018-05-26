/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.activities;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import com.todoroo.astrid.api.CustomFilter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

public class FilterSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String TOKEN_FILTER = "token_filter";

  public static final String ACTION_FILTER_DELETED = "filterDeleted";
  public static final String ACTION_FILTER_RENAMED = "filterRenamed";
  @Inject FilterDao filterDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  private CustomFilter filter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.filter_settings_activity);
    ButterKnife.bind(this);

    filter = getIntent().getParcelableExtra(TOKEN_FILTER);

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            this, backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
    toolbar.setTitle(filter.listingTitle);
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
    MenuColorizer.colorToolbar(this, toolbar);

    name.setText(filter.listingTitle);
  }

  @OnTextChanged(R.id.name)
  void onTextChanged(CharSequence ignored) {
    nameLayout.setError(null);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private void save() {
    String oldName = filter.listingTitle;
    String newName = name.getText().toString().trim();

    if (isEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    boolean nameChanged = !oldName.equals(newName);
    if (nameChanged) {
      filter.listingTitle = newName;
      filterDao.update(filter.toStoreObject());
      setResult(RESULT_OK, new Intent(ACTION_FILTER_RENAMED).putExtra(TOKEN_FILTER, filter));
    }

    finish();
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
    dialogBuilder
        .newMessageDialog(R.string.delete_tag_confirmation, filter.listingTitle)
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> {
              filterDao.delete(filter.getId());
              setResult(
                  RESULT_OK, new Intent(ACTION_FILTER_DELETED).putExtra(TOKEN_FILTER, filter));
              finish();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void discard() {
    String tagName = this.name.getText().toString().trim();
    if (filter.listingTitle.equals(tagName)) {
      finish();
    } else {
      dialogBuilder
          .newMessageDialog(R.string.discard_changes)
          .setPositiveButton(R.string.keep_editing, null)
          .setNegativeButton(R.string.discard, (dialog, which) -> finish())
          .show();
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete:
        deleteTag();
        break;
    }
    return super.onOptionsItemSelected(item);
  }
}
