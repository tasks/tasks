package org.tasks.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

public class ThemePickerDialog {

    public enum ColorPalette {THEMES, COLORS, ACCENTS, WIDGET_BACKGROUND}

    public interface ThemePickerCallback {
        void themePicked(ColorPalette palette, int index);

        void initiateThemePurchase();
    }

    private DialogBuilder dialogBuilder;
    private Context context;
    private Preferences preferences;
    private ThemeCache themeCache;
    private Theme theme;

    @Inject
    public ThemePickerDialog(DialogBuilder dialogBuilder, @ForApplication Context context, Preferences preferences,
                             Theme theme, ThemeCache themeCache) {
        this.dialogBuilder = dialogBuilder;
        this.context = context;
        this.preferences = preferences;
        this.theme = theme;
        this.themeCache = themeCache;
    }

    public AlertDialog createDialog(final ColorPalette palette, final ThemePickerCallback callback) {
        if (palette == ColorPalette.THEMES || palette == ColorPalette.WIDGET_BACKGROUND) {
            theme = theme.withBaseTheme(themeCache.getThemeBase(2));
        }

        final String[] themes = context.getResources().getStringArray(getNameRes(palette));

        final boolean purchasedThemes = preferences.hasPurchase(R.string.p_purchased_themes);

        final LayoutInflater inflater = theme.getLayoutInflater(context);
        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.color_selection_row, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) (convertView == null
                        ? inflater.inflate(R.layout.color_selection_row, parent, false)
                        : convertView);
                Drawable original = context.getResources().getDrawable(purchasedThemes || position < 2
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, getDisplayColor(palette, position));
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

    private int getNameRes(ColorPalette palette) {
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

    private int getDisplayColor(ColorPalette palette, int index) {
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
}
