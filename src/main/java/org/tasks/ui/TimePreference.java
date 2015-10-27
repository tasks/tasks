package org.tasks.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;

import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.R;
import org.tasks.activities.TimePickerActivity;
import org.tasks.time.DateTime;

public class TimePreference extends Preference {

    private int millisOfDay;
    private String summary;

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimePreference);
        summary = a.getString(R.styleable.TimePreference_summary);
        a.recycle();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            int noon = new DateTime().startOfDay().withHourOfDay(12).getMillisOfDay();
            millisOfDay = getPersistedInt(noon);
        } else {
            millisOfDay = Integer.parseInt((String) defaultValue);
        }

        setMillisOfDay(millisOfDay);
    }

    public int getMillisOfDay() {
        return millisOfDay;
    }

    public void handleTimePickerActivityIntent(Intent data) {
        long timestamp = data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L);
        int millisOfDay = new DateTime(timestamp).getMillisOfDay();
        if (callChangeListener(millisOfDay)) {
            persistInt(millisOfDay);
            setMillisOfDay(millisOfDay);
        }
    }

    private void setMillisOfDay(int millisOfDay) {
        this.millisOfDay = millisOfDay;
        String setting = DateUtilities.getTimeString(getContext(), new DateTime().withMillisOfDay(millisOfDay));
        setSummary(summary == null ? setting : String.format(summary, setting));
    }
}
