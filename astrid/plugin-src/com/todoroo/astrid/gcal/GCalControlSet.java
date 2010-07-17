package com.todoroo.astrid.gcal;

import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.gcal.Calendars.CalendarResult;
import com.todoroo.astrid.model.Task;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GCalControlSet implements TaskEditControlSet {

    /** If task has no estimated time, how early to set a task in calendar (seconds)*/
    private static final int DEFAULT_CAL_TIME = 3600;

    // --- instance variables

    private final Activity activity;

    private final CalendarResult calendars;

    private final CheckBox addToCalendar;

    private final Spinner calendarSelector;

    private final Button viewCalendarEvent;

    public GCalControlSet(Activity activity, ViewGroup parent) {
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.gcal_control, parent, true);

        this.addToCalendar = (CheckBox) activity.findViewById(R.id.add_to_calendar);
        this.calendarSelector = (Spinner) activity.findViewById(R.id.calendars);
        this.viewCalendarEvent = (Button) activity.findViewById(R.id.view_calendar_event);

        calendars = Calendars.getCalendars(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, calendars.calendars);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarSelector.setAdapter(adapter);

        addToCalendar.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                calendarSelector.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

    }

    /** Take the values from the model and set the calendar start and end times
     * based on these. Sets keys 'dtstart' and 'dtend'.
     *
     * @param preferred preferred due date or null
     * @param definite definite due date or null
     * @param estimatedSeconds estimated duration or null
     * @param values
     */
    @SuppressWarnings({ "nls", "unused" })
    public void createCalendarStartEndTimes(Date preferred, Date definite,
            Integer estimatedSeconds, ContentValues values) {
        FlurryAgent.onEvent("create-calendar-event");

        Long deadlineDate = null;
        if (preferred != null && preferred.after(new Date()))
            deadlineDate = preferred.getTime();
        else if (definite != null)
            deadlineDate = definite.getTime();
        else
            deadlineDate = System.currentTimeMillis() + 24*3600*1000L;

        int estimatedTime = DEFAULT_CAL_TIME;
        if(estimatedSeconds != null && estimatedSeconds > 0) {
            estimatedTime = estimatedSeconds;
        }
        values.put("dtstart", deadlineDate - estimatedTime * 1000L);
        values.put("dtend", deadlineDate);
    }

    @SuppressWarnings("unused")
    protected void onPause() {
        // create calendar event
        /*if(addToCalendar.isChecked() && model.getCalendarUri() == null) {

            Uri uri = Uri.parse("content://calendar/events");
            ContentResolver cr = getContentResolver();

            ContentValues values = new ContentValues();
            values.put("title", title.getText().toString());
            values.put("calendar_id", Preferences.getDefaultCalendarIDSafe(this));
            values.put("description", notes.getText().toString());
            values.put("hasAlarm", 0);
            values.put("transparency", 0);
            values.put("visibility", 0);

            createCalendarStartEndTimes(model.getPreferredDueDate(),
                    model.getDefiniteDueDate(), model.getEstimatedSeconds(),
                    values);

            Uri result = null;
            try{
                result = cr.insert(uri, values);
                model.setCalendarUri(result.toString());
            } catch (IllegalArgumentException e) {
                Log.e("astrid", "Error creating calendar event!", e);
            }
        } */

        // save save save

        /* if(addToCalendar.isChecked() && model.getCalendarUri() != null) {
            Uri result = Uri.parse(model.getCalendarUri());
            Intent intent = new Intent(Intent.ACTION_EDIT, result);

            ContentValues values = new ContentValues();
            createCalendarStartEndTimes(model.getPreferredDueDate(),
                    model.getDefiniteDueDate(), model.getEstimatedSeconds(),
                    values);

            intent.putExtra("beginTime", values.getAsLong("dtstart"));
            intent.putExtra("endTime", values.getAsLong("dtend"));

            startActivity(intent);
        } */

    }

    @Override
    public void readFromTask(Task task) {
        //
    }

    @Override
    public void writeToModel(Task task) {
        //
    }
}