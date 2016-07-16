package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class ThemePickerDialog extends InjectingDialogFragment {

    private static final String EXTRA_COLOR_PALETTE = "extra_color_palette";

    public enum ColorPalette {
        THEMES,
        COLORS,
        ACCENTS,
        WIDGET_BACKGROUND
    }

    public static ThemePickerDialog newThemePickerDialog(ColorPalette palette) {
        ThemePickerDialog dialog = new ThemePickerDialog();
        dialog.palette = palette;
        return dialog;
    }

    public interface ThemePickerCallback {
        void themePicked(ColorPalette palette, int index);

        void initiateThemePurchase();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForApplication Context context;
    @Inject Preferences preferences;
    @Inject Theme theme;
    @Inject ThemeCache themeCache;

    private ThemePickerCallback callback;
    private ColorPalette palette;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            palette = (ColorPalette) savedInstanceState.getSerializable(EXTRA_COLOR_PALETTE);
        }

        if (palette == ColorPalette.THEMES || palette == ColorPalette.WIDGET_BACKGROUND) {
            theme = theme.withBaseTheme(themeCache.getThemeBase(2));
        }

        final String[] themes = getResources().getStringArray(getNameRes());

        final boolean purchasedThemes = preferences.hasPurchase(R.string.p_purchased_themes);

        final LayoutInflater inflater = theme.getLayoutInflater(context);
        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.color_selection_row, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) (convertView == null
                        ? inflater.inflate(R.layout.color_selection_row, parent, false)
                        : convertView);
                Drawable original = getResources().getDrawable(purchasedThemes || position < 2
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, getDisplayColor(position));
                if (atLeastJellybeanMR1()) {
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
                }
                textView.setText(themes[position]);
                return textView;
            }
        };

        return dialogBuilder.newDialog(theme)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (purchasedThemes || which < 2) {
                            callback.themePicked(palette, which);
                        } else {
                            callback.initiateThemePurchase();
                        }
                    }
                })
                .show();
    }

    private int getNameRes() {
        switch (palette) {
            case COLORS:
                return R.array.colors;
            case ACCENTS:
                return R.array.accents;
            case WIDGET_BACKGROUND:
                return R.array.widget_background;
            default:
                return R.array.themes;
        }
    }

    private int getDisplayColor(int index) {
        switch (palette) {
            case COLORS:
                return themeCache.getThemeColor(index).getPrimaryColor();
            case ACCENTS:
                return themeCache.getThemeAccent(index).getAccentColor();
            case WIDGET_BACKGROUND:
                return themeCache.getWidgetTheme(index).getBackgroundColor();
            default:
                return themeCache.getThemeBase(index).getContentBackground();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(EXTRA_COLOR_PALETTE, palette);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (ThemePickerCallback) activity;
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
