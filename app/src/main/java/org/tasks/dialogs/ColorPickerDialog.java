package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;
import org.tasks.ui.SingleCheckedArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;

public class ColorPickerDialog extends InjectingDialogFragment {

    private static final String EXTRA_ITEMS = "extra_items";
    private static final String EXTRA_SELECTED = "extra_selected";
    private static final String EXTRA_SHOW_NONE = "extra_show_none";

    public interface Pickable extends Parcelable {
        String getName();

        int getPickerColor();

        boolean isFree();

        int getIndex();
    }

    public interface ThemePickerCallback {
        void themePicked(Pickable pickable);

        void initiateThemePurchase();

        void dismissed();
    }

    public static ColorPickerDialog newColorPickerDialog(List<? extends Pickable> items, boolean showNone, int selection) {
        ColorPickerDialog dialog = new ColorPickerDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(EXTRA_ITEMS, new ArrayList<Pickable>(items));
        args.putInt(EXTRA_SELECTED, selection);
        args.putBoolean(EXTRA_SHOW_NONE, showNone);
        dialog.setArguments(args);
        return dialog;
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;
    @Inject Preferences preferences;
    @Inject Theme theme;

    private ThemePickerCallback callback;
    private SingleCheckedArrayAdapter adapter;
    private Dialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Bundle arguments = getArguments();
        final List<Pickable> items = arguments.getParcelableArrayList(EXTRA_ITEMS);
        boolean showNone = arguments.getBoolean(EXTRA_SHOW_NONE);
        int selected = arguments.getInt(EXTRA_SELECTED, -1);

        adapter = new SingleCheckedArrayAdapter(context, transform(items, Pickable::getName), theme.getThemeAccent()) {
            @Override
            protected int getDrawable(int position) {
                return preferences.hasPurchase(R.string.p_purchased_themes) || items.get(position).isFree()
                        ? R.drawable.ic_lens_black_24dp
                        : R.drawable.ic_vpn_key_black_24dp;
            }

            @Override
            protected int getDrawableColor(int position) {
                return items.get(position).getPickerColor();
            }
        };

        AlertDialogBuilder builder = dialogBuilder.newDialog(theme)
                .setSingleChoiceItems(adapter, selected, (dialog, which) -> {
                    Pickable picked = items.get(which);
                    if (preferences.hasPurchase(R.string.p_purchased_themes) || picked.isFree()) {
                        callback.themePicked(picked);
                    } else {
                        callback.initiateThemePurchase();
                    }
                })
                .setOnDismissListener(dialogInterface -> callback.dismissed());
        if (showNone) {
            builder.setNeutralButton(R.string.none, (dialogInterface, i) -> callback.themePicked(null));
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
}
