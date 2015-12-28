/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.net.Uri;
import android.provider.CalendarContract;

import javax.inject.Singleton;

@Singleton
public class Calendars {

    public static final String CALENDAR_CONTENT_CALENDARS = "calendars";
    public static final String CALENDAR_CONTENT_EVENTS = "events";
    public static final String CALENDAR_CONTENT_ATTENDEES = "attendees";

	public static final String ID_COLUMN_NAME = "_id";
	public static final String CALENDARS_DISPLAY_COL = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME;
	public static final String CALENDARS_ACCESS_LEVEL_COL = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL;
	public static final String EVENTS_DTSTART_COL = CalendarContract.Events.DTSTART;
	public static final String EVENTS_DTEND_COL = CalendarContract.Events.DTEND;
	public static final String EVENTS_NAME_COL = CalendarContract.Events.TITLE;
	public static final String ATTENDEES_EVENT_ID_COL = CalendarContract.Attendees.EVENT_ID;
    public static final String ATTENDEES_NAME_COL = CalendarContract.Attendees.ATTENDEE_NAME;
    public static final String ATTENDEES_EMAIL_COL = CalendarContract.Attendees.ATTENDEE_EMAIL;

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
