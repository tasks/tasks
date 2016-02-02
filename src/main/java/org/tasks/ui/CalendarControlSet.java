package org.tasks.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.AndroidCalendar;
import com.todoroo.astrid.gcal.GCalHelper;

import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.injection.ForActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;

public class CalendarControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_gcal;

    private static final int REQUEST_CODE_CALENDAR = 70;
    private static final String EXTRA_URI = "extra_uri";
    private static final String EXTRA_ID = "extra_id";
    private static final String EXTRA_NAME = "extra_name";

    @Bind(R.id.clear) View cancelButton;
    @Bind(R.id.calendar_display_which) TextView calendar;

    @Inject GCalHelper gcalHelper;
    @Inject Preferences preferences;
    @Inject @ForActivity Context context;

    private String calendarId;
    private String calendarName;
    private String eventUri;
    private boolean isNewTask;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            eventUri = savedInstanceState.getString(EXTRA_URI);
            calendarName = savedInstanceState.getString(EXTRA_NAME);
            calendarId = savedInstanceState.getString(EXTRA_ID);
        } else if (isNewTask) {
            calendarId = preferences.getDefaultCalendar();
            if (!Strings.isNullOrEmpty(calendarId)) {
                AndroidCalendar defaultCalendar = gcalHelper.getCalendar(calendarId);
                if (defaultCalendar == null) {
                    calendarId = null;
                } else {
                    calendarName = defaultCalendar.getName();
                }
            }
        }
        if (!calendarEntryExists(eventUri)) {
            eventUri = null;
        }
        refreshDisplayView();
        return view;
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_gcal_display;
    }

    @Override
    protected int getIcon() {
        return R.drawable.ic_event_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        this.isNewTask = isNewTask;
        eventUri = task.getCalendarURI();
    }

    @Override
    public boolean hasChanges(Task original) {
        return !isNullOrEmpty(calendarId);
    }

    @Override
    public void apply(Task task) {
        if (!task.hasDueDate()) {
            return;
        }

        if (calendarEntryExists(task.getCalendarURI())) {
            ContentResolver cr = context.getContentResolver();
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
                    gcalHelper.createStartAndEndDate(task, updateValues);
                }

                cr.update(Uri.parse(task.getCalendarURI()), updateValues, null, null);
            } catch (Exception e) {
                Timber.e(e, "unable-to-update-calendar: %s", task.getCalendarURI());
            }
        } else if (!isNullOrEmpty(calendarId)) {
            ContentResolver cr = context.getContentResolver();
            try{
                ContentValues values = new ContentValues();
                values.put("calendar_id", calendarId);
                Uri uri = gcalHelper.createTaskEvent(task, cr, values);
                if(uri != null) {
                    task.setCalendarUri(uri.toString());
                    // pop up the new event
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra("beginTime", values.getAsLong("dtstart"));
                    intent.putExtra("endTime", values.getAsLong("dtend"));
                    startActivity(intent);
                }
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_URI, eventUri);
        outState.putString(EXTRA_NAME, calendarName);
        outState.putString(EXTRA_ID, calendarId);
    }

    @OnClick(R.id.clear)
    void clearCalendar(View view) {
        calendarName = null;
        calendarId = null;
        eventUri = null;
        refreshDisplayView();
    }

    @OnClick(R.id.calendar_display_which)
    void clickCalendar(View view) {
        if (Strings.isNullOrEmpty(eventUri)) {
            startActivityForResult(new Intent(context, CalendarSelectionActivity.class), REQUEST_CODE_CALENDAR);
        } else {
            ContentResolver cr = getActivity().getContentResolver();
            Uri uri = Uri.parse(eventUri);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            Cursor cursor = cr.query(uri, new String[] { "dtstart", "dtend" }, null, null, null);
            try {
                if(cursor.getCount() == 0) {
                    // event no longer exists
                    eventUri = null;
                    refreshDisplayView();
                    return;
                }
                cursor.moveToFirst();
                intent.putExtra("beginTime", cursor.getLong(0));
                intent.putExtra("endTime", cursor.getLong(1));
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
                Toast.makeText(getActivity(), R.string.gcal_TEA_error, Toast.LENGTH_LONG).show();
            } finally {
                cursor.close();
            }

            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CALENDAR) {
            if (resultCode == Activity.RESULT_OK) {
                calendarId = data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_ID);
                calendarName = data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME);
                refreshDisplayView();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void refreshDisplayView() {
        if (!Strings.isNullOrEmpty(eventUri)) {
            calendar.setAlpha(1.0f);
            calendar.setText(R.string.gcal_TEA_showCalendar_label);
            cancelButton.setVisibility(View.GONE);
        } else if (calendarName != null) {
            calendar.setAlpha(1.0f);
            calendar.setText(calendarName);
            cancelButton.setVisibility(View.VISIBLE);
        } else {
            calendar.setAlpha(0.5f);
            calendar.setText(R.string.gcal_TEA_addToCalendar_label);
            cancelButton.setVisibility(View.GONE);
        }
    }

    private boolean calendarEntryExists(String eventUri) {
        if (isNullOrEmpty(eventUri)) {
            return false;
        }

        try {
            Uri uri = Uri.parse(eventUri);
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(uri, new String[]{"dtstart"}, null, null, null);
            try {
                if (cursor.getCount() != 0) {
                    return true;
                }
            } finally {
                cursor.close();
            }
        } catch(Exception e) {
            Timber.e(e, "%s: %s", eventUri, e.getMessage());
        }

        return false;
    }
}
