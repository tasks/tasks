package org.tasks.widget;

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.dialogs.ColorPalettePicker;
import org.tasks.dialogs.ColorPickerAdapter.Palette;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.intents.TaskIntents;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.themes.DrawableUtil;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public class ShortcutConfigActivity extends ThemedInjectingAppCompatActivity
    implements ColorPalettePicker.ColorPickedCallback {

  private static final String EXTRA_FILTER = "extra_filter";
  private static final String EXTRA_THEME = "extra_theme";
  private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
  private static final int REQUEST_FILTER = 1019;

  @Inject DefaultFilterProvider defaultFilterProvider;
  @Inject ThemeColor themeColor;
  @Inject ThemeCache themeCache;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.shortcut_list_layout)
  TextInputLayout shortcutListLayout;

  @BindView(R.id.shortcut_list)
  TextInputEditText shortcutList;

  @BindView(R.id.shortcut_name)
  TextInputEditText shortcutName;

  @BindView(R.id.color)
  TextView colorIcon;

  @BindView(R.id.clear)
  View clear;

  private Filter selectedFilter;
  private int selectedTheme;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setContentView(R.layout.activity_widget_shortcut_layout);

    ButterKnife.bind(this);

    toolbar.setTitle(R.string.FSA_label);
    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());

    if (icicle == null) {
      selectedFilter = defaultFilterProvider.getDefaultFilter();
      selectedTheme = 7;
    } else {
      selectedFilter = icicle.getParcelable(EXTRA_FILTER);
      selectedTheme = icicle.getInt(EXTRA_THEME);
    }

    updateFilterAndTheme();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_FILTER) {
      if (resultCode == Activity.RESULT_OK) {
        if (selectedFilter != null && selectedFilter.listingTitle.equals(getShortcutName())) {
          shortcutName.setText(null);
        }
        selectedFilter = data.getParcelableExtra(FilterSelectionActivity.EXTRA_FILTER);
        updateFilterAndTheme();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_FILTER, selectedFilter);
    outState.putInt(EXTRA_THEME, selectedTheme);
  }

  @OnFocusChange(R.id.shortcut_list)
  void onListFocusChange(boolean focused) {
    if (focused) {
      shortcutList.clearFocus();
      showListPicker();
    }
  }

  @OnClick(R.id.shortcut_list)
  void showListPicker() {
    Intent intent = new Intent(this, FilterSelectionActivity.class);
    intent.putExtra(FilterSelectionActivity.EXTRA_FILTER, selectedFilter);
    intent.putExtra(FilterSelectionActivity.EXTRA_RETURN_FILTER, true);
    startActivityForResult(intent, REQUEST_FILTER);
  }

  @OnClick(R.id.color_row)
  void showThemePicker() {
    ColorPalettePicker.Companion.newColorPalette(null, 0, Palette.LAUNCHERS)
        .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
  }

  private void updateFilterAndTheme() {
    if (Strings.isNullOrEmpty(getShortcutName()) && selectedFilter != null) {
      shortcutName.setText(selectedFilter.listingTitle);
    }
    if (selectedFilter != null) {
      shortcutList.setText(selectedFilter.listingTitle);
    }
    updateTheme();
  }

  private void updateTheme() {
    clear.setVisibility(View.GONE);
    ThemeColor color = themeCache.getThemeColor(getThemeIndex());
    DrawableUtil.setLeftDrawable(this, colorIcon, R.drawable.color_picker);
    DrawableUtil.setTint(DrawableUtil.getLeftDrawable(colorIcon), color.getPrimaryColor());
    color.apply(toolbar);
    color.applyToSystemBars(this);
  }

  private int getThemeIndex() {
    return selectedTheme >= 0 && selectedTheme < ThemeColor.ICONS.length - 1 ? selectedTheme : 7;
  }

  private String getShortcutName() {
    return shortcutName.getText().toString().trim();
  }

  private void save() {
    String filterId = defaultFilterProvider.getFilterPreferenceValue(selectedFilter);
    Intent shortcutIntent = TaskIntents.getTaskListByIdIntent(this, filterId);
    Parcelable icon = ShortcutIconResource.fromContext(this, ThemeColor.ICONS[getThemeIndex()]);

    Intent intent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getShortcutName());
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
    setResult(RESULT_OK, intent);
    finish();
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void onColorPicked(int color) {
    selectedTheme = color;
    updateTheme();
  }
}
