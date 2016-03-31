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

    public interface ThemePickerCallback {
        void themePicked(Theme theme);

        void initiateThemePurchase();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForApplication Context context;
    @Inject Preferences preferences;
    @Inject ThemeManager themeManager;

    private ThemePickerCallback callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final String[] themes = context.getResources().getStringArray(R.array.themes);

        final boolean purchasedThemes = preferences.hasPurchase(R.string.p_purchased_themes);

        ListAdapter adapter = new ArrayAdapter<String>(context, R.layout.color_selection_row, themes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;

                if (row == null) {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    row = inflater.inflate(R.layout.color_selection_row, parent, false);
                }

                Resources resources = context.getResources();
                Theme theme = themeManager.getTheme(position);
                ImageView primary = (ImageView) row.findViewById(R.id.color_primary);
                Drawable original = resources.getDrawable(purchasedThemes || position < 2
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, theme.getPrimaryColor());
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
                            callback.themePicked(themeManager.getTheme(which));
                        } else {
                            callback.initiateThemePurchase();
                        }
                    }
                })
                .show();
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
