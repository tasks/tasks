/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gcal;

import android.provider.CalendarContract;

import javax.inject.Singleton;

import static android.provider.BaseColumns._ID;

@Singleton
public class Calendars {

	public static final String[] CALENDARS_PROJECTION = new String[] {
			_ID,
			CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
	};

	// Only show calendars that the user can modify. Access level 500
	// corresponds to Calendars.CONTRIBUTOR_ACCESS
	public static final String CALENDARS_WHERE = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">= 500";

	public static final String CALENDARS_SORT = CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC";
}
