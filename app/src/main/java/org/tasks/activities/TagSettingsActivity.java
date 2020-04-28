/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.activities;

import static org.tasks.Strings.isNullOrEmpty;

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
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.helper.UUIDHelper;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.ActivityComponent;

public class TagSettingsActivity extends BaseListSettingsActivity {

  public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; // $NON-NLS-1$
  public static final String EXTRA_TAG_DATA = "tagData"; // $NON-NLS-1$
  private static final String EXTRA_TAG_UUID = "uuid"; // $NON-NLS-1$
  @Inject TagDataDao tagDataDao;
  @Inject TagDao tagDao;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  private boolean isNewTag;
  private TagData tagData;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    tagData = getIntent().getParcelableExtra(EXTRA_TAG_DATA);

    super.onCreate(savedInstanceState);

    if (tagData == null) {
      isNewTag = true;
      tagData = new TagData();
      tagData.setRemoteId(UUIDHelper.newUUID());
    }
    if (savedInstanceState == null) {
      selectedColor = tagData.getColor();
      selectedIcon = tagData.getIcon();
    }

    name.setText(tagData.getName());

    String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
    if (!isNullOrEmpty(autopopulateName)) {
      name.setText(autopopulateName);
      getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
    } else if (isNewTag) {
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    }

    updateTheme();
  }

  @Override
  protected boolean isNew() {
    return tagData == null;
  }

  @Override
  protected String getToolbarTitle() {
    return isNew() ? getString(R.string.new_tag) : tagData.getName();
  }

  @OnTextChanged(R.id.name)
  void onTextChanged() {
    nameLayout.setError(null);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  private boolean clashes(String newName) {
    return (isNewTag || !newName.equalsIgnoreCase(tagData.getName()))
        && tagDataDao.getTagByName(newName) != null;
  }

  @Override
  protected void save() {
    String newName = getNewName();

    if (isNullOrEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    if (clashes(newName)) {
      nameLayout.setError(getString(R.string.tag_already_exists));
      return;
    }

    if (isNewTag) {
      tagData.setName(newName);
      tagData.setColor(selectedColor);
      tagData.setIcon(selectedIcon);
      tagDataDao.createNew(tagData);
      setResult(RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, new TagFilter(tagData)));
    } else if (hasChanges()) {
      tagData.setName(newName);
      tagData.setColor(selectedColor);
      tagData.setIcon(selectedIcon);
      tagDataDao.update(tagData);
      tagDao.rename(tagData.getRemoteId(), newName);
      setResult(
          RESULT_OK,
          new Intent(TaskListFragment.ACTION_RELOAD)
              .putExtra(MainActivity.OPEN_FILTER, new TagFilter(tagData)));
    }

    finish();
  }

  @Override
  protected boolean hasChanges() {
    if (isNewTag) {
      return selectedColor >= 0 || selectedIcon >= 0 || !isNullOrEmpty(getNewName());
    }
    return !(selectedColor == tagData.getColor()
        && selectedIcon == tagData.getIcon()
        && getNewName().equals(tagData.getName()));
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
    super.finish();
  }

  @Override
  protected int getLayout() {
    return R.layout.activity_tag_settings;
  }

  @Override
  protected void delete() {
    if (tagData != null) {
      String uuid = tagData.getRemoteId();
      tagDataDao.delete(tagData);
      setResult(
          RESULT_OK,
          new Intent(TaskListFragment.ACTION_DELETED).putExtra(EXTRA_TAG_UUID, uuid));
    }
    finish();
  }
}
