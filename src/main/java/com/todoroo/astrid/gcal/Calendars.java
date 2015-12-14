/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.net.Uri;
import android.provider.CalendarContract;

import com.todoroo.andlib.utility.AndroidUtilities;

import javax.inject.Singleton;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastIceCreamSandwich;

@Singleton
public class Calendars {

    public static final String CALENDAR_CONTENT_CALENDARS = "calendars";
    public static final String CALENDAR_CONTENT_EVENTS = "events";
    public static final String CALENDAR_CONTENT_ATTENDEES = "attendees";

	private static final boolean USE_ICS_NAMES = AndroidUtilities.atLeastIceCreamSandwich();

	public static final String ID_COLUMN_NAME = "_id";
	public static final String CALENDARS_DISPLAY_COL = (USE_ICS_NAMES ? CalendarContract.Calendars.CALENDAR_DISPLAY_NAME : "displayName");
	public static final String CALENDARS_ACCESS_LEVEL_COL = (USE_ICS_NAMES ? CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL : "access_level");
	public static final String EVENTS_DTSTART_COL = (USE_ICS_NAMES ? CalendarContract.Events.DTSTART : "dtstart");
	public static final String EVENTS_DTEND_COL = (USE_ICS_NAMES ? CalendarContract.Events.DTEND : "dtend");
	public static final String EVENTS_NAME_COL = (USE_ICS_NAMES ? CalendarContract.Events.TITLE : "title");
	public static final String ATTENDEES_EVENT_ID_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.EVENT_ID : "event_id");
    public static final String ATTENDEES_NAME_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_NAME : "attendeeName");
    public static final String ATTENDEES_EMAIL_COL = (USE_ICS_NAMES ? CalendarContract.Attendees.ATTENDEE_EMAIL: "attendeeEmail");

	public static final String[] CALENDARS_PROJECTION = new String[] {
			ID_COLUMN_NAME,
			CALENDARS_DISPLAY_COL,
	};

	// Only show calendars that the user can modify. Access level 500
	// corresponds to Calendars.CONTRIBUTOR_ACCESS
	public static final String CALENDARS_WHERE = CALENDARS_ACCESS_LEVEL_COL + ">= 500";

	public static final String CALENDARS_SORT = CALENDARS_DISPLAY_COL + " ASC";

    // --- api access

	/** Return content uri for calendars
	 * @param table provider table, something like calendars, events
	 */
	public static Uri getCalendarContentUri(String table) {
	    if (atLeastIceCreamSandwich()) {
	        return getIcsUri(table);
	    }

		return Uri.parse("content://com.android.calendar/" + table);
	}

	private static Uri getIcsUri(String table) {
        switch (table) {
            case CALENDAR_CONTENT_CALENDARS:
                return CalendarContract.Calendars.CONTENT_URI;
            case CALENDAR_CONTENT_EVENTS:
                return CalendarContract.Events.CONTENT_URI;
            case CALENDAR_CONTENT_ATTENDEES:
                return CalendarContract.Attendees.CONTENT_URI;
        }
	    return null;
	}
}
