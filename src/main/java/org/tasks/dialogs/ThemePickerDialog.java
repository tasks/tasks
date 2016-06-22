package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.Theme;
import org.tasks.preferences.ThemeManager;

import javax.inject.Inject;

public class ThemePickerDialog extends InjectingDialogFragment {

    private static final String EXTRA_COLOR_PALETTE = "extra_color_palette";

    public enum ColorPalette {
        THEMES,
        COLORS,
        ACCENTS
    }

    public static ThemePickerDialog newThemePickerDialog(ColorPalette palette) {
        ThemePickerDialog dialog = new ThemePickerDialog();
        dialog.palette = palette;
        return dialog;
    }

    public interface ThemePickerCallback {
        void themePicked(ColorPalette palette, Theme theme);

        void initiateThemePurchase();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForApplication Context context;
    @Inject Preferences preferences;
    @Inject ThemeManager themeManager;

    private ThemePickerCallback callback;
    private ColorPalette palette;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            palette = (ColorPalette) savedInstanceState.getSerializable(EXTRA_COLOR_PALETTE);
        }

        final String[] themes = context.getResources().getStringArray(getNameRes());

        final boolean purchasedThemes = preferences.hasPurchase(R.string.p_purchased_themes);

        final LayoutInflater inflater = themeManager.getThemedLayoutInflater();
        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.color_selection_row, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;

                if (row == null) {
                    row = inflater.inflate(R.layout.color_selection_row, parent, false);
                }

                Resources resources = context.getResources();
                Theme theme = getTheme(position);
                ImageView primary = (ImageView) row.findViewById(R.id.color_primary);
                Drawable original = resources.getDrawable(purchasedThemes || position < 2
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, getDisplayColor(theme));
                primary.setImageDrawable(wrapped);

                TextView text = (TextView) row.findViewById(android.R.id.text1);
                text.setText(themes[position]);

                return row;
            }
        };

        return dialogBuilder.newDialog()
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (purchasedThemes || which < 2) {
                            callback.themePicked(palette, themeManager.getColor(which));
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
            default:
                return R.array.themes;
        }
    }

    private Theme getTheme(int position) {
        switch (palette) {
            case COLORS:
                return themeManager.getColor(position);
            case ACCENTS:
                return themeManager.getAccent(position);
            default:
                return themeManager.getBaseTheme(position);
        }
    }

    private int getDisplayColor(Theme theme) {
        switch (palette) {
            case COLORS:
                return theme.getPrimaryColor();
            case ACCENTS:
                return theme.getAccentColor();
            default:
                return theme.getContentBackground();
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
