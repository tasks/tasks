/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;
import org.tasks.activities.CalendarSelectionDialog;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

import timber.log.Timber;

/**
 * Control Set for managing repeats
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GCalControlSet extends TaskEditControlSetBase implements CalendarSelectionDialog.CalendarSelectionHandler {

    private static final String FRAG_TAG_CALENDAR_SELECTION = "frag_tag_calendar_selection";

    // --- instance variables

    private final GCalHelper gcal;
    private Preferences preferences;
    private final TaskEditFragment taskEditFragment;
    private PermissionRequestor permissionRequestor;

    private Uri calendarUri = null;

    private boolean hasEvent = false;
    private TextView calendar;
    private ImageView cancelButton;
    private String calendarId;
    private String calendarName;

    public GCalControlSet(GCalHelper gcal, Preferences preferences,
                          TaskEditFragment taskEditFragment, PermissionRequestor permissionRequestor) {
        super(taskEditFragment.getActivity(), R.layout.control_set_gcal_display);
        this.gcal = gcal;
        this.preferences = preferences;
        this.taskEditFragment = taskEditFragment;
        this.permissionRequestor = permissionRequestor;
    }

    @Override
    protected void afterInflate() {
        View view = getView();
        calendar = (TextView) view.findViewById(R.id.calendar_display_which);
        cancelButton = (ImageView) view.findViewById(R.id.clear_calendar);
        calendar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasEvent) {
                    viewCalendarEvent();
                } else {
                    // TODO: show calendar selection if permission has just been granted
                    // can't do this now because the app saves state when TEA is paused,
                    // which triggers calendar creation if there is a default add to calendar.
                    if (permissionRequestor.requestCalendarPermissions()) {
                        showCalendarSelectionDialog();
                    }
                }
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearEvent();
                refreshDisplayView();
            }
        });
    }

    public void showCalendarSelectionDialog() {
        FragmentManager fragmentManager = taskEditFragment.getFragmentManager();
        CalendarSelectionDialog fragmentByTag = (CalendarSelectionDialog) fragmentManager.findFragmentByTag(FRAG_TAG_CALENDAR_SELECTION);
        if (fragmentByTag == null) {
            fragmentByTag = new CalendarSelectionDialog();
            fragmentByTag.show(fragmentManager, FRAG_TAG_CALENDAR_SELECTION);
        }
        fragmentByTag.setCalendarSelectionHandler(GCalControlSet.this);
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
                        clearEvent();
                        hasEvent = false;
                    } else {
                        hasEvent = true;
                    }
                } finally {
                    cursor.close();
                }
            } catch (Exception e) {
                Timber.e(e, "unable-to-parse-calendar: %s", model.getCalendarURI());
            }
        } else {
            hasEvent = false;
            clearEvent();
        }
        refreshDisplayView();
    }

    private void clearEvent() {
        calendarId = null;
        calendarUri = null;
        calendarName = null;
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        if (!task.hasDueDate()) {
            return;
        }

        if ((preferences.isDefaultCalendarSet() || calendarId != null) && calendarUri == null) {
            try{
                ContentResolver cr = activity.getContentResolver();

                ContentValues values = new ContentValues();
                values.put("calendar_id", calendarId);
                calendarUri = gcal.createTaskEvent(task, cr, values);
                if(calendarUri != null) {
                    task.setCalendarUri(calendarUri.toString());

                    if (!hasEvent) {
                        // pop up the new event
                        Intent intent = new Intent(Intent.ACTION_VIEW, calendarUri);
                        intent.putExtra("beginTime", values.getAsLong("dtstart"));
                        intent.putExtra("endTime", values.getAsLong("dtend"));
                        activity.startActivity(intent);
                    }
                }

            } catch (Exception e) {
                Timber.e(e, e.getMessage());
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
                Timber.e(e, "unable-to-update-calendar: %s", task.getCalendarURI());
            }
        }
    }

    private void viewCalendarEvent() {
        if(calendarUri == null) {
            return;
        }

        ContentResolver cr = activity.getContentResolver();
        Intent intent = new Intent(Intent.ACTION_VIEW, calendarUri);
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
            Timber.e(e, e.getMessage());
            Toast.makeText(activity, R.string.gcal_TEA_error, Toast.LENGTH_LONG).show();
        } finally {
            cursor.close();
        }

        activity.startActivity(intent);
    }

    private void refreshDisplayView() {
        calendar.setTextColor(themeColor);
        if (initialized) {
            if (hasEvent) {
                calendar.setText(R.string.gcal_TEA_showCalendar_label);
                cancelButton.setVisibility(View.GONE);
            } else if (calendarName != null) {
                calendar.setText(calendarName);
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                calendar.setTextColor(unsetColor);
                calendar.setText(R.string.gcal_TEA_addToCalendar_label);
                cancelButton.setVisibility(View.GONE);
            }
        } else {
            cancelButton.setVisibility(View.GONE);
            if (TextUtils.isEmpty(model.getCalendarURI())) {
                calendar.setTextColor(unsetColor);
                calendar.setText(R.string.gcal_TEA_addToCalendar_label);
            } else {
                calendar.setText(R.string.gcal_TEA_showCalendar_label);
            }
        }
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_event_24dp;
    }

    @Override
    public void selectedCalendar(AndroidCalendar androidCalendar) {
        this.calendarId = androidCalendar.getId();
        this.calendarName = androidCalendar.getName();
        refreshDisplayView();
    }

    @Override
    public void dismiss() {

    }
}
