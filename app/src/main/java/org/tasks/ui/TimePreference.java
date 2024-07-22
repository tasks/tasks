package org.tasks.ui;

import static org.tasks.time.DateTimeUtils2.currentTimeMillis;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.Preference;

import org.tasks.R;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.kmp.org.tasks.time.DateUtilitiesKt;
import org.tasks.time.DateTime;
import org.tasks.time.LongExtensionsKt;

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
      int noon = new DateTime().startOfDay().withHourOfDay(12).getMillisOfDay();
      millisOfDay = getPersistedInt(noon);
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
        DateUtilitiesKt.getTimeString(
                LongExtensionsKt.withMillisOfDay(currentTimeMillis(), millisOfDay),
                org.tasks.extensions.Context.INSTANCE.is24HourFormat(getContext())
        );
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
