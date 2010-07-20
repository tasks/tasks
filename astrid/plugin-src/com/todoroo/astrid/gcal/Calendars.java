package com.todoroo.astrid.gcal;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.utility.Preferences;

@SuppressWarnings("nls")
public class Calendars {

    public static final String CALENDAR_CONTENT_CALENDARS = "calendars";
    public static final String CALENDAR_CONTENT_EVENTS = "events";

	private static final String ID_COLUMN_NAME = "_id";
	private static final String DISPLAY_COLUMN_NAME = "displayName";
	private static final String ACCES_LEVEL_COLUMN_NAME = "access_level";

	private static final String[] CALENDARS_PROJECTION = new String[] {
			ID_COLUMN_NAME, // Calendars._ID,
			DISPLAY_COLUMN_NAME // Calendars.DISPLAY_NAME
	};

	// Only show calendars that the user can modify. Access level 500
	// corresponds to Calendars.CONTRIBUTOR_ACCESS
	private static final String CALENDARS_WHERE = ACCES_LEVEL_COLUMN_NAME + ">= 500";

	private static final String CALENDARS_SORT = "displayName ASC";

	// --- api access

	/** Return content uri for calendars
	 * @param table provider table, something like calendars, events
	 */
	public static Uri getCalendarContentUri(String table) {
	    if(android.os.Build.VERSION.SDK_INT >= 8)
	        return Uri.parse("content://com.android.calendar/" + table);
	    else
	        return Uri.parse("content://calendar/" + table);
	}

	/** Return calendar package name */
	public static String getCalendarPackage() {
	    if(android.os.Build.VERSION.SDK_INT >= 8)
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
	 * Appends all user-modifiable calendars to listPreference. Always includes
	 * entry called "Astrid default" with calendar id of
	 * prefs_defaultCalendar_default.
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
			int nameColumn = c.getColumnIndex(DISPLAY_COLUMN_NAME);
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

			if (result.defaultIndex == -1 || result.defaultIndex >= calendarCount) {
			    result.defaultIndex = 0;
			}

			return result;
		} finally {
		    if(c != null)
		        c.close();
		}
	}

	/**
	 * sets the default calendar for future use
	 * @param defaultCalendar default calendar id
	 */
	public static void setDefaultCalendar(String defaultCalendar) {
        Preferences.setString(R.string.gcal_p_default, defaultCalendar);
	}

}