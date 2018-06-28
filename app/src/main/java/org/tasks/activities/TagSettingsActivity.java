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
import android.text.InputType;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.tags.TagService;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class TagSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String TOKEN_AUTOPOPULATE_NAME = "autopopulateName"; // $NON-NLS-1$
  public static final String EXTRA_TAG_DATA = "tagData"; // $NON-NLS-1$
  public static final String ACTION_RELOAD = "tagRenamed";
  public static final String ACTION_DELETED = "tagDeleted";
  private static final String EXTRA_TAG_UUID = "uuid"; // $NON-NLS-1$
  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final int REQUEST_COLOR_PICKER = 10109;
  @Inject TagService tagService;
  @Inject TagDataDao tagDataDao;
  @Inject TagDao tagDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject Tracker tracker;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.color)
  TextInputEditText color;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  private boolean isNewTag;
  private TagData tagData;
  private int selectedTheme;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.tag_settings_activity);
    ButterKnife.bind(this);

    tagData = getIntent().getParcelableExtra(EXTRA_TAG_DATA);
    if (tagData == null) {
      isNewTag = true;
      tagData = new TagData();
      tagData.setRemoteId(UUIDHelper.newUUID());
    }
    if (savedInstanceState == null) {
      selectedTheme = tagData.getColor();
    } else {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
    }

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setTitle(isNewTag ? getString(R.string.new_tag) : tagData.getName());
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
    toolbar.inflateMenu(R.menu.menu_tag_settings);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.showOverflowMenu();

    color.setInputType(InputType.TYPE_NULL);

    name.setText(tagData.getName());

    String autopopulateName = getIntent().getStringExtra(TOKEN_AUTOPOPULATE_NAME);
    if (!isEmpty(autopopulateName)) {
      name.setText(autopopulateName);
      getIntent().removeExtra(TOKEN_AUTOPOPULATE_NAME);
    } else if (isNewTag) {
      toolbar.getMenu().findItem(R.id.delete).setVisible(false);
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    }

    updateTheme();
  }

  @OnTextChanged(R.id.name)
  void onTextChanged(CharSequence ignored) {
    nameLayout.setError(null);
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
    Intent intent = new Intent(TagSettingsActivity.this, ColorPickerActivity.class);
    intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
    intent.putExtra(ColorPickerActivity.EXTRA_THEME_INDEX, selectedTheme);
    intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
    startActivityForResult(intent, REQUEST_COLOR_PICKER);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  private boolean clashes(String newName) {
    TagData existing = tagDataDao.getTagByName(newName);
    return existing != null && tagData.getId() != existing.getId();
  }

  private void save() {
    String newName = getNewName();

    if (isEmpty(newName)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      return;
    }

    if (clashes(newName)) {
      nameLayout.setError(getString(R.string.tag_already_exists));
      return;
    }

    if (isNewTag) {
      tagData.setName(newName);
      tagData.setColor(selectedTheme);
      tagDataDao.createNew(tagData);
      setResult(RESULT_OK, new Intent().putExtra(MainActivity.OPEN_FILTER, new TagFilter(tagData)));
    } else if (hasChanges()) {
      tagData.setName(newName);
      tagData.setColor(selectedTheme);
      tagService.rename(tagData.getRemoteId(), newName);
      tagDataDao.update(tagData);
      tagDao.rename(tagData.getRemoteId(), newName);
      setResult(
          RESULT_OK,
          new Intent(ACTION_RELOAD).putExtra(MainActivity.OPEN_FILTER, new TagFilter(tagData)));
    }

    finish();
  }

  private boolean hasChanges() {
    if (isNewTag) {
      return selectedTheme >= 0 || !isEmpty(getNewName());
    }
    return !(selectedTheme == tagData.getColor() && getNewName().equals(tagData.getName()));
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

  private void deleteTag() {
    dialogBuilder
        .newMessageDialog(R.string.delete_tag_confirmation, tagData.getName())
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> {
              if (tagData != null) {
                String uuid = tagData.getRemoteId();
                tagDao.deleteTag(uuid);
                tagDataDao.delete(tagData.getId());
                setResult(RESULT_OK, new Intent(ACTION_DELETED).putExtra(EXTRA_TAG_UUID, uuid));
              }
              finish();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
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
