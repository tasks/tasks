package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.ui.SingleCheckedArrayAdapter;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

public class ColorPickerDialog extends InjectingDialogFragment {

    private static final String EXTRA_PALETTE = "extra_palette";
    private static final String EXTRA_SELECTED = "extra_selected";
    private static final String EXTRA_SHOW_NONE = "extra_show_none";

    public enum ColorPalette {THEMES, COLORS, ACCENTS, WIDGET_BACKGROUND}

    public interface ThemePickerCallback {
        void themePicked(ColorPalette palette, int index);

        void initiateThemePurchase();

        void dismissed();
    }

    public static ColorPickerDialog newColorPickerDialog(ColorPalette palette, boolean showNone, int selection) {
        ColorPickerDialog dialog = new ColorPickerDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_PALETTE, palette);
        args.putInt(EXTRA_SELECTED, selection);
        args.putBoolean(EXTRA_SHOW_NONE, showNone);
        dialog.setArguments(args);
        return dialog;
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject Preferences preferences;
    @Inject ThemeCache themeCache;
    @Inject Theme theme;

    private ColorPalette palette;
    private ThemePickerCallback callback;
    private SingleCheckedArrayAdapter adapter;
    private Dialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle arguments = getArguments();
        palette = (ColorPalette) arguments.getSerializable(EXTRA_PALETTE);
        boolean showNone = arguments.getBoolean(EXTRA_SHOW_NONE);
        int selected = arguments.getInt(EXTRA_SELECTED, -1);

        final List<String> themes = Arrays.asList(context.getResources().getStringArray(getNameRes()));

        adapter = new SingleCheckedArrayAdapter(context, themes, theme.getThemeAccent()) {
            @Override
            protected int getDrawable(int position) {
                return preferences.hasPurchase(R.string.p_purchased_themes) || position < getNumFree()
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp;
            }

            @Override
            protected int getDrawableColor(int position) {
                return getDisplayColor(position);
            }
        };

        AlertDialogBuilder builder = dialogBuilder.newDialog(theme)
                .setSingleChoiceItems(adapter, selected, (dialog, which) -> {
                    if (preferences.hasPurchase(R.string.p_purchased_themes) || which < getNumFree()) {
                        callback.themePicked(palette, which);
                    } else {
                        callback.initiateThemePurchase();
                    }
                })
                .setOnDismissListener(dialogInterface -> callback.dismissed());
        if (showNone) {
            builder.setNeutralButton(R.string.none, (dialogInterface, i) -> callback.themePicked(palette, -1));
        }
        dialog = builder.create();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        callback.dismissed();
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

    private int getNumFree() {
        return 2;
    }
}
