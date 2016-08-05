package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeekBarDialog extends InjectingDialogFragment {

    private static final String EXTRA_MIN_VALUE = "extra_min_value";
    private static final String EXTRA_MAX_VALUE = "extra_max_value";
    private static final String EXTRA_INITIAL_VALUE = "extra_initial_value";
    public static final String EXTRA_VALUE = "extra_value";

    public static SeekBarDialog newSeekBarDialog(int minValue, int maxValue, int initial) {
        SeekBarDialog dialog = new SeekBarDialog();
        Bundle args = new Bundle();
        args.putInt(EXTRA_MIN_VALUE, minValue);
        args.putInt(EXTRA_MAX_VALUE, maxValue);
        dialog.setArguments(args);
        dialog.initial = initial;
        return dialog;
    }

    @BindView(R.id.seekbar) SeekBar seekBar;
    @BindView(R.id.min) TextView min;
    @BindView(R.id.max) TextView max;

    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;

    private int minValue;
    private int maxValue;
    private int initial;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = theme.getLayoutInflater(getContext());
        View view = layoutInflater.inflate(R.layout.dialog_seekbar, null);
        ButterKnife.bind(this, view);

        Bundle arguments = getArguments();
        if (savedInstanceState != null) {
            initial = savedInstanceState.getInt(EXTRA_INITIAL_VALUE);
        }
        minValue = arguments.getInt(EXTRA_MIN_VALUE);
        maxValue = arguments.getInt(EXTRA_MAX_VALUE);
        min.setText(NumberFormat.getIntegerInstance().format(minValue));
        max.setText(NumberFormat.getIntegerInstance().format(maxValue));
        seekBar.setMax(maxValue - minValue);
        seekBar.setProgress(initial);
        return dialogBuilder.newDialog()
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, new Intent() {{
                            putExtra(EXTRA_VALUE, seekBar.getProgress() + minValue);
                        }});
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_INITIAL_VALUE, seekBar.getProgress());
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }
}
