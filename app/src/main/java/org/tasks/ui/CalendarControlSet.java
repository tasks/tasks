package org.tasks.ui;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.analytics.Tracker;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarEventProvider;
import org.tasks.calendars.CalendarProvider;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeBase;
import timber.log.Timber;

public class CalendarControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_gcal;

  private static final int REQUEST_CODE_PICK_CALENDAR = 70;
  private static final int REQUEST_CODE_OPEN_EVENT = 71;
  private static final int REQUEST_CODE_CLEAR_EVENT = 72;

  private static final String EXTRA_URI = "extra_uri";
  private static final String EXTRA_ID = "extra_id";

  @BindView(R.id.clear)
  View cancelButton;

  @BindView(R.id.calendar_display_which)
  TextView calendar;

  @Inject GCalHelper gcalHelper;
  @Inject CalendarProvider calendarProvider;
  @Inject Preferences preferences;
  @Inject @ForActivity Context context;
  @Inject PermissionChecker permissionChecker;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject Tracker tracker;
  @Inject DialogBuilder dialogBuilder;
  @Inject ThemeBase themeBase;
  @Inject CalendarEventProvider calendarEventProvider;

  private String calendarId;
  private String eventUri;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    boolean canAccessCalendars = permissionChecker.canAccessCalendars();
    if (savedInstanceState != null) {
      eventUri = savedInstanceState.getString(EXTRA_URI);
      calendarId = savedInstanceState.getString(EXTRA_ID);
    } else if (task.isNew() && canAccessCalendars) {
      calendarId = preferences.getDefaultCalendar();
      if (!Strings.isNullOrEmpty(calendarId)) {
        try {
          AndroidCalendar defaultCalendar = calendarProvider.getCalendar(calendarId);
          if (defaultCalendar == null) {
            calendarId = null;
          }
        } catch (Exception e) {
          Timber.e(e);
          tracker.reportException(e);
          calendarId = null;
        }
      }
    } else {
      eventUri = task.getCalendarURI();
    }

    if (canAccessCalendars && !calendarEntryExists(eventUri)) {
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

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean hasChanges(Task original) {
    if (!permissionChecker.canAccessCalendars()) {
      return false;
    }
    if (!isNullOrEmpty(calendarId)) {
      return true;
    }
    String originalUri = original.getCalendarURI();
    if (isNullOrEmpty(eventUri) && isNullOrEmpty(originalUri)) {
      return false;
    }
    return !originalUri.equals(eventUri);
  }

  @Override
  public void apply(Task task) {
    if (!permissionChecker.canAccessCalendars()) {
      return;
    }

    if (!isNullOrEmpty(task.getCalendarURI())) {
      if (eventUri == null) {
        calendarEventProvider.deleteEvent(task);
      } else if (!calendarEntryExists(task.getCalendarURI())) {
        task.setCalendarUri("");
      }
    }

    if (!task.hasDueDate()) {
      return;
    }

    if (calendarEntryExists(task.getCalendarURI())) {
      ContentResolver cr = context.getContentResolver();
      try {
        ContentValues updateValues = new ContentValues();

        // check if we need to update the item
        updateValues.put(CalendarContract.Events.TITLE, task.getTitle());
        updateValues.put(CalendarContract.Events.DESCRIPTION, task.getNotes());
        gcalHelper.createStartAndEndDate(task, updateValues);

        cr.update(Uri.parse(task.getCalendarURI()), updateValues, null, null);
      } catch (Exception e) {
        Timber.e(e, "unable-to-update-calendar: %s", task.getCalendarURI());
      }
    } else if (!isNullOrEmpty(calendarId)) {
      try {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        Uri uri = gcalHelper.createTaskEvent(task, values);
        if (uri != null) {
          task.setCalendarUri(uri.toString());
          // pop up the new event
          Intent intent = new Intent(Intent.ACTION_VIEW, uri);
          intent.putExtra(
              CalendarContract.EXTRA_EVENT_BEGIN_TIME,
              values.getAsLong(CalendarContract.Events.DTSTART));
          intent.putExtra(
              CalendarContract.EXTRA_EVENT_END_TIME,
              values.getAsLong(CalendarContract.Events.DTEND));
          startActivity(intent);
        }
      } catch (Exception e) {
        Timber.e(e);
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(EXTRA_URI, eventUri);
    outState.putString(EXTRA_ID, calendarId);
  }

  @OnClick(R.id.clear)
  void clearCalendar(View view) {
    if (isNullOrEmpty(eventUri)) {
      clear();
    } else {
      dialogBuilder
          .newMessageDialog(R.string.delete_calendar_event_confirmation)
          .setPositiveButton(
              R.string.delete,
              (dialog, which) -> {
                if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_CLEAR_EVENT)) {
                  clear();
                }
              })
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  private void clear() {
    calendarId = null;
    eventUri = null;
    refreshDisplayView();
  }

  @OnClick(R.id.calendar_display_which)
  void clickCalendar(View view) {
    if (Strings.isNullOrEmpty(eventUri)) {
      Intent intent = new Intent(context, CalendarSelectionActivity.class);
      intent.putExtra(CalendarSelectionActivity.EXTRA_CALENDAR_NAME, getCalendarName());
      startActivityForResult(intent, REQUEST_CODE_PICK_CALENDAR);
    } else {
      if (permissionRequestor.requestCalendarPermissions(REQUEST_CODE_OPEN_EVENT)) {
        openCalendarEvent();
      }
    }
  }

  private void openCalendarEvent() {
    ContentResolver cr = getActivity().getContentResolver();
    Uri uri = Uri.parse(eventUri);
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    Cursor cursor =
        cr.query(
            uri,
            new String[] {CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND},
            null,
            null,
            null);
    try {
      if (cursor.getCount() == 0) {
        // event no longer exists
        Toast.makeText(context, R.string.calendar_event_not_found, Toast.LENGTH_SHORT).show();
        eventUri = null;
        refreshDisplayView();
      } else {
        cursor.moveToFirst();
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cursor.getLong(0));
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, cursor.getLong(1));
        startActivity(intent);
      }
    } catch (Exception e) {
      Timber.e(e);
      Toast.makeText(getActivity(), R.string.gcal_TEA_error, Toast.LENGTH_LONG).show();
    } finally {
      cursor.close();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_PICK_CALENDAR) {
      if (resultCode == Activity.RESULT_OK) {
        calendarId = data.getStringExtra(CalendarSelectionActivity.EXTRA_CALENDAR_ID);
        refreshDisplayView();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private String getCalendarName() {
    if (calendarId == null) {
      return null;
    }
    AndroidCalendar calendar = calendarProvider.getCalendar(calendarId);
    return calendar == null ? null : calendar.getName();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CODE_OPEN_EVENT) {
      if (verifyPermissions(grantResults)) {
        openCalendarEvent();
      }
    } else if (requestCode == REQUEST_CODE_CLEAR_EVENT) {
      if (verifyPermissions(grantResults)) {
        clear();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void refreshDisplayView() {
    if (!Strings.isNullOrEmpty(eventUri)) {
      calendar.setText(R.string.gcal_TEA_showCalendar_label);
      cancelButton.setVisibility(View.VISIBLE);
    } else if (calendarId != null) {
      calendar.setText(getCalendarName());
      cancelButton.setVisibility(View.GONE);
    } else {
      calendar.setText(null);
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
      Cursor cursor =
          contentResolver.query(
              uri, new String[] {CalendarContract.Events.DTSTART}, null, null, null);
      try {
        if (cursor.getCount() != 0) {
          return true;
        }
      } finally {
        cursor.close();
      }
    } catch (Exception e) {
      Timber.e(e, "%s: %s", eventUri, e.getMessage());
    }

    return false;
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }
}
