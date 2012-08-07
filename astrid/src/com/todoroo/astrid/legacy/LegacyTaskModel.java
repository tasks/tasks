/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy;

/** Legacy task class */
@SuppressWarnings("nls")
abstract public class LegacyTaskModel {

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String NOTES = "notes";
    public static final String PROGRESS_PERCENTAGE = "progressPercentage";
    public static final String IMPORTANCE = "importance";
    public static final String ESTIMATED_SECONDS = "estimatedSeconds";
    public static final String ELAPSED_SECONDS = "elapsedSeconds";
    public static final String TIMER_START = "timerStart";
    public static final String DEFINITE_DUE_DATE = "definiteDueDate";
    public static final String PREFERRED_DUE_DATE = "preferredDueDate";
    public static final String HIDDEN_UNTIL = "hiddenUntil";
    public static final String POSTPONE_COUNT = "postponeCount";
    public static final String NOTIFICATIONS = "notifications";
    public static final String NOTIFICATION_FLAGS = "notificationFlags";
    public static final String LAST_NOTIFIED = "lastNotified";
    public static final String REPEAT = "repeat";
    public static final String CREATION_DATE = "creationDate";
    public static final String COMPLETION_DATE = "completionDate";
    public static final String CALENDAR_URI = "calendarUri";
    public static final String FLAGS = "flags";
    public static final String BLOCKING_ON = "blockingOn";

    // notification flags
    public static final int NOTIFY_BEFORE_DEADLINE = 1 << 0;
    public static final int NOTIFY_AT_DEADLINE     = 1 << 1;
    public static final int NOTIFY_AFTER_DEADLINE  = 1 << 2;
    public static final int NOTIFY_NONSTOP         = 1 << 3;

    // other flags
    public static final int FLAG_SYNC_ON_COMPLETE  = 1 << 0;

}
