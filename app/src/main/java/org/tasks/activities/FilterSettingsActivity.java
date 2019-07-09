/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.activities;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import butterknife.BindView;
import butterknife.OnTextChanged;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.CustomFilter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.caldav.BaseListSettingsActivity;
import org.tasks.data.FilterDao;
import org.tasks.injection.ActivityComponent;

public class FilterSettingsActivity extends BaseListSettingsActivity {

  public static final String TOKEN_FILTER = "token_filter";

  @Inject FilterDao filterDao;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  private CustomFilter filter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    filter = getIntent().getParcelableExtra(TOKEN_FILTER);

    super.onCreate(savedInstanceState);

    if (savedInstanceState == null) {
      selectedTheme = filter.tint;
      selectedIcon = filter.icon;
    }

    name.setText(filter.listingTitle);

    updateTheme();
  }

  @Override
  protected boolean isNew() {
    return false;
  }

  @Override
  protected String getToolbarTitle() {
    return filter.listingTitle;
  }

  @OnTextChanged(R.id.name)
  void onTextChanged(CharSequence ignored) {
    nameLayout.setError(null);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void save() {
    String newName = getNewName();

    if (isEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    if (hasChanges()) {
      filter.listingTitle = newName;
      filter.tint = selectedTheme;
      filter.icon = selectedIcon;
      filterDao.update(filter.toStoreObject());
      setResult(
          RESULT_OK,
          new Intent(TaskListFragment.ACTION_RELOAD).putExtra(MainActivity.OPEN_FILTER, filter));
    }

    finish();
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  @Override
  protected boolean hasChanges() {
    return !(getNewName().equals(filter.listingTitle)
        && selectedTheme == filter.tint
        && selectedIcon == filter.icon);
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
    super.finish();
  }

  @Override
  protected int getLayout() {
    return R.layout.filter_settings_activity;
  }

  @Override
  protected void delete() {
    filterDao.delete(filter.getId());
    setResult(
        RESULT_OK,
        new Intent(TaskListFragment.ACTION_DELETED).putExtra(TOKEN_FILTER, filter));
    finish();
  }
}
