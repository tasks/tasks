package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;

import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;

import javax.inject.Inject;

public class SupportThemePickerDialog extends InjectingDialogFragment implements ThemePickerDialog.ThemePickerCallback {

    private static final String EXTRA_PALETTE = "extra_palette";

    public static SupportThemePickerDialog newSupportThemePickerDialog(ThemePickerDialog.ColorPalette palette) {
        SupportThemePickerDialog dialog = new SupportThemePickerDialog();
        dialog.palette = palette;
        return dialog;
    }

    @Inject ThemePickerDialog themePickerDialog;

    private ThemePickerDialog.ColorPalette palette;
    private ThemePickerDialog.ThemePickerCallback callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            palette = (ThemePickerDialog.ColorPalette) savedInstanceState.getSerializable(EXTRA_PALETTE);
        }
        return themePickerDialog.createDialog(palette, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (ThemePickerDialog.ThemePickerCallback) activity;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(EXTRA_PALETTE, palette);
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    @Override
    public void themePicked(ThemePickerDialog.ColorPalette palette, int index) {
        callback.themePicked(palette, index);
    }

    @Override
    public void initiateThemePurchase() {
        callback.initiateThemePurchase();
    }
}
