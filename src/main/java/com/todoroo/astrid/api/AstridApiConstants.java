/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import org.tasks.BuildConfig;

/**
 * Constants for interfacing with Astrid.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class AstridApiConstants {

    // --- General Constants

    /**
     * Permission for reading tasks and receiving to GET_FILTERS intent
     */
    public static final String PERMISSION_READ = BuildConfig.APPLICATION_ID + ".READ";

    /**
     * Permission for writing and creating tasks
     */
    public static final String PERMISSION_WRITE = BuildConfig.APPLICATION_ID + ".WRITE";

    /**
     * Name of Astrid's publicly readable preference store
     */
    public static final String PUBLIC_PREFS = "public";

    // --- Content Provider

    /**
     * URI to append to base content URI for making group-by queries
     */
    public static final String GROUP_BY_URI = "/groupby/";

    // --- Broadcast Extras

    /**
     * Extras name for task id
     */
    public static final String EXTRAS_TASK_ID = "task_id";

    public static final String EXTRAS_TASK = "task";
    public static final String EXTRAS_VALUES = "values";
    /**
     * Extras name for old task due date
     */
    public static final String EXTRAS_OLD_DUE_DATE= "oldDueDate";

    /**
     * Extras name for new task due date
     */
    public static final String EXTRAS_NEW_DUE_DATE = "newDueDate";

    // --- Events API

    /**
     * Action name for broadcast intent notifying Astrid task list to refresh
     */
    public static final String BROADCAST_EVENT_REFRESH = BuildConfig.APPLICATION_ID + ".REFRESH";
    public static final String BROADCAST_EVENT_REFRESH_LISTS = BuildConfig.APPLICATION_ID + ".REFRESH_LISTS";

    /**
     * Action name for broadcast intent notifying that task was completed
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_COMPLETED = BuildConfig.APPLICATION_ID + ".TASK_COMPLETED";

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_OLD_DUE_DATE task old due date (could be 0)
     * <li> EXTRAS_NEW_DUE_DATE task new due date (will not be 0)
     */
    public static final String BROADCAST_EVENT_TASK_REPEATED = BuildConfig.APPLICATION_ID + ".TASK_REPEATED";

    public static final String BROADCAST_EVENT_TASK_SAVED = BuildConfig.APPLICATION_ID + ".TASK_SAVED";
}
