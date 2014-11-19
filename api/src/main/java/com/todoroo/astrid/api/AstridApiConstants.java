/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

/**
 * Constants for interfacing with Astrid.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class AstridApiConstants {

    // --- General Constants

    /**
     * Astrid broadcast base package name
     */
    public static final String API_PACKAGE = "org.tasks";

    /**
     * Astrid app base package name
     */
    public static final String ASTRID_PACKAGE = "org.tasks";

    /**
     * Permission for reading tasks and receiving to GET_FILTERS intent
     */
    public static final String PERMISSION_READ = API_PACKAGE + ".READ";

    /**
     * Permission for writing and creating tasks
     */
    public static final String PERMISSION_WRITE = API_PACKAGE + ".WRITE";

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
     * Extras name for a response item broadcast to astrid
     */
    public static final String EXTRAS_RESPONSE = "response";

    /**
     * Extras name for old task due date
     */
    public static final String EXTRAS_OLD_DUE_DATE= "oldDueDate";

    /**
     * Extras name for new task due date
     */
    public static final String EXTRAS_NEW_DUE_DATE = "newDueDate";

    // -- Custom criteria API

    /**
     * Action name for a broadcast intent requesting custom filter criteria (e.g. "Due by, Tagged, Tag contains", etc.)
     */
    public static final String BROADCAST_REQUEST_CUSTOM_FILTER_CRITERIA = API_PACKAGE + ".REQUEST_CUSTOM_FILTER_CRITERIA";


    /**
     * Action name for broadcast intent sending custom filter criteria back to Astrid
     * <li> EXTRAS_ADDON you add-on identifier
     * <li> EXTRAS_RESPONSE an array of {@link CustomFilterCriterion} </li>
     */
    public static final String BROADCAST_SEND_CUSTOM_FILTER_CRITERIA = API_PACKAGE + ".SEND_CUSTOM_FILTER_CRITERIA";

    // --- Actions API

    /**
     * Action name for intents to be displayed in Astrid's settings. By default,
     * your application will be put into the category named by your application,
     * but you can add a string meta-data with name "category" to override this.
     */
    public static final String ACTION_SETTINGS = API_PACKAGE + ".SETTINGS";

    // --- Events API

    /**
     * Action name for broadcast intent notifying Astrid task list to refresh
     */
    public static final String BROADCAST_EVENT_REFRESH = API_PACKAGE + ".REFRESH";

    /**
     * Action name for broadcast intent notifying that task was created or
     * title was changed
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_LIST_UPDATED = API_PACKAGE + ".TASK_LIST_UPDATED";

    public static final String BROADCAST_EVENT_FILTER_LIST_UPDATED = API_PACKAGE + ".FILTER_LIST_UPDATED";

    /**
     * Action name for broadcast intent notifying that task was completed
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_COMPLETED = API_PACKAGE + ".TASK_COMPLETED";

    /**
     * Action name for broadcast intent notifying that task was created from repeating template
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_OLD_DUE_DATE task old due date (could be 0)
     * <li> EXTRAS_NEW_DUE_DATE task new due date (will not be 0)
     */
    public static final String BROADCAST_EVENT_TASK_REPEATED = API_PACKAGE + ".TASK_REPEATED";

    /**
     * Action name for broadcast intent notifying that a repeating task has passed its repeat_until value
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_OLD_DUE_DATE task old due date (could be 0)
     * <li> EXTRAS_NEW_DUE_DATE task new due date (will not be 0)
     */
    public static final String BROADCAST_EVENT_TASK_REPEAT_FINISHED = API_PACKAGE + ".TASK_REPEAT_FINISHED";

    /**
     * Action name for broadcast intent notifying that tag was deleted
     */
    public static final String BROADCAST_EVENT_TAG_DELETED = API_PACKAGE + ".TAG_DELETED";

    /**
     * Action name for broadcast intent notifying that tag was renamed
     */
    public static final String BROADCAST_EVENT_TAG_RENAMED = API_PACKAGE + ".TAG_RENAMED";

}
