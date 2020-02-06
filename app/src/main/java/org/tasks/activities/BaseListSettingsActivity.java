package org.tasks.activities;

import static org.tasks.dialogs.IconPickerDialog.newIconPicker;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.IconPickerDialog.IconPickerCallback;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.themes.CustomIcons;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.MenuColorizer;

public abstract class BaseListSettingsActivity extends ThemedInjectingAppCompatActivity
    implements IconPickerCallback, OnMenuItemClickListener {

  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final String EXTRA_SELECTED_ICON = "extra_selected_icon";
  private static final String FRAG_TAG_ICON_PICKER = "frag_tag_icon_picker";
  private static final int REQUEST_COLOR_PICKER = 10109;

  @BindView(R.id.color)
  ImageView color;

  @BindView(R.id.icon)
  ImageView icon;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject DialogBuilder dialogBuilder;

  protected int selectedTheme = -1;
  protected int selectedIcon = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(getLayout());

    ButterKnife.bind(this);

    if (savedInstanceState != null) {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
      selectedIcon = savedInstanceState.getInt(EXTRA_SELECTED_ICON);
    }

    toolbar.setTitle(getToolbarTitle());
    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());
    if (!isNew()) {
      toolbar.inflateMenu(R.menu.menu_tag_settings);
    }
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(this, toolbar);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_THEME, selectedTheme);
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

  @OnClick(R.id.color_row)
  protected void showThemePicker() {
    Intent intent = new Intent(BaseListSettingsActivity.this, ColorPickerActivity.class);
    intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
    intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
    intent.putExtra(ColorPickerActivity.EXTRA_THEME_INDEX, selectedTheme);
    startActivityForResult(intent, REQUEST_COLOR_PICKER);
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
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        selectedTheme = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        updateTheme();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
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
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> delete())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  protected void updateTheme() {
    ThemeColor themeColor;
    if (selectedTheme < 0) {
      themeColor = this.themeColor;
      color.setVisibility(View.GONE);
    } else {
      themeColor = themeCache.getThemeColor(selectedTheme);
      Drawable colorIcon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_lens_24px);
      Drawable wrappedColorIcon = DrawableCompat.wrap(colorIcon.mutate());
      DrawableCompat.setTint(wrappedColorIcon, themeColor.getPrimaryColor());
      color.setImageDrawable(wrappedColorIcon);
      color.setVisibility(View.VISIBLE);
    }
    themeColor.apply(toolbar);
    themeColor.applyToSystemBars(this);
    Integer icon = CustomIcons.getIconResId(selectedIcon);
    if (icon == null) {
      icon = CustomIcons.getIconResId(CustomIcons.getCLOUD());
    }
    this.icon.setImageResource(icon == null ? R.drawable.ic_outline_cloud_24px : icon);
  }
}
