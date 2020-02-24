package org.tasks.activities;

import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;
import static org.tasks.dialogs.ColorPickerDialog.newColorPickerDialog;
import static org.tasks.themes.ThemeColor.newThemeColor;

import android.content.Intent;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.ColorPickerDialog.Pickable;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

public class ColorPickerActivity extends InjectingAppCompatActivity
    implements ColorPickerDialog.ThemePickerCallback {

  public static final String EXTRA_PALETTE = "extra_palette";
  public static final String EXTRA_SHOW_NONE = "extra_show_none";
  public static final String EXTRA_COLOR = "extra_index";
  private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
  private static final String FRAG_TAG_PURCHASE = "frag_tag_purchase";
  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject Inventory inventory;
  @Inject Preferences preferences;

  private ColorPalette palette;

  @Override
  protected void onPostResume() {
    super.onPostResume();

    Intent intent = getIntent();
    palette = (ColorPalette) intent.getSerializableExtra(EXTRA_PALETTE);
    boolean showNone = intent.getBooleanExtra(EXTRA_SHOW_NONE, false);
    List<? extends Pickable> items = getItems(palette);
    int selected;
    if (palette == ColorPalette.COLORS) {
      selected =
          items.indexOf(
              intent.hasExtra(EXTRA_COLOR)
                  ? newThemeColor(this, intent.getIntExtra(EXTRA_COLOR, 0))
                  : theme.getThemeColor());
    } else {
      selected =
          intent.hasExtra(EXTRA_COLOR)
              ? intent.getIntExtra(EXTRA_COLOR, -1)
              : getCurrentSelection(palette);
    }

    newColorPickerDialog(items, showNone, selected)
        .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
  }

  private List<? extends ColorPickerDialog.Pickable> getItems(ColorPalette palette) {
    switch (palette) {
      case ACCENTS:
        return themeCache.getAccents();
      case COLORS:
        return themeCache.getColors();
      case LAUNCHER:
        return themeCache.getColors().subList(0, themeCache.getColors().size() - 1);
      case THEMES:
        return themeCache.getThemes();
      case WIDGET_BACKGROUND:
        return themeCache.getWidgetThemes();
      default:
        throw new IllegalArgumentException("Unsupported palette: " + palette);
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void themePicked(ColorPickerDialog.Pickable picked) {
    Intent data = new Intent();
    data.putExtra(EXTRA_PALETTE, palette);
    data.putExtra(EXTRA_COLOR, picked == null ? -1 : picked.getIndex());
    setResult(RESULT_OK, data);
    finish();
  }

  @Override
  public void initiateThemePurchase() {
    newPurchaseDialog().show(getSupportFragmentManager(), FRAG_TAG_PURCHASE);
  }

  @Override
  public void dismissed() {
    finish();
  }

  private int getCurrentSelection(ColorPalette palette) {
    switch (palette) {
      case COLORS:
        return theme.getThemeColor().getIndex();
      case ACCENTS:
        return theme.getThemeAccent().getIndex();
      case LAUNCHER:
        return preferences.getInt(R.string.p_theme_launcher, 7);
      default:
        return theme.getThemeBase().getIndex();
    }
  }

  public enum ColorPalette {
    THEMES,
    COLORS,
    ACCENTS,
    LAUNCHER,
    WIDGET_BACKGROUND
  }
}
