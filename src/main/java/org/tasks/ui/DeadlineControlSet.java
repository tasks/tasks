package org.tasks.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.tasks.R;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class DeadlineControlSet extends TaskEditControlSetBase {

    private static final String FRAG_TAG_PICK_A_DATE = "frag_tag_pick_a_date";
    private static final String FRAG_TAG_PICK_A_TIME = "frag_tag_pick_a_time";

    private final List<String> dueDateOptions = new ArrayList<>();
    private final List<String> dueTimeOptions = new ArrayList<>();
    private final List<String> dueTimeHint;
    private final int dateShortcutMorning;
    private final int dateShortcutAfternoon;
    private final int dateShortcutEvening;
    private final int dateShortcutNight;
    private final String nightString;
    private final String eveningString;
    private final String afternoonString;
    private final String morningString;
    private final String noTimeString;
    private final String todayString;
    private final String tomorrowString;

    private Activity activity;
    private ActivityPreferences preferences;
    private Spinner dueDateSpinner;
    private Spinner dueTimeSpinner;
    private View clearButton;
    private ArrayAdapter<String> dueDateAdapter;
    private ArrayAdapter<String> dueTimeAdapter;
    private long date = 0;
    private int time = -1;

    public DeadlineControlSet(Activity activity, ActivityPreferences preferences) {
        super(activity, R.layout.control_set_deadline);
        this.activity = activity;
        this.preferences = preferences;
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
        dueDateOptions.addAll(asList(
                "",
                todayString,
                tomorrowString,
                "",
                activity.getString(R.string.pick_a_date)));
        dueTimeOptions.addAll(asList(
                "",
                noTimeString,
                morningString,
                afternoonString,
                eveningString,
                nightString,
                activity.getString(R.string.pick_a_time)));
    }

    private String getTimeHint(int millisOfDay) {
        DateTime dateTime = newDateTime().withMillisOfDay(millisOfDay);
        return DateUtilities.getTimeString(activity, dateTime);
    }

    private void refreshDisplayView() {
        updateDueDateOptions();
        updateDueTimeOptions();
        clearButton.setVisibility(date > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateDueDateOptions() {
        DateTime today = newDateTime().startOfDay();
        String nextWeekString = activity.getString(R.string.next, today.plusWeeks(1).toString("EEEE"));
        if (date == 0) {
            dueDateOptions.set(0, activity.getString(R.string.TEA_no_date));
        } else {
            if (date == today.getMillis()) {
                dueDateOptions.set(0, todayString);
            } else if (date == today.plusDays(1).getMillis()) {
                dueDateOptions.set(0, tomorrowString);
            } else if (date == today.plusWeeks(1).getMillis()) {
                dueDateOptions.set(0, nextWeekString);
            } else {
                dueDateOptions.set(0, DateUtilities.getLongDateStringHideYear(newDateTime(date)));
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
                dueTimeOptions.set(0, DateUtilities.getTimeString(activity, newDateTime().withMillisOfDay(time)));
            }
        }
        dueTimeAdapter.notifyDataSetChanged();
        dueTimeSpinner.setSelection(0);
    }

    @Override
    protected void afterInflate() {
        View view = getView();
        clearButton = view.findViewById(R.id.clear_due_date);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date = 0;
                time = -1;
                refreshDisplayView();
            }
        });
        dueDateSpinner = (Spinner) view.findViewById(R.id.due_date);
        dueDateAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, dueDateOptions) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) LayoutInflater.from(activity).inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(dueDateOptions.get(selectedItemPosition));
                if (date == 0) {
                    dueDateSpinner.setAlpha(0.5f);
                    dueDateSpinner.setBackgroundDrawable(getThemedUnderline());
                } else {
                    dueDateSpinner.setAlpha(1.0f);
                    if (date < newDateTime().startOfDay().getMillis()) {
                        dueDateSpinner.setBackgroundDrawable(getRedUnderline());
                        tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                    } else {
                        dueDateSpinner.setBackgroundDrawable(getThemedUnderline());
                        tv.setTextColor(themeColor);
                    }
                }
                return tv;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
                View v;

                if (position == 0) {
                    TextView tv = new TextView(getContext());
                    tv.setHeight(0);
                    tv.setVisibility(View.GONE);
                    v = tv;
                } else {
                    TextView tv = (TextView) LayoutInflater.from(activity).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                    tv.setText(dueDateOptions.get(position));
                    tv.setTextColor(themeColor);
                    v = tv;
                }

                parent.setVerticalScrollBarEnabled(false);
                return v;
            }
        };
        dueDateSpinner.setAdapter(dueDateAdapter);

        dueTimeSpinner = (Spinner) view.findViewById(R.id.due_time);
        dueTimeAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, dueTimeOptions) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) LayoutInflater.from(activity).inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(dueTimeOptions.get(selectedItemPosition));
                if (time == -1) {
                    dueTimeSpinner.setAlpha(0.5f);
                    dueTimeSpinner.setBackgroundDrawable(getThemedUnderline());
                } else {
                    dueTimeSpinner.setAlpha(1.0f);
                    if (newDateTime(date).withMillisOfDay(time).isBeforeNow()) {
                        dueTimeSpinner.setBackgroundDrawable(getRedUnderline());
                        tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                    } else {
                        dueTimeSpinner.setBackgroundDrawable(getThemedUnderline());
                        tv.setTextColor(themeColor);
                    }
                }
                return tv;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
                View v;

                if (position == 0) {
                    TextView tv = new TextView(getContext());
                    tv.setHeight(0);
                    tv.setVisibility(View.GONE);
                    v = tv;
                } else {
                    ViewGroup vg = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.simple_spinner_dropdown_item, parent, false);
                    ((TextView) vg.findViewById(R.id.text1)).setText(dueTimeOptions.get(position));
                    if (position < dueTimeHint.size()) {
                        ((TextView) vg.findViewById(R.id.text2)).setText(dueTimeHint.get(position));
                    }
                    v = vg;
                }

                parent.setVerticalScrollBarEnabled(false);
                return v;
            }
        };
        dueTimeSpinner.setAdapter(dueTimeAdapter);

        dueDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
                        MyDatePickerDialog dialog = new MyDatePickerDialog();
                        DateTime initial = date > 0 ? newDateTime(date) : today;
                        dialog.initialize(new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
                                setDate(new DateTime(year, month + 1, day, 0, 0, 0, 0).getMillis());
                            }
                        }, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth());
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                refreshDisplayView();
                            }
                        });
                        if (preferences.isDarkTheme()) {
                            dialog.setAccentColor(activity.getResources().getColor(R.color.black_text_hint));
                        }
                        dialog.show(activity.getFragmentManager(), FRAG_TAG_PICK_A_DATE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        dueTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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
                        MyTimePickerDialog dialog = new MyTimePickerDialog();
                        int initialHours = 0;
                        int initialMinutes = 0;
                        if (time >= 0) {
                            DateTime initial = newDateTime(date).withMillisOfDay(time);
                            initialHours = initial.getHourOfDay();
                            initialMinutes = initial.getMinuteOfHour();
                        }
                        dialog.initialize(new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute, int seconds) {
                                setTime((int) TimeUnit.HOURS.toMillis(hour) + (int) TimeUnit.MINUTES.toMillis(minute));
                            }
                        }, initialHours, initialMinutes, 0, DateFormat.is24HourFormat(activity));
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                refreshDisplayView();
                            }
                        });
                        if (preferences.isDarkTheme()) {
                            dialog.setAccentColor(activity.getResources().getColor(R.color.black_text_hint));
                        }
                        dialog.show(activity.getFragmentManager(), FRAG_TAG_PICK_A_TIME);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private Drawable getThemedUnderline() {
        Drawable drawable = DrawableCompat.wrap(activity.getResources().getDrawable(R.drawable.textfield_underline_black));
        DrawableCompat.setTint(drawable, activity.getResources().getColor(preferences.isDarkTheme()
                ? android.R.color.white
                : android.R.color.black));
        return drawable;
    }

    private Drawable getRedUnderline() {
        Drawable drawable = DrawableCompat.wrap(activity.getResources().getDrawable(R.drawable.textfield_underline_black));
        DrawableCompat.setTint(drawable, activity.getResources().getColor(R.color.overdue));
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

    @Override
    protected void readFromTaskOnInitialize() {
        Long dueDate = model.getDueDate();
        if (dueDate > 0) {
            DateTime dateTime = newDateTime(dueDate);
            date = dateTime.startOfDay().getMillis();
            if (Task.hasDueTime(dateTime.getMillis())) {
                setTime(dateTime.getMillisOfDay());
            } else {
                time = -1;
            }
        } else {
            date = 0;
            time = -1;
        }
        refreshDisplayView();
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        long dueDate = time >= 0
                ? Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDateTime(date).withMillisOfDay(time).getMillis())
                : Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, date);
        if (dueDate != task.getDueDate()) {
            task.setReminderSnooze(0L);
        }
        task.setDueDate(dueDate);
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_schedule_24dp;
    }
}
