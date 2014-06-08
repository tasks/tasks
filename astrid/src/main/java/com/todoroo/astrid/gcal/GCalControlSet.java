/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

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

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.PopupControlSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.util.ArrayList;
import java.util.Collections;

import static org.tasks.preferences.ResourceResolver.getResource;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GCalControlSet extends PopupControlSet {

    private static final Logger log = LoggerFactory.getLogger(GCalControlSet.class);

    // --- instance variables

    private final GCalHelper gcal;

    private Uri calendarUri = null;

    private final GCalHelper.CalendarResult calendars;
    private boolean hasEvent = false;
    private Spinner calendarSelector;
    private final int title;
    private final ImageView image;

    public GCalControlSet(ActivityPreferences preferences, GCalHelper gcal, final Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(preferences, activity, viewLayout, displayViewLayout, title);
        this.gcal = gcal;
        this.title = title;
        this.calendars = gcal.getCalendars();
        getView(); // Hack to force initialized
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    @Override
    protected void afterInflate() {
        ((LinearLayout) getDisplayView()).addView(getView()); //hack for spinner

        this.calendarSelector = (Spinner) getView().findViewById(R.id.calendars);
        ArrayList<String> items = new ArrayList<>();
        Collections.addAll(items, calendars.calendars);
        items.add(0, activity.getString(R.string.gcal_TEA_nocal));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
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
        String uri = gcal.getTaskEventUri(model);
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
                log.error("unable-to-parse-calendar: {}", model.getCalendarURI(), e);
            }
        } else {
            hasEvent = false;
            calendarUri = null;
        }
        refreshDisplayView();
    }

    public void resetCalendarSelector() {
        if (calendarSelector != null) {
            calendarSelector.setSelection(calendars.defaultIndex + 1); // plus 1 for the no selection item
        }
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if (!task.hasDueDate()) {
            return;
        }

        boolean gcalCreateEventEnabled = gcal.getDefaultCalendar() != null &&
                                        !gcal.getDefaultCalendar().equals("-1");
        if ((gcalCreateEventEnabled || calendarSelector.getSelectedItemPosition() != 0) &&
                calendarUri == null) {

            try{
                ContentResolver cr = activity.getContentResolver();

                ContentValues values = new ContentValues();
                String calendarId = calendars.calendarIds[calendarSelector.getSelectedItemPosition() - 1];
                values.put("calendar_id", calendarId);

                calendarUri = gcal.createTaskEvent(task, cr, values);
                if(calendarUri != null) {
                    task.setCalendarUri(calendarUri.toString());

                    if (calendarSelector.getSelectedItemPosition() != 0 && !hasEvent) {
                        // pop up the new event
                        Intent intent = new Intent(Intent.ACTION_EDIT, calendarUri);
                        intent.putExtra("beginTime", values.getAsLong("dtstart"));
                        intent.putExtra("endTime", values.getAsLong("dtend"));
                        activity.startActivity(intent);
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if(calendarUri != null) {
            try {
                ContentValues updateValues = new ContentValues();

                // check if we need to update the item
                ContentValues setValues = task.getSetValues();
                if(setValues.containsKey(Task.TITLE.name)) {
                    updateValues.put("title", task.getTitle());
                }
                if(setValues.containsKey(Task.NOTES.name)) {
                    updateValues.put("description", task.getNotes());
                }
                if(setValues.containsKey(Task.DUE_DATE.name) || setValues.containsKey(Task.ESTIMATED_SECONDS.name)) {
                    gcal.createStartAndEndDate(task, updateValues);
                }

                ContentResolver cr = activity.getContentResolver();
                cr.update(calendarUri, updateValues, null, null);
            } catch (Exception e) {
                log.error("unable-to-update-calendar: {}", task.getCalendarURI(), e);
            }
        }
    }

    private void viewCalendarEvent() {
        if(calendarUri == null) {
            return;
        }

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
        image.setImageResource(getResource(activity, R.attr.tea_icn_addcal));
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
            if (!TextUtils.isEmpty(model.getCalendarURI())) {
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
                if (calendarSelector == null) {
                    getView(); // Force load
                }
                if (!hasEvent) {
                    calendarSelector.performClick();
                } else {
                    viewCalendarEvent();
                }
            }
        };
    }
}
