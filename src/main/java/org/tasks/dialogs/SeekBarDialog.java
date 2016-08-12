package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import com.rey.material.widget.Slider;

import org.tasks.R;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SeekBarDialog extends InjectingNativeDialogFragment {

    private static final String EXTRA_LAYOUT = "extra_layout";
    private static final String EXTRA_INITIAL_VALUE = "extra_initial_value";
    private static final String EXTRA_REQUEST_CODE = "extra_request_code";

    public static SeekBarDialog newSeekBarDialog(int layout, int initial, int requestCode) {
        SeekBarDialog dialog = new SeekBarDialog();
        Bundle args = new Bundle();
        args.putInt(EXTRA_LAYOUT, layout);
        dialog.setArguments(args);
        dialog.initial = initial;
        dialog.requestCode = requestCode;
        return dialog;
    }

    public interface SeekBarCallback {
        void valueSelected(int value, int requestCode);
    }

    @BindView(R.id.slider) Slider slider;

    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;

    private int initial;
    private int requestCode;
    private SeekBarCallback callback;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (savedInstanceState != null) {
            initial = savedInstanceState.getInt(EXTRA_INITIAL_VALUE);
            requestCode = savedInstanceState.getInt(EXTRA_REQUEST_CODE);
        }
        int layout = arguments.getInt(EXTRA_LAYOUT);

        LayoutInflater layoutInflater = theme.getLayoutInflater(getActivity());
        View view = layoutInflater.inflate(layout, null);
        ButterKnife.bind(this, view);

        slider.setValue(initial, true);
        return dialogBuilder.newDialog()
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> callback.valueSelected(slider.getValue(), requestCode))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (SeekBarCallback) activity;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_INITIAL_VALUE, slider.getValue());
        outState.putInt(EXTRA_REQUEST_CODE, requestCode);
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
