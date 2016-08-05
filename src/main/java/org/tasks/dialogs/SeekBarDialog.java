package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import com.rey.material.widget.Slider;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeekBarDialog extends InjectingDialogFragment {

    private static final String EXTRA_LAYOUT = "extra_layout";
    private static final String EXTRA_INITIAL_VALUE = "extra_initial_value";

    public static final String EXTRA_VALUE = "extra_value";

    public static SeekBarDialog newSeekBarDialog(int layout, int initial) {
        SeekBarDialog dialog = new SeekBarDialog();
        Bundle args = new Bundle();
        args.putInt(EXTRA_LAYOUT, layout);
        dialog.setArguments(args);
        dialog.initial = initial;
        return dialog;
    }

    @BindView(R.id.slider) Slider slider;

    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;

    private int initial;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (savedInstanceState != null) {
            initial = savedInstanceState.getInt(EXTRA_INITIAL_VALUE);
        }
        int layout = arguments.getInt(EXTRA_LAYOUT);

        LayoutInflater layoutInflater = theme.getLayoutInflater(getContext());
        View view = layoutInflater.inflate(layout, null);
        ButterKnife.bind(this, view);

        slider.setValue(initial, true);
        return dialogBuilder.newDialog()
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, new Intent() {{
                            putExtra(EXTRA_VALUE, slider.getValue());
                        }});
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_INITIAL_VALUE, slider.getValue());
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
