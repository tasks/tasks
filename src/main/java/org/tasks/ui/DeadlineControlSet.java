package org.tasks.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.joda.time.DateTime;
import org.tasks.R;
import org.tasks.dialogs.MyDatePickerDialog;
import org.tasks.dialogs.MyTimePickerDialog;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastHoneycomb;
import static java.util.Arrays.asList;
import static org.tasks.date.DateTimeUtils.newDate;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.preferences.ResourceResolver.getResource;

public class DeadlineControlSet extends TaskEditControlSetBase {

    private static final String FRAG_TAG_PICK_A_DATE = "frag_tag_pick_a_date";
    private static final String FRAG_TAG_PICK_A_TIME = "frag_tag_pick_a_time";
    private final List<String> DateOptions = new ArrayList<>();
    private final List<String> startDateOptions = new ArrayList<>();
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
    private boolean isStart = false;

    private FragmentActivity activity;
    private Spinner DateSpinner;
    private Spinner TimeSpinner;
    private View clearButton;
    private ArrayAdapter<String> DateAdapter;
    private ArrayAdapter<String> startDateAdapter;
    private ArrayAdapter<String> TimeAdapter;
    public static long date = 0;
    private static long startDate = 0;
    private int time = -1;

    public DeadlineControlSet(FragmentActivity activity, Preferences preferences, boolean isStart) {
        super(activity, R.layout.control_set_deadline);
        this.activity = activity;
        this.isStart  = isStart;
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
        noTimeString = isStart ? activity.getString(R.string.TEA_start_time) : activity.getString(R.string.TEA_due_time);
        todayString = activity.getString(R.string.today);
        tomorrowString = activity.getString(R.string.tomorrow);
        DateOptions.addAll(asList(
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

    public long returnDate()
    {
        return date;
    }
    private void refreshDisplayView() {
        updateDueDateOptions();
        updateDueTimeOptions();
        clearButton.setVisibility(date > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateDueDateOptions() {
        DateTime today = newDateTime().withMillisOfDay(0);
        String nextWeekString = activity.getString(R.string.next, today.plusWeeks(1).toString("EEEE"));
        if (date == 0) {
            if(isStart)
                DateOptions.set(0, activity.getString(R.string.TEA_start_date)) ;
            else
                DateOptions.set(0, activity.getString(R.string.TEA_due_date));
        }
        else {
            if (date == today.getMillis()) {
                DateOptions.set(0, todayString);
            } else if (date == today.plusDays(1).getMillis()) {
                DateOptions.set(0, tomorrowString);
            } else if (date == today.plusWeeks(1).getMillis()) {
                DateOptions.set(0, nextWeekString);
            } else {
                DateOptions.set(0, DateUtilities.getLongDateStringHideYear(newDate(date)));
            }

        }
        DateOptions.set(3, nextWeekString);
        DateAdapter.notifyDataSetChanged();
        DateSpinner.setSelection(0);

    }

    public int CompareDates(long startDate, long dueDate)
    {
        if(startDate >= date)
            return -1;
        else
            return 0;

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
        TimeAdapter.notifyDataSetChanged();
        TimeSpinner.setSelection(0);
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
        DateSpinner = (Spinner) view.findViewById(R.id.due_date);
        DateAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, DateOptions) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) LayoutInflater.from(activity).inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(DateOptions.get(selectedItemPosition));
                if (atLeastHoneycomb()) {
                    if (date == 0) {
                        DateSpinner.setAlpha(0.5f);
                        DateSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                    } else {
                        DateSpinner.setAlpha(1.0f);
                        if (date < newDateTime().withMillisOfDay(0).getMillis()) {
                            DateSpinner.setBackgroundResource(R.drawable.textfield_underline_red);
                            tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                        } else {
                            DateSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                            tv.setTextColor(themeColor);
                        }
                    }
                } else {
                    if (date == 0) {
                        DateSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                        tv.setTextColor(unsetColor);
                    } else if (date < newDateTime().withMillisOfDay(0).getMillis()) {
                        DateSpinner.setBackgroundResource(R.drawable.textfield_underline_red);
                        tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                    } else {
                        DateSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
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
                    tv.setText(DateOptions.get(position));
                    tv.setTextColor(themeColor);
                    v = tv;
                }

                parent.setVerticalScrollBarEnabled(false);
                return v;
            }
        };
        DateSpinner.setAdapter(DateAdapter);

        TimeSpinner = (Spinner) view.findViewById(R.id.due_time);
        TimeAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, dueTimeOptions) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                int selectedItemPosition = position;
                if (parent instanceof AdapterView) {
                    selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
                }
                TextView tv = (TextView) LayoutInflater.from(activity).inflate(android.R.layout.simple_spinner_item, parent, false);
                tv.setText(dueTimeOptions.get(selectedItemPosition));
                if (atLeastHoneycomb()) {
                    if (time == -1) {
                        TimeSpinner.setAlpha(0.5f);
                        TimeSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                    } else {
                        TimeSpinner.setAlpha(1.0f);
                        if (newDateTime(date).withMillisOfDay(time).isBeforeNow()) {
                            TimeSpinner.setBackgroundResource(R.drawable.textfield_underline_red);
                            tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                        } else {
                            TimeSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                            tv.setTextColor(themeColor);
                        }
                    }
                } else {
                    if (time == -1) {
                        TimeSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
                        tv.setTextColor(unsetColor);
                    } else if (newDateTime(date).withMillisOfDay(time).isBeforeNow()) {
                        TimeSpinner.setBackgroundResource(R.drawable.textfield_underline_red);
                        tv.setTextColor(activity.getResources().getColor(R.color.overdue));
                    } else {
                        TimeSpinner.setBackgroundResource(getResource(activity, R.attr.textfield_underline));
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
        TimeSpinner.setAdapter(TimeAdapter);

        DateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DateTime today = newDateTime().withMillisOfDay(0);


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
                            }, initial.getYear(), initial.getMonthOfYear() - 1, initial.getDayOfMonth(), false);
                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    refreshDisplayView();
                                }
                            });
                            dialog.show(activity.getSupportFragmentManager(), FRAG_TAG_PICK_A_DATE);
                            break;
                    }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        TimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                            public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute) {
                                setTime((int) TimeUnit.HOURS.toMillis(hour) + (int) TimeUnit.MINUTES.toMillis(minute));
                            }
                        }, initialHours, initialMinutes, DateFormat.is24HourFormat(activity), false);
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                refreshDisplayView();
                            }
                        });
                        dialog.show(activity.getSupportFragmentManager(), FRAG_TAG_PICK_A_TIME);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setDate(long millis) {

       /* int result = 0;

        if(!isStart){
            if(startDate!= 0)
            {
                result = CompareDates(startDate, date);
            }
        }
        else
        {
            startDate = date;
        }
        if(result != -1) {*/

            date = millis;
            if (date == 0) {
                time = -1;
            }
            refreshDisplayView();
       /* }
        else
        {
            final AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this.activity);
            dlgAlert.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dlgAlert.setMessage("Cannot set a due date with a value less than the start date");
                            dlgAlert.setCancelable(true);
                            dlgAlert.create().show();

                        }
                    });
        }*/
    }

    private void setTime(int millisOfDay) {
        time = millisOfDay;

        if (date == 0 && time >= 0) {
            DateTime dateTime = newDateTime().withMillisOfDay(time);
            if (dateTime.isBeforeNow()) {
                dateTime = dateTime.plusDays(1);
            }
            date = dateTime.withMillisOfDay(0).getMillis();
        }

        refreshDisplayView();
    }

    @Override
    protected void readFromTaskOnInitialize() {
        Long dueDate = model.getDueDate();
        if (dueDate > 0) {
            DateTime dateTime = newDateTime(dueDate);
            date = dateTime.withMillisOfDay(0).getMillis();
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
        return R.attr.ic_action_clock;
    }
}
