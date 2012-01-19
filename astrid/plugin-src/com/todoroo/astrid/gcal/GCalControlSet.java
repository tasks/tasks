package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.Calendars.CalendarResult;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.ui.PopupControlSet;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GCalControlSet extends PopupControlSet {

    // --- instance variables

    @Autowired
    private ExceptionService exceptionService;

    private final Activity activity;

    private Uri calendarUri = null;

    private Task myTask;
    private final CalendarResult calendars;
    private boolean hasEvent = false;
    private final Spinner calendarSelector;

    public GCalControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
        ViewGroup parent = (ViewGroup) getView().getParent();
        parent.removeView(getView());
        ((LinearLayout) getDisplayView()).addView(getView()); //hack for spinner

        this.activity = activity;
        this.calendarSelector = (Spinner) getView().findViewById(R.id.calendars);

        calendars = Calendars.getCalendars();
        ArrayList<String> items = new ArrayList<String>();
        Collections.addAll(items, calendars.calendars);
        items.add(0, activity.getString(R.string.gcal_TEA_nocal));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, items.toArray(new String[items.size()]));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarSelector.setPromptId(title);
        calendarSelector.setAdapter(adapter);
        calendarSelector.setSelection(calendars.defaultIndex);
        calendarSelector.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                    int arg2, long arg3) {
                refreshDisplayView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                //nothing
            }
        });
    }

    @Override
    public void readFromTask(Task task) {
        this.myTask = task;
        String uri = GCalHelper.getTaskEventUri(task);
        if(!TextUtils.isEmpty(uri)) {
            try {
                calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = activity.getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                boolean deleted = cursor.getCount() == 0;
                cursor.close();
                if(deleted) {
                    calendarUri = null;
                    return;
                }

                hasEvent = true;
            } catch (Exception e) {
                exceptionService.reportError("unable-to-parse-calendar: " +  //$NON-NLS-1$
                        task.getValue(Task.CALENDAR_URI), e);
            }
        }
        refreshDisplayView();
    }

    @SuppressWarnings("nls")
    @Override
    public String writeToModel(Task task) {
        boolean gcalCreateEventEnabled = Preferences.getStringValue(R.string.gcal_p_default) != null &&
                                        !Preferences.getStringValue(R.string.gcal_p_default).equals("-1");
        if ((gcalCreateEventEnabled || calendarSelector.getSelectedItemPosition() != 0) &&
                calendarUri == null) {
            StatisticsService.reportEvent(StatisticsConstants.CREATE_CALENDAR_EVENT);

            try{
                ContentResolver cr = activity.getContentResolver();

                ContentValues values = new ContentValues();
                String calendarId = calendars.calendarIds[calendarSelector.getSelectedItemPosition() - 1];
                values.put("calendar_id", calendarId);

                calendarUri = GCalHelper.createTaskEvent(task, cr, values);
                task.setValue(Task.CALENDAR_URI, calendarUri.toString());

                if (calendarSelector.getSelectedItemPosition() != 0 && !hasEvent) {
                    // pop up the new event
                    Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                    intent.putExtra("beginTime", values.getAsLong("dtstart"));
                    intent.putExtra("endTime", values.getAsLong("dtend"));
                    activity.startActivity(intent);
                }

            } catch (Exception e) {
                exceptionService.displayAndReportError(activity,
                        activity.getString(R.string.gcal_TEA_error), e);
            }
        } else if(calendarUri != null) {
            try {
                ContentValues updateValues = new ContentValues();

                // check if we need to update the item
                ContentValues setValues = task.getSetValues();
                if(setValues.containsKey(Task.TITLE.name))
                    updateValues.put("title", task.getValue(Task.TITLE));
                if(setValues.containsKey(Task.NOTES.name))
                    updateValues.put("description", task.getValue(Task.NOTES));
                if(setValues.containsKey(Task.DUE_DATE.name) || setValues.containsKey(Task.ESTIMATED_SECONDS.name))
                    GCalHelper.createStartAndEndDate(task, updateValues);

                ContentResolver cr = activity.getContentResolver();
                if(cr.update(calendarUri, updateValues, null, null) > 0)
                    return activity.getString(R.string.gcal_TEA_calendar_updated);
            } catch (Exception e) {
                exceptionService.reportError("unable-to-update-calendar: " +  //$NON-NLS-1$
                        task.getValue(Task.CALENDAR_URI), e);
            }
        }

        return null;
    }

    private void viewCalendarEvent() {
        if(calendarUri == null)
            return;

        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = cr.query(calendarUri, new String[] { "dtstart", "dtend" },
                null, null, null);

        Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
        try {
            if(cursor == null || cursor.getCount() == 0) {
                // event no longer exists, recreate it
                calendarUri = null;
                writeToModel(myTask);
                return;
            }
            cursor.moveToFirst();
            intent.putExtra("beginTime", cursor.getLong(0));
            intent.putExtra("endTime", cursor.getLong(1));
        } catch (Exception e) {
            Log.e("gcal-error", "Error opening calendar", e); //$NON-NLS-1$ //$NON-NLS-2$
            Toast.makeText(activity, R.string.gcal_TEA_error, Toast.LENGTH_LONG);
        } finally {
            if(cursor != null)
                cursor.close();
        }

        activity.startActivity(intent);
    }

    @Override
    protected void refreshDisplayView() {
        TextView calendar = (TextView) getDisplayView().findViewById(R.id.calendar_display_which);
        if (calendarSelector.getSelectedItemPosition() != 0) {
            calendar.setText((String)calendarSelector.getSelectedItem());
        } else {
            calendar.setText(R.string.gcal_TEA_none_selected);
        }
    }

    @Override
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasEvent) {
                    calendarSelector.performClick();
                } else {
                    viewCalendarEvent();
                }
            }
        };
    }
}