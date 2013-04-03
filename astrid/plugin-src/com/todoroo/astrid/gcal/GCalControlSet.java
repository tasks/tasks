/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
import com.todoroo.astrid.service.ThemeService;
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

    private Uri calendarUri = null;

    private final CalendarResult calendars;
    private boolean hasEvent = false;
    private Spinner calendarSelector;
    private final int title;
    private final ImageView image;

    public GCalControlSet(final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
        this.title = title;
        calendars = Calendars.getCalendars();
        getView(); // Hack to force initialized
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void afterInflate() {
        ((LinearLayout) getDisplayView()).addView(getView()); //hack for spinner

        this.calendarSelector = (Spinner) getView().findViewById(R.id.calendars);
        ArrayList<String> items = new ArrayList<String>();
        Collections.addAll(items, calendars.calendars);
        items.add(0, activity.getString(R.string.gcal_TEA_nocal));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, items.toArray(new String[items.size()]));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        calendarSelector.setPromptId(title);
        calendarSelector.setAdapter(adapter);
        resetCalendarSelector();
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
    protected void readFromTaskOnInitialize() {
        String uri = GCalHelper.getTaskEventUri(model);
        if(!TextUtils.isEmpty(uri)) {
            try {
                calendarUri = Uri.parse(uri);

                // try to load calendar
                ContentResolver cr = activity.getContentResolver();
                Cursor cursor = cr.query(calendarUri, new String[] { "dtstart" }, null, null, null); //$NON-NLS-1$
                try {
                    boolean deleted = cursor.getCount() == 0;

                    if(deleted) {
                        calendarUri = null;
                        return;
                    }
                } finally {
                    cursor.close();
                }

                hasEvent = true;
            } catch (Exception e) {
                exceptionService.reportError("unable-to-parse-calendar: " +  //$NON-NLS-1$
                        model.getValue(Task.CALENDAR_URI), e);
            }
        } else {
            hasEvent = false;
            calendarUri = null;
        }
        refreshDisplayView();
    }

    public void resetCalendarSelector() {
        if (calendarSelector != null)
            calendarSelector.setSelection(calendars.defaultIndex + 1); // plus 1 for the no selection item
    }

    @SuppressWarnings("nls")
    @Override
    protected String writeToModelAfterInitialized(Task task) {
        if (!task.hasDueDate())
            return null;

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
                if(calendarUri != null) {
                    task.setValue(Task.CALENDAR_URI, calendarUri.toString());

                    if (calendarSelector.getSelectedItemPosition() != 0 && !hasEvent) {
                        // pop up the new event
                        Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                        intent.putExtra("beginTime", values.getAsLong("dtstart"));
                        intent.putExtra("endTime", values.getAsLong("dtend"));
                        activity.startActivity(intent);
                    }
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

    @SuppressWarnings("nls")
    private void viewCalendarEvent() {
        if(calendarUri == null)
            return;

        ContentResolver cr = activity.getContentResolver();
        Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
        Cursor cursor = cr.query(calendarUri, new String[] { "dtstart", "dtend" },
                null, null, null);
        try {
            if(cursor.getCount() == 0) {
                // event no longer exists, recreate it
                calendarUri = null;
                writeToModel(model);
                return;
            }
            cursor.moveToFirst();
            intent.putExtra("beginTime", cursor.getLong(0));
            intent.putExtra("endTime", cursor.getLong(1));

        } catch (Exception e) {
            Log.e("gcal-error", "Error opening calendar", e); //$NON-NLS-1$ //$NON-NLS-2$
            Toast.makeText(activity, R.string.gcal_TEA_error, Toast.LENGTH_LONG).show();
        } finally {
            cursor.close();
        }

        activity.startActivity(intent);
    }

    @Override
    protected void refreshDisplayView() {
        TextView calendar = (TextView) getDisplayView().findViewById(R.id.calendar_display_which);
        calendar.setTextColor(themeColor);
        image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_addcal, R.drawable.tea_icn_addcal_lightblue));
        if (initialized) {
            if (hasEvent) {
                calendar.setText(R.string.gcal_TEA_has_event);
            } else if (calendarSelector.getSelectedItemPosition() != 0) {
                calendar.setText((String)calendarSelector.getSelectedItem());
            } else {
                calendar.setTextColor(unsetColor);
                image.setImageResource(R.drawable.tea_icn_addcal_gray);
                calendar.setText(R.string.gcal_TEA_none_selected);
            }
        } else {
            int index = calendars.defaultIndex;
            if (!TextUtils.isEmpty(model.getValue(Task.CALENDAR_URI))) {
                calendar.setText(R.string.gcal_TEA_has_event);
            } else if (index >= 0 && index < calendars.calendars.length) {
                calendar.setText(calendars.calendars[index]);
            } else {
                calendar.setTextColor(unsetColor);
                image.setImageResource(R.drawable.tea_icn_addcal_gray);
                calendar.setText(R.string.gcal_TEA_none_selected);
            }
        }
    }

    @Override
    protected OnClickListener getDisplayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (calendarSelector == null)
                    getView(); // Force load
                if (!hasEvent) {
                    calendarSelector.performClick();
                } else {
                    viewCalendarEvent();
                }
            }
        };
    }
}
