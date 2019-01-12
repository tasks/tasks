package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.rey.material.widget.Slider;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public class SeekBarDialog extends InjectingDialogFragment {

  public static final String EXTRA_VALUE = "extra_value";
  private static final String EXTRA_LAYOUT = "extra_layout";
  private static final String EXTRA_MIN = "extra_min";
  private static final String EXTRA_MAX = "extra_max";

  @BindView(R.id.slider)
  Slider slider;

  @BindView(R.id.min)
  TextView min;

  @BindView(R.id.max)
  TextView max;

  @Inject DialogBuilder dialogBuilder;
  @Inject Theme theme;
  @Inject Locale locale;

  static SeekBarDialog newSeekBarDialog(int layout, int min, int max, int initial) {
    SeekBarDialog dialog = new SeekBarDialog();
    Bundle args = new Bundle();
    args.putInt(EXTRA_LAYOUT, layout);
    args.putInt(EXTRA_MIN, min);
    args.putInt(EXTRA_MAX, max);
    args.putInt(EXTRA_VALUE, initial);
    dialog.setArguments(args);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle arguments = getArguments();
    int initial =
        savedInstanceState == null
            ? arguments.getInt(EXTRA_VALUE)
            : savedInstanceState.getInt(EXTRA_VALUE);
    int layout = arguments.getInt(EXTRA_LAYOUT);

    LayoutInflater layoutInflater = theme.getLayoutInflater(getActivity());
    View view = layoutInflater.inflate(layout, null);
    ButterKnife.bind(this, view);

    slider.setValueDescriptionProvider(value -> locale.formatNumber(value));
    slider.setValueRange(arguments.getInt(EXTRA_MIN), arguments.getInt(EXTRA_MAX), false);
    slider.setValue(initial, true);
    min.setText(locale.formatNumber(slider.getMinValue()));
    max.setText(locale.formatNumber(slider.getMaxValue()));
    return dialogBuilder
        .newDialog()
        .setView(view)
        .setOnCancelListener(this::sendResult)
        .setPositiveButton(android.R.string.ok, this::sendResult)
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    sendResult(dialog);
  }

  private void sendResult(DialogInterface d, int... i) {
    Intent data = new Intent();
    data.putExtra(EXTRA_VALUE, slider.getValue());
    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_VALUE, slider.getValue());
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
