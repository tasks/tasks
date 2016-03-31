package org.tasks.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.activities.DatePickerActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnItemSelected;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.preferences.ResourceResolver.getData;

public class DeadlineControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_when_pref;

    private static final int REQUEST_DATE = 504;
    private static final int REQUEST_TIME = 505;
    private static final String EXTRA_DATE = "extra_date";
    private static final String EXTRA_TIME = "extra_time";

    private List<String> dueDateOptions = new ArrayList<>();
    private List<String> dueTimeOptions = new ArrayList<>();
    private List<String> dueTimeHint = new ArrayList<>();
    private int dateShortcutMorning;
    private int dateShortcutAfternoon;
    private int dateShortcutEvening;
    private int dateShortcutNight;
    private String nightString;
    private String eveningString;
    private String afternoonString;
    private String morningString;
    private String noTimeString;
    private String todayString;
    private String tomorrowString;

    @Inject Preferences preferences;
    @Inject @ForActivity Context context;

    @Bind(R.id.due_date) Spinner dueDateSpinner;
    @Bind(R.id.due_time) Spinner dueTimeSpinner;
    @Bind(R.id.clear) View clearButton;

    private ArrayAdapter<String> dueDateAdapter;
    private ArrayAdapter<String> dueTimeAdapter;
    private long date = 0;
    private int time = -1;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        dateShortcutMorning = preferences.getDateShortcutMorning();
        dateShortcutAfternoon = preferences.getDateShortcutAfternoon();
        dateShortcutEvening = preferences.getDateShortcutEvening();
        dateShortcutNight = preferences.getDateShortcutNight();
        dueTimeHint = asList(
                "",
                "",
                getTimeHint(dateShortcutMorning),
                getTimeHint(dateShortcutAfternoon),
                getTimeHint(dateShortcutEvening),
                getTimeHint(dateShortcutNight));
        nightString = activity.getString(R.string.date_shortcut_night);
        eveningString = activity.getString(R.string.date_shortcut_evening);
        afternoonString = activity.getString(R.string.date_shortcut_afternoon);
        morningString = activity.getString(R.string.date_shortcut_morning);
        noTimeString = activity.getString(R.string.TEA_no_time);
        todayString = activity.getString(R.string.today);
        tomorrowString = activity.getString(R.string.tomorrow);
        dueDateOptions = newArrayList(
                "",
                todayString,
                tomorrowString,
                "",
                activity.getString(R.string.pick_a_date));
        dueTimeOptions = newArrayList(
                "",
                noTimeString,
                morningString,
                afternoonString,
                eveningString,
                nightString,
                activity.getString(R.string.pick_a_time));
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            date = savedInstanceState.getLong(EXTRA_DATE);
            time = savedInstanceState.getInt(EXTRA_TIME);
        }
        final int themeColor = getData(context, R.attr.asTextColor);
        final int overdueColor = context.getResources().getColor(R.color.overdue);
        dueDateAdapter = new HiddenTopArrayAdapter<String>(context, android.R.layout.simple_spinner_item, dueDateOptions) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(dueDateOptions.get(selectedItemPosition));
                if (date == 0) {
                    dueDateSpinner.setAlpha(0.5f);
                    dueDateSpinner.setBackgroundDrawable(getUnderline(themeColor));
                } else {
                    dueDateSpinner.setAlpha(1.0f);
                    if (date < newDateTime().startOfDay().getMillis()) {
                        dueDateSpinner.setBackgroundDrawable(getUnderline(overdueColor));
                        tv.setTextColor(overdueColor);
                    } else {
                        dueDateSpinner.setBackgroundDrawable(getUnderline(themeColor));
                        tv.setTextColor(themeColor);
                    }
                }
                return tv;
            }
        };
        dueDateSpinner.setAdapter(dueDateAdapter);

        dueTimeAdapter = new HiddenTopArrayAdapter<String>(context, android.R.layout.simple_spinner_item, dueTimeOptions, dueTimeHint) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(dueTimeOptions.get(selectedItemPosition));
                if (time == -1) {
                    dueTimeSpinner.setAlpha(0.5f);
                    dueTimeSpinner.setBackgroundDrawable(getUnderline(themeColor));
                } else {
                    dueTimeSpinner.setAlpha(1.0f);
                    if (newDateTime(date).withMillisOfDay(time).isBeforeNow()) {
                        dueTimeSpinner.setBackgroundDrawable(getUnderline(overdueColor));
                        tv.setTextColor(overdueColor);
                    } else {
                        dueTimeSpinner.setBackgroundDrawable(getUnderline(themeColor));
                        tv.setTextColor(themeColor);
                    }
                }
                return tv;
            }
        };
        dueTimeSpinner.setAdapter(dueTimeAdapter);

        refreshDisplayView();

        return view;
    }

    @OnClick(R.id.clear)
    void clearTime(View view) {
        date = 0;
        time = -1;
        refreshDisplayView();
    }

    @OnItemSelected(R.id.due_date)
    void onDateSelected(int position) {
        DateTime today = newDateTime().startOfDay();
        switch (position) {
            case 0:
                return;
            case 1:
                setDate(today.getMillis());
                break;
            case 2:
                setDate(today.plusDays(1).getMillis());
                break;
            case 3:
                setDate(today.plusWeeks(1).getMillis());
                break;
            case 4:
                startActivityForResult(new Intent(context, DatePickerActivity.class) {{
                    putExtra(DatePickerActivity.EXTRA_TIMESTAMP, date);
                }}, REQUEST_DATE);
                break;
        }
    }

    @OnItemSelected(R.id.due_time)
    void onTimeSelected(int position) {
        switch (position) {
            case 0:
                return;
            case 1:
                setTime(-1);
                break;
            case 2:
                setTime(dateShortcutMorning);
                break;
            case 3:
                setTime(dateShortcutAfternoon);
                break;
            case 4:
                setTime(dateShortcutEvening);
                break;
            case 5:
                setTime(dateShortcutNight);
                break;
            case 6:
                startActivityForResult(new Intent(context, TimePickerActivity.class) {{
                    putExtra(TimePickerActivity.EXTRA_TIMESTAMP, getDueDateTime());
                }}, REQUEST_TIME);
                break;
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_deadline;
    }

    @Override
    protected int getIcon() {
        return R.drawable.ic_schedule_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @Override
    public boolean hasChanges(Task original) {
        return original.getDueDate() != getDueDateTime();
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        if (task.hasDueDate()) {
            DateTime dateTime = newDateTime(task.getDueDate());
            date = dateTime.startOfDay().getMillis();
            time = task.hasDueTime() ? dateTime.getMillisOfDay() : -1;
        } else {
            date = 0;
            time = -1;
        }
    }

    @Override
    public void apply(Task task) {
        long dueDate = getDueDateTime();
        if (dueDate != task.getDueDate()) {
            task.setReminderSnooze(0L);
        }
        task.setDueDate(dueDate);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DATE) {
            if (resultCode == Activity.RESULT_OK) {
                long timestamp = data.getLongExtra(DatePickerActivity.EXTRA_TIMESTAMP, 0L);
                DateTime dateTime = new DateTime(timestamp);
                setDate(dateTime.getMillis());
            } else {
                refreshDisplayView();
            }
        } else if (requestCode == REQUEST_TIME) {
            if (resultCode == Activity.RESULT_OK) {
                long timestamp = data.getLongExtra(TimePickerActivity.EXTRA_TIMESTAMP, 0L);
                DateTime dateTime = new DateTime(timestamp);
                setTime(dateTime.getMillisOfDay());
            } else {
                refreshDisplayView();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private long getDueDateTime() {
        return time >= 0
                ? Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDateTime(date).withMillisOfDay(time).getMillis())
                : Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, date);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(EXTRA_DATE, date);
        outState.putInt(EXTRA_TIME, time);
    }

    private String getTimeHint(int millisOfDay) {
        DateTime dateTime = newDateTime().withMillisOfDay(millisOfDay);
        return DateUtilities.getTimeString(context, dateTime);
    }

    private void refreshDisplayView() {
        updateDueDateOptions();
        updateDueTimeOptions();
        clearButton.setVisibility(date > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateDueDateOptions() {
        DateTime today = newDateTime().startOfDay();
        String nextWeekString = getString(R.string.next, today.plusWeeks(1).toString("EEEE"));
        if (date == 0) {
            dueDateOptions.set(0, getString(R.string.TEA_no_date));
        } else {
            if (date == today.getMillis()) {
                dueDateOptions.set(0, todayString);
            } else if (date == today.plusDays(1).getMillis()) {
                dueDateOptions.set(0, tomorrowString);
            } else if (date == today.plusWeeks(1).getMillis()) {
                dueDateOptions.set(0, nextWeekString);
            } else {
                dueDateOptions.set(0, DateUtilities.getLongDateString(newDateTime(date)));
            }
        }
        dueDateOptions.set(3, nextWeekString);
        dueDateAdapter.notifyDataSetChanged();
        dueDateSpinner.setSelection(0);
    }

    private void updateDueTimeOptions() {
        if (time == -1) {
            dueTimeOptions.set(0, noTimeString);
        } else {
            int compareTime = newDateTime()
                    .withMillisOfDay(time)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0)
                    .getMillisOfDay();
            if (compareTime == dateShortcutMorning) {
                dueTimeOptions.set(0, morningString);
            } else if (compareTime == dateShortcutAfternoon) {
                dueTimeOptions.set(0, afternoonString);
            } else if (compareTime == dateShortcutEvening) {
                dueTimeOptions.set(0, eveningString);
            } else if (compareTime == dateShortcutNight) {
                dueTimeOptions.set(0, nightString);
            } else {
                dueTimeOptions.set(0, DateUtilities.getTimeString(context, newDateTime().withMillisOfDay(time)));
            }
        }
        dueTimeAdapter.notifyDataSetChanged();
        dueTimeSpinner.setSelection(0);
    }

    private Drawable getUnderline(int color) {
        Drawable drawable = DrawableCompat.wrap(context.getResources().getDrawable(R.drawable.textfield_underline_black));
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    private void setDate(long millis) {
        date = millis;
        if (date == 0) {
            time = -1;
        }
        refreshDisplayView();
    }

    private void setTime(int millisOfDay) {
        time = millisOfDay;

        if (date == 0 && time >= 0) {
            DateTime dateTime = newDateTime().withMillisOfDay(time);
            if (dateTime.isBeforeNow()) {
                dateTime = dateTime.plusDays(1);
            }
            date = dateTime.startOfDay().getMillis();
        }

        refreshDisplayView();
    }
}
