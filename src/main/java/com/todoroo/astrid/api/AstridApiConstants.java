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
    public static final String EXTRAS_TASK_ID = "task";

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

    /**
     * Action name for broadcast intent notifying that tag was deleted
     */
    public static final String BROADCAST_EVENT_TAG_DELETED = BuildConfig.APPLICATION_ID + ".TAG_DELETED";

    /**
     * Action name for broadcast intent notifying that tag was renamed
     */
    public static final String BROADCAST_EVENT_TAG_RENAMED = BuildConfig.APPLICATION_ID + ".TAG_RENAMED";

    public static final String BROADCAST_EVENT_FILTER_DELETED = BuildConfig.APPLICATION_ID + ".FILTER_DELETED";

    public static final String BROADCAST_EVENT_FILTER_RENAMED = BuildConfig.APPLICATION_ID + ".FILTER_RENAMED";
}
