package com.timsu.astrid.utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.timsu.astrid.R;

public class Calendars {

	// Private SDK
	private static final Uri CALENDAR_CONTENT_URI = Uri
			.parse("content://calendar/calendars"); // Calendars.CONTENT_URI

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

	private static final String CALENDARS_WHERE_ID = ACCES_LEVEL_COLUMN_NAME+
	    " >= 500 AND " + ID_COLUMN_NAME +"=?";

	private static final String CALENDARS_SORT = "displayName ASC";

	/**
	 * Ensures that the default calendar preference is pointing to
	 * user-modifiable calendar that exists. If the calendar does not exist
	 * anymore, the preference is reset to default value.
	 *
	 * @param context
	 *            Context
	 */
	public static void ensureValidDefaultCalendarPreference(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Resources r = context.getResources();
		Editor editor = prefs.edit();
		// We default the 'defaultCalendar' setting when it is undefined
		// or when the calendar does not exist anymore
//		if (!prefs.contains(r.getString(R.string.prefs_defaultCalendar))
//				|| !Calendars.isCalendarPresent(context, prefs.getString(r
//						.getString(R.string.prefs_defaultCalendar), null))) {
//			editor.putString(r.getString(R.string.prefs_defaultCalendar), r
//					.getString(R.string.prefs_defaultCalendar_default));
//			editor.commit();
//		}
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
	public static void initCalendarsPreference(Context context,
			ListPreference listPreference) {

		ContentResolver cr = context.getContentResolver();
		Resources r = context.getResources();
		Cursor c = cr.query(CALENDAR_CONTENT_URI, CALENDARS_PROJECTION,
				CALENDARS_WHERE, null, CALENDARS_SORT);

		// Fetch the current setting. Invalid calendar id will
		// be changed to default value.
		String currentSetting = String.valueOf(Preferences
				.getDefaultCalendarIDSafe(context));

		int currentSettingIndex = -1;

		if (c == null || c.getCount() == 0) {
			// Something went wrong when querying calendars
			// Let's keep the "Astrid default" only.
			listPreference
					.setEntries(new String[] { r
							.getString(R.string.prefs_defaultCalendar_astrid_default) });
//			listPreference.setEntryValues(new String[] { r
//					.getString(R.string.prefs_defaultCalendar_default) });
			listPreference.setValueIndex(0);
			listPreference.setEnabled(true);
			return;
		}

		int calendarCount = c.getCount();

		String[] entries = new String[calendarCount];
		String[] entryValues = new String[calendarCount];

		// Iterate calendars one by one, and fill up the list preference
		try {
			int row = 0;
			int idColumn = c.getColumnIndex(ID_COLUMN_NAME);
			int nameColumn = c.getColumnIndex(DISPLAY_COLUMN_NAME);
			while (c.moveToNext()) {
				String id = c.getString(idColumn);
				String name = c.getString(nameColumn);
				entries[row] = name;
				entryValues[row] = id;

				// We found currently selected calendar
				if (currentSetting.equals(id)) {
					currentSettingIndex = row;
				}

				row++;
			}

			if (currentSettingIndex == -1) {
				// Should not happen!
				// Let's keep the "Astrid default" only.
				Log.d("astrid", "initCalendarsPreference: Unknown calendar.");
				listPreference
						.setEntries(new String[] { r
								.getString(R.string.prefs_defaultCalendar_astrid_default) });
//				listPreference.setEntryValues(new String[] { r
//						.getString(R.string.prefs_defaultCalendar_default) });
				listPreference.setValueIndex(0);
				listPreference.setEnabled(true);
			} else if(currentSettingIndex >= entryValues.length) {
			    currentSettingIndex = 0;
			}

			listPreference.setEntries(entries);
			listPreference.setEntryValues(entryValues);

			listPreference.setValueIndex(currentSettingIndex);
			listPreference.setEnabled(true);

		} finally {
			c.deactivate();
		}
	}

	/**
	 * Checks whether user-modifiable calendar is present with a given id.
	 *
	 * @param context
	 *            Context
	 * @param id
	 *            Calendar ID to search for
	 * @return true, if user-modifiable calendar with the given id exists; false
	 *         otherwise.
	 */
	private static boolean isCalendarPresent(Context context, String id) {
		if (id == null)
			return false;

		ContentResolver cr = context.getContentResolver();
		Cursor c = null;

		try {
			c = cr.query(CALENDAR_CONTENT_URI, CALENDARS_PROJECTION,
					CALENDARS_WHERE_ID, new String[] { id }, CALENDARS_SORT);
		} finally {
			if (c != null) {
				c.deactivate();
			}
		}

		return (c != null) && (c.getCount() != 0);
	}

}