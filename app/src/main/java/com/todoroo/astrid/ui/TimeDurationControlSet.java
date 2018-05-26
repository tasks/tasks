/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;
import com.todoroo.astrid.ui.NNumberPickerDialog.OnNNumberPickedListener;
import org.tasks.R;
import org.tasks.themes.Theme;

public class TimeDurationControlSet implements OnNNumberPickedListener, View.OnClickListener {

  private final Context context;
  private final Theme theme;
  private final TextView timeButton;
  private int timeDuration;
  private int[] initialValues = null;
  private NNumberPickerDialog dialog = null;

  public TimeDurationControlSet(Context context, View view, int timeButtonId, Theme theme) {
    this.context = context;
    this.theme = theme;

    timeButton = view.findViewById(timeButtonId);
    ((View) timeButton.getParent()).setOnClickListener(this);
  }

  public int getTimeDurationInSeconds() {
    return timeDuration;
  }

  public void setTimeDuration(Integer timeDurationInSeconds) {
    if (timeDurationInSeconds == null) {
      timeDurationInSeconds = 0;
    }

    timeDuration = timeDurationInSeconds;

    Resources r = context.getResources();
    if (timeDurationInSeconds == 0) {
      timeButton.setText(r.getString(R.string.WID_dateButtonUnset));
      return;
    }

    timeButton.setText(DateUtils.formatElapsedTime(timeDuration));
    int hours = timeDuration / 3600;
    int minutes = timeDuration / 60 - 60 * hours;
    initialValues = new int[] {hours, minutes};
  }

  /** Called when NumberPicker activity is completed */
  @Override
  public void onNumbersPicked(int[] values) {
    setTimeDuration(values[0] * 3600 + values[1] * 60);
  }

  /** Called when time button is clicked */
  @Override
  public void onClick(View v) {
    if (dialog == null) {
      dialog =
          new NNumberPickerDialog(
              theme.getThemedDialog(context),
              this,
              context.getResources().getString(R.string.DLG_hour_minutes),
              new int[] {0, 0},
              new int[] {1, 5},
              new int[] {0, 0},
              new int[] {999, 59},
              new String[] {":", null});
      final NumberPicker hourPicker = dialog.getPicker(0);
      final NumberPicker minutePicker = dialog.getPicker(1);
      minutePicker.setFormatter(value -> String.format("%02d", value));
      minutePicker.setOnChangeListener(
          newVal -> {
            if (newVal < 0) {
              if (hourPicker.getCurrent() == 0) {
                return 0;
              }
              hourPicker.setCurrent(hourPicker.getCurrent() - 1);
              return 60 + newVal;
            } else if (newVal > 59) {
              hourPicker.setCurrent(hourPicker.getCurrent() + 1);
              return newVal % 60;
            }
            return newVal;
          });
    }

    if (initialValues != null) {
      dialog.setInitialValues(initialValues);
    }

    theme.applyToContext(dialog.getContext());
    dialog.show();
  }
}
