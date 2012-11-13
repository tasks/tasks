/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.ListPreference;
import android.provider.CalendarContract;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;

@SuppressWarnings("nls")
public class Calendars {

    public static final String CALENDAR_CONTENT_CALENDARS = "calendars";
    public static final String CALENDAR_CONTENT_EVENTS = "events";
    public static final String CALENDAR_CONTENT_ATTENDEES = "attendees";

	private static final boolean USE_ICS_NAMES = AndroidUtilities.getSdkVersion() >= 14;

	public static final String ID_COLUMN_NAME = "_id";
	public static final String CALENDARS_DISPLAY_COL = (USE_ICS_NAMES ? CalendarContract.Calendars.CALENDAR_DISPLAY_NAME : "displayName");
	public static final String CALENDARS_ACCESS_LEVEL_COL = (USE_ICS_NAMES ? CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL : "access_level");
	public static final String EVENTS_DTSTART_COL = (USE_ICS_NAMES ? CalendarContract.Events.DTSTART : "dtstart");
	public static final String EVENTS_DTEND_COL = (USE_ICS_NAMES ? CalendarContract.Events.DTEND : "dtend");
	public static final String EVENTS_NAME_COL = (USE_ICS_NAMES ? CalendarContract.Events.TITLE : "title");
	public static final String ATTENDEES_EVENT_ID_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.EVENT_ID : "event_id");
    public static final String ATTENDEES_NAME_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_NAME : "attendeeName");
    public static final String ATTENDEES_EMAIL_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_EMAIL: "attendeeEmail");


	private static final String[] CALENDARS_PROJECTION = new String[] {
			ID_COLUMN_NAME,
			CALENDARS_DISPLAY_COL,
	};

	// Only show calendars that the user can modify. Access level 500
	// corresponds to Calendars.CONTRIBUTOR_ACCESS
	private static final String CALENDARS_WHERE = CALENDARS_ACCESS_LEVEL_COL + ">= 500";

	private static final String CALENDARS_SORT = CALENDARS_DISPLAY_COL + " ASC";

	// --- api access

	/** Return content uri for calendars
	 * @param table provider table, something like calendars, events
	 */
	public static Uri getCalendarContentUri(String table) {
	    if (AndroidUtilities.getSdkVersion() >= 14) {
	        return getIcsUri(table);
	    }

	    if(AndroidUtilities.getSdkVersion() >= 8)
	        return Uri.parse("content://com.android.calendar/" + table);
	    else
	        return Uri.parse("content://calendar/" + table);
	}

	private static Uri getIcsUri(String table) {
	    if (CALENDAR_CONTENT_CALENDARS.equals(table))
	        return CalendarContract.Calendars.CONTENT_URI;
	    else if (CALENDAR_CONTENT_EVENTS.equals(table))
	        return CalendarContract.Events.CONTENT_URI;
	    else if (CALENDAR_CONTENT_ATTENDEES.equals(table))
	        return CalendarContract.Attendees.CONTENT_URI;
	    return null;
	}

	/** Return calendar package name */
	public static String getCalendarPackage() {
	    if(AndroidUtilities.getSdkVersion() >= 8)
	        return "com.google.android.calendar";
	    else
	        return "com.android.calendar";
	}

	// --- helper data structure

	/**
	 * Helper class for working with the results of getCalendars
	 */
	public static class CalendarResult {
	    /** calendar names */
	    public String[] calendars;

	    /** calendar ids. null entry -> use default */
	    public String[] calendarIds;

	    /** default selection index */
	    public int defaultIndex = -1;
	}

	/**
	 * Appends all user-modifiable calendars to listPreference.
	 *
	 * @param context
	 *            context
	 * @param listPreference
	 *            preference to init
	 */
	public static CalendarResult getCalendars() {
	    Context context = ContextManager.getContext();
		ContentResolver cr = context.getContentResolver();
		Resources r = context.getResources();

		Cursor c = cr.query(getCalendarContentUri(CALENDAR_CONTENT_CALENDARS), CALENDARS_PROJECTION,
				CALENDARS_WHERE, null, CALENDARS_SORT);
		try {
    		// Fetch the current setting. Invalid calendar id will
    		// be changed to default value.
    		String defaultSetting = Preferences.getStringValue(R.string.gcal_p_default);

    		CalendarResult result = new CalendarResult();

    		if (c == null || c.getCount() == 0) {
    			// Something went wrong when querying calendars. Only offer them
    		    // the system default choice
    		    result.calendars = new String[] {
    			        r.getString(R.string.gcal_GCP_default) };
    			result.calendarIds = new String[] { null };
    			result.defaultIndex = 0;
    			return result;
    		}

    		int calendarCount = c.getCount();

    		result.calendars = new String[calendarCount];
    		result.calendarIds = new String[calendarCount];

    		// Iterate calendars one by one, and fill up the list preference
			int row = 0;
			int idColumn = c.getColumnIndex(ID_COLUMN_NAME);
			int nameColumn = c.getColumnIndex(CALENDARS_DISPLAY_COL);
			while (c.moveToNext()) {
				String id = c.getString(idColumn);
				String name = c.getString(nameColumn);
				result.calendars[row] = name;
				result.calendarIds[row] = id;

				// We found currently selected calendar
				if (defaultSetting != null && defaultSetting.equals(id)) {
					result.defaultIndex = row;
				}

				row++;
			}

			if (result.defaultIndex >= calendarCount) {
			    result.defaultIndex = 0;
			}

			return result;
		} finally {
		    if(c != null)
		        c.close();
		}
	}

    /**
     * Appends all user-modifiable calendars to listPreference.
     *
     * @param context
     *            context
     * @param listPreference
     *            preference to init
     */
    public static void initCalendarsPreference(Context context,
            ListPreference listPreference) {

        Resources r = context.getResources();
        CalendarResult calendars = getCalendars();

        // Fetch the current setting. Invalid calendar id will
        // be changed to default value.
        String currentSetting = Preferences.getStringValue(R.string.gcal_p_default);

        int currentSettingIndex = -1;

        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        entries.addAll(Arrays.asList(r.getStringArray(R.array.EPr_default_addtocalendar)));
        entries.addAll(Arrays.asList(calendars.calendars));

        ArrayList<CharSequence> entryValues = new ArrayList<CharSequence>();
        entryValues.addAll(Arrays.asList(r.getStringArray(R.array.EPr_default_addtocalendar_values)));
        entryValues.addAll(Arrays.asList(calendars.calendarIds));

        listPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
        listPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));

        listPreference.setValueIndex(0);
        listPreference.setEnabled(true);

        if (calendars.calendarIds.length == 0 || calendars.calendars.length == 0) {
            // Something went wrong when querying calendars
            // Leave the preference at disabled.
            return;
        }

        // Iterate calendars one by one, and fill up the list preference
        if (currentSetting != null) {
            for (int i=0; i<calendars.calendarIds.length; i++) {
                // We found currently selected calendar
                if (currentSetting.equals(calendars.calendarIds[i])) {
                    currentSettingIndex = i+1; // +1 correction for disabled-entry
                    break;
                }
            }
        }

        if(currentSettingIndex == -1 || currentSettingIndex > calendars.calendarIds.length+1) {
            // Should not happen!
            // Leave the preference at disabled.
            Log.d("astrid", "initCalendarsPreference: Unknown calendar.");
            currentSettingIndex = 0;
        }

        listPreference.setValueIndex(currentSettingIndex);
        listPreference.setEnabled(true);
    }

	/**
	 * sets the default calendar for future use
	 * @param defaultCalendar default calendar id
	 */
	public static void setDefaultCalendar(String defaultCalendar) {
        Preferences.setString(R.string.gcal_p_default, defaultCalendar);
	}

	/**
	 * gets the default calendar for future use
	 * @return the calendar id for use with the contentresolver
	 */
	public static String getDefaultCalendar() {
	    return Preferences.getStringValue(R.string.gcal_p_default);
	}

}
