package com.todoroo.astrid.gcal;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ExceptionService;
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

    @Autowired
    private ExceptionService exceptionService;

    private final Activity activity;

    private Uri calendarUri = null;

    private Task myTask;
    private final CalendarResult calendars;
    private final CheckBox addToCalendar;
    private final Spinner calendarSelector;
    private final Button viewCalendarEvent;

    public GCalControlSet(final Activity activity, ViewGroup parent) {
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.gcal_control, parent, true);

        this.addToCalendar = (CheckBox) activity.findViewById(R.id.add_to_calendar);
        this.calendarSelector = (Spinner) activity.findViewById(R.id.calendars);
        this.viewCalendarEvent = (Button) activity.findViewById(R.id.view_calendar_event);

        calendars = Calendars.getCalendars();
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

        viewCalendarEvent.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("nls")
            @Override
            public void onClick(View v) {
                if(calendarUri == null)
                    return;

                ContentResolver cr = activity.getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart", "dtend" },
                        null, null, null);

                Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                try {
                    if(cursor.getCount() == 0) {
                        // event no longer exists, recreate it
                        calendarUri = null;
                        writeToModel(myTask);
                        return;
                    }
                    cursor.moveToFirst();
                    intent.putExtra("beginTime", cursor.getLong(0));
                    intent.putExtra("endTime", cursor.getLong(1));
                } finally {
                    cursor.close();
                }

                activity.startActivity(intent);
            }
        });
    }

    @Override
    public void readFromTask(Task task) {
        this.myTask = task;
        String uri = task.getValue(Task.CALENDAR_URI);
        if(!TextUtils.isEmpty(uri)) {
            try {
                calendarUri = Uri.parse(uri);
                addToCalendar.setVisibility(View.GONE);
                calendarSelector.setVisibility(View.GONE);
                viewCalendarEvent.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                exceptionService.reportError("unable-to-parse-calendar: " +  //$NON-NLS-1$
                        task.getValue(Task.CALENDAR_URI), e);
            }
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void writeToModel(Task task) {
        if(addToCalendar.isChecked() && calendarUri == null) {
            FlurryAgent.onEvent("create-calendar-event");

            try{
                Uri uri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);
                ContentResolver cr = activity.getContentResolver();

                ContentValues values = new ContentValues();
                values.put("title", task.getValue(Task.TITLE));
                String calendarId = calendars.calendarIds[calendarSelector.getSelectedItemPosition()];
                Calendars.setDefaultCalendar(calendarId);
                values.put("calendar_id", calendarId);
                values.put("description", task.getValue(Task.NOTES));
                values.put("hasAlarm", 0);
                values.put("transparency", 0);
                values.put("visibility", 0);

                createStartAndEndDate(task, values);

                calendarUri = cr.insert(uri, values);
                task.setValue(Task.CALENDAR_URI, calendarUri.toString());

                // pop up the new event
                Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                intent.putExtra("beginTime", values.getAsLong("dtstart"));
                intent.putExtra("endTime", values.getAsLong("dtend"));
                activity.startActivity(intent);
            } catch (Exception e) {
                exceptionService.displayAndReportError(activity,
                        activity.getString(R.string.gcal_TEA_error), e);
            }
        }
    }

    private void createStartAndEndDate(Task task, ContentValues values) {
        long dueDate = task.getValue(Task.DUE_DATE);
        if(task.hasDueDate()) {
            if(task.hasDueTime()) {
                int estimatedTime = task.getValue(Task.ESTIMATED_SECONDS);
                if(estimatedTime <= 0)
                    estimatedTime = DEFAULT_CAL_TIME;
                values.put("dtstart", dueDate - estimatedTime);
                values.put("dtend", dueDate);
            } else {
                values.put("dtstart", dueDate);
                values.put("dtend", dueDate);
                values.put("allDay", "1");
            }
        }
    }
}