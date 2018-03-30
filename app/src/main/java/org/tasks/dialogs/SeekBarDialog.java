package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.rey.material.widget.Slider;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public class SeekBarDialog extends InjectingNativeDialogFragment {

  private static final String EXTRA_LAYOUT = "extra_layout";
  private static final String EXTRA_INITIAL_VALUE = "extra_initial_value";
  private static final String EXTRA_MIN = "extra_min";
  private static final String EXTRA_MAX = "extra_max";
  private static final String EXTRA_REQUEST_CODE = "extra_request_code";

  @BindView(R.id.slider)
  Slider slider;

  @BindView(R.id.min)
  TextView min;

  @BindView(R.id.max)
  TextView max;

  @Inject DialogBuilder dialogBuilder;
  @Inject Theme theme;
  @Inject Locale locale;
  private int initial;
  private int requestCode;
  private SeekBarCallback callback;

  public static SeekBarDialog newSeekBarDialog(
      int layout, int min, int max, int initial, int requestCode) {
    SeekBarDialog dialog = new SeekBarDialog();
    Bundle args = new Bundle();
    args.putInt(EXTRA_LAYOUT, layout);
    args.putInt(EXTRA_MIN, min);
    args.putInt(EXTRA_MAX, max);
    dialog.setArguments(args);
    dialog.initial = initial;
    dialog.requestCode = requestCode;
    return dialog;
  }

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
    slider.setValueDescriptionProvider(value -> locale.formatNumber(value));
    slider.setValueRange(arguments.getInt(EXTRA_MIN), arguments.getInt(EXTRA_MAX), false);
    min.setText(locale.formatNumber(slider.getMinValue()));
    max.setText(locale.formatNumber(slider.getMaxValue()));
    return dialogBuilder
        .newDialog()
        .setView(view)
        .setPositiveButton(
            android.R.string.ok,
            (dialogInterface, i) -> callback.valueSelected(slider.getValue(), requestCode))
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

  public interface SeekBarCallback {

    void valueSelected(int value, int requestCode);
  }
}
