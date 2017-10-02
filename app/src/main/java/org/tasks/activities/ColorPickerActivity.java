package org.tasks.activities;

import android.content.Intent;
import android.os.Bundle;

import org.tasks.R;
import org.tasks.billing.PurchaseHelper;
import org.tasks.billing.PurchaseHelperCallback;
import org.tasks.dialogs.ColorPickerDialog;
import org.tasks.dialogs.ColorPickerDialog.ColorPalette;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import static org.tasks.dialogs.ColorPickerDialog.newColorPickerDialog;

public class ColorPickerActivity extends ThemedInjectingAppCompatActivity implements ColorPickerDialog.ThemePickerCallback, PurchaseHelperCallback {

    private static final String FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker";
    private static final int REQUEST_PURCHASE = 1006;

    public static final String EXTRA_PALETTE = "extra_palette";
    public static final String EXTRA_SHOW_NONE = "extra_show_none";
    public static final String EXTRA_THEME_INDEX = "extra_index";

    @Inject PurchaseHelper purchaseHelper;
    @Inject Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        Intent intent = getIntent();
        ColorPalette palette = (ColorPalette) intent.getSerializableExtra(EXTRA_PALETTE);
        boolean showNone = intent.getBooleanExtra(EXTRA_SHOW_NONE, false);
        int selected = intent.hasExtra(EXTRA_THEME_INDEX)
                ? intent.getIntExtra(EXTRA_THEME_INDEX, -1)
                : getCurrentSelection(palette);
        newColorPickerDialog(palette, showNone, selected)
                .show(getSupportFragmentManager(), FRAG_TAG_COLOR_PICKER);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void themePicked(final ColorPalette palette, final int index) {
        Intent data = new Intent();
        data.putExtra(EXTRA_PALETTE, palette);
        data.putExtra(EXTRA_THEME_INDEX, index);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void initiateThemePurchase() {
        purchaseHelper.purchase(this, getString(R.string.sku_themes), getString(R.string.p_purchased_themes), REQUEST_PURCHASE, this);
    }

    @Override
    public void dismissed() {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PURCHASE) {
            purchaseHelper.handleActivityResult(null, requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void purchaseCompleted(boolean success, String sku) {
        if (!success) {
            finish();
        }
    }

    private int getCurrentSelection(ColorPalette palette) {
        switch (palette) {
            case COLORS:
                return theme.getThemeColor().getIndex();
            case ACCENTS:
                return theme.getThemeAccent().getIndex();
            default:
                return theme.getThemeBase().getIndex();
        }
    }
}
