package org.tasks.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.preference.Preference;
import com.todoroo.andlib.utility.DateUtilities;
import org.tasks.R;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.time.DateTime;

public class TimePreference extends Preference {

  private final String summary;
  private int millisOfDay;

  public TimePreference(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePreference);
    summary = a.getString(R.styleable.TimePreference_time_summary);
    a.recycle();
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInteger(index, -1);
  }

  @Override
  public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    if (restoreValue) {
      int startOfDay = new DateTime().startOfDay().getMillisOfDay();
      millisOfDay = getPersistedInt(startOfDay);
    } else {
      millisOfDay = (Integer) defaultValue;
    }

    setMillisOfDay(millisOfDay);
  }

  public int getMillisOfDay() {
    return millisOfDay;
  }

  private void setMillisOfDay(int millisOfDay) {
    this.millisOfDay = millisOfDay;
    String setting =
        DateUtilities.getTimeString(getContext(), new DateTime().withMillisOfDay(millisOfDay));
    setSummary(summary == null ? setting : String.format(summary, setting));
  }

  public void handleTimePickerActivityIntent(Intent data) {
    long timestamp = data.getLongExtra(MyTimePickerDialog.EXTRA_TIMESTAMP, 0L);
    int millisOfDay = new DateTime(timestamp).getMillisOfDay();
    if (callChangeListener(millisOfDay)) {
      persistInt(millisOfDay);
      setMillisOfDay(millisOfDay);
    }
  }
}
