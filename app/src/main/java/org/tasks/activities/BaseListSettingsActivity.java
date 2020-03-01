package org.tasks.activities;

import static org.tasks.dialogs.IconPickerDialog.newIconPicker;
import static org.tasks.themes.DrawableUtil.getLeftDrawable;
import static org.tasks.themes.ThemeColor.newThemeColor;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.ColorPalettePicker;
import org.tasks.dialogs.ColorWheelPicker;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.IconPickerDialog.IconPickerCallback;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.DrawableUtil;
import org.tasks.themes.ThemeColor;

public abstract class BaseListSettingsActivity extends ThemedInjectingAppCompatActivity
    implements IconPickerCallback,
        OnMenuItemClickListener,
        ColorPalettePicker.ColorPickedCallback,
        ColorWheelPicker.ColorPickedCallback {

  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final String EXTRA_SELECTED_ICON = "extra_selected_icon";
  private static final String FRAG_TAG_ICON_PICKER = "frag_tag_icon_picker";
  private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
  protected int selectedColor = 0;
  protected int selectedIcon = -1;

  @BindView(R.id.clear)
  View clear;

  @BindView(R.id.color)
  TextView color;

  @BindView(R.id.icon)
  TextView icon;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @Inject ThemeColor themeColor;
  @Inject DialogBuilder dialogBuilder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(getLayout());

    ButterKnife.bind(this);

    if (savedInstanceState != null) {
      selectedColor = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
      selectedIcon = savedInstanceState.getInt(EXTRA_SELECTED_ICON);
    }

    toolbar.setTitle(getToolbarTitle());
    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());
    if (!isNew()) {
      toolbar.inflateMenu(R.menu.menu_tag_settings);
    }
    toolbar.setOnMenuItemClickListener(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_THEME, selectedColor);
    outState.putInt(EXTRA_SELECTED_ICON, selectedIcon);
  }

  @Override
  public void onBackPressed() {
    discard();
  }

  protected abstract int getLayout();

  protected abstract boolean hasChanges();

  protected abstract void save();

  protected abstract boolean isNew();

  protected abstract String getToolbarTitle();

  protected abstract void delete();

  protected void discard() {
    if (!hasChanges()) {
      finish();
    } else {
      dialogBuilder
          .newDialog(R.string.discard_changes)
          .setPositiveButton(R.string.discard, (dialog, which) -> finish())
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  @OnClick(R.id.clear)
  protected void clearColor() {
    onColorPicked(0);
  }

  @OnClick(R.id.color_row)
  protected void showThemePicker() {
    ColorPalettePicker.Companion.newColorPalette(null, 0, selectedColor)
        .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
  }

  @OnClick(R.id.icon_row)
  protected void showIconPicker() {
    newIconPicker(selectedIcon).show(getSupportFragmentManager(), FRAG_TAG_ICON_PICKER);
  }

  @Override
  public void onSelected(DialogInterface dialogInterface, int icon) {
    this.selectedIcon = icon;
    dialogInterface.dismiss();
    updateTheme();
  }

  @Override
  public void onColorPicked(int color) {
    selectedColor = color;
    updateTheme();
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.delete) {
      promptDelete();
      return true;
    }
    return onOptionsItemSelected(item);
  }

  protected void promptDelete() {
    dialogBuilder
        .newDialog(R.string.delete_tag_confirmation, getToolbarTitle())
        .setPositiveButton(R.string.delete, (dialog, which) -> delete())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  protected void updateTheme() {
    ThemeColor themeColor;
    DrawableUtil.setLeftDrawable(this, color, R.drawable.color_picker);
    Drawable leftDrawable = getLeftDrawable(color);
    if (selectedColor == 0) {
      themeColor = this.themeColor;
      DrawableCompat.setTint(
          leftDrawable, ContextCompat.getColor(this, android.R.color.transparent));
      clear.setVisibility(View.GONE);
    } else {
      themeColor = newThemeColor(this, selectedColor);
      DrawableCompat.setTint(
          leftDrawable instanceof LayerDrawable
              ? ((LayerDrawable) leftDrawable).getDrawable(0)
              : leftDrawable,
          themeColor.getPrimaryColor());
      clear.setVisibility(View.VISIBLE);
    }
    themeColor.apply(toolbar);
    themeColor.applyToSystemBars(this);
    Integer icon = CustomIcons.getIconResId(selectedIcon);
    if (icon == null) {
      icon = CustomIcons.getIconResId(CustomIcons.getCLOUD());
    }
    DrawableUtil.setLeftDrawable(this, this.icon, icon);
    DrawableCompat.setTint(
        getLeftDrawable(this.icon), ContextCompat.getColor(this, R.color.icon_tint_with_alpha));
  }
}
