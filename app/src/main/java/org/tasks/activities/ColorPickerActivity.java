package org.tasks.activities;

import static org.tasks.dialogs.ColorPickerDialog.newColorPickerDialog;

import android.content.Intent;
import android.os.Bundle;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.billing.PurchaseActivity;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

public class ColorPickerActivity extends ThemedInjectingAppCompatActivity
    implements ColorPickerDialog.ThemePickerCallback {

  public static final String EXTRA_PALETTE = "extra_palette";
  public static final String EXTRA_SHOW_NONE = "extra_show_none";
  public static final String EXTRA_THEME_INDEX = "extra_index";
  private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
  private static final int REQUEST_SUBSCRIPTION = 10101;
  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject BillingClient billingClient;
  @Inject Inventory inventory;
  @Inject Preferences preferences;

  private ColorPalette palette;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();

    Intent intent = getIntent();
    palette = (ColorPalette) intent.getSerializableExtra(EXTRA_PALETTE);
    boolean showNone = intent.getBooleanExtra(EXTRA_SHOW_NONE, false);
    int selected =
        intent.hasExtra(EXTRA_THEME_INDEX)
            ? intent.getIntExtra(EXTRA_THEME_INDEX, -1)
            : getCurrentSelection(palette);
    newColorPickerDialog(getItems(palette), showNone, selected)
        .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
  }

  private List<? extends ColorPickerDialog.Pickable> getItems(ColorPalette palette) {
    switch (palette) {
      case ACCENTS:
        return themeCache.getAccents();
      case COLORS:
      case LAUNCHER:
        return themeCache.getColors();
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
    data.putExtra(EXTRA_THEME_INDEX, picked == null ? -1 : picked.getIndex());
    setResult(RESULT_OK, data);
    finish();
  }

  @Override
  public void initiateThemePurchase() {
    startActivityForResult(new Intent(this, PurchaseActivity.class), REQUEST_SUBSCRIPTION);
  }

  @Override
  public void dismissed() {
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SUBSCRIPTION) {
      if (!inventory.purchasedThemes()) {
        finish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
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
