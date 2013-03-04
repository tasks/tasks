/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Constants for interfacing with Astrid.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@SuppressWarnings("nls")
public class AstridApiConstants {

    // --- General Constants

    /**
     * Astrid broadcast base package name
     */
    public static final String API_PACKAGE = "com.todoroo.astrid";

    /**
     * Astrid app base package name
     */
    public static final String ASTRID_PACKAGE = "com.timsu.astrid";

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
     * Extras name for plug-in identifier
     */
    public static final String EXTRAS_ADDON = "addon";

    /**
     * Extras name for old task due date
     */
    public static final String EXTRAS_OLD_DUE_DATE= "oldDueDate";

    /**
     * Extras name for new task due date
     */
    public static final String EXTRAS_NEW_DUE_DATE = "newDueDate";

    /**
     * Extras name for sync provider name
     */
    public static final String EXTRAS_NAME = "name";

    // --- Add-ons API

    /**
     * Action name for broadcast intent requesting add-ons
     */
    public static final String BROADCAST_REQUEST_ADDONS = API_PACKAGE + ".REQUEST_ADDONS";

    /**
     * Action name for broadcast intent sending add-ons back to Astrid
     * <li> EXTRAS_RESPONSE an {@link Addon} object
     */
    public static final String BROADCAST_SEND_ADDONS = API_PACKAGE + ".SEND_ADDONS";

    // --- Filters API

    /**
     * Action name for broadcast intent requesting filters
     */
    public static final String BROADCAST_REQUEST_FILTERS = API_PACKAGE + ".REQUEST_FILTERS";

    /**
     * Action name for broadcast intent sending filters back to Astrid
     * <li> EXTRAS_ADDON your add-on identifier </li>
     * <li> EXTRAS_RESPONSE an array of {@link FilterListItem}s </li>
     */
    public static final String BROADCAST_SEND_FILTERS = API_PACKAGE + ".SEND_FILTERS";

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

    // --- Edit Controls API

    /**
     * Action name for broadcast intent requesting task edit controls
     * <li> EXTRAS_TASK_ID id of the task user is editing
     */
    public static final String BROADCAST_REQUEST_EDIT_CONTROLS = API_PACKAGE + ".REQUEST_EDIT_CONTROLS";

    /**
     * Action name for broadcast intent sending task edit controls back to Astrid
     * <li> EXTRAS_ADDON your add-on identifier
     * <li> EXTRAS_RESPONSE a {@link RemoteViews} with your edit controls
     */
    public static final String BROADCAST_SEND_EDIT_CONTROLS = API_PACKAGE + ".SEND_EDIT_CONTROLS";

    // --- Task Details API

    /**
     * Action name for broadcast intent requesting details for a task.
     * Extended details are displayed when a user presses on a task.
     *
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_EXTENDED whether request is for standard or extended details
     */
    public static final String BROADCAST_REQUEST_DETAILS = API_PACKAGE + ".REQUEST_DETAILS";

    /**
     * Action name for broadcast intent sending details back to Astrid
     * <li> EXTRAS_ADDON your add-on identifier
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_EXTENDED whether request is for standard or extended details
     * <li> EXTRAS_RESPONSE a String
     */
    public static final String BROADCAST_SEND_DETAILS = API_PACKAGE + ".SEND_DETAILS";

    // --- Sync Action API

    /**
     * Action name for broadcast intent requesting a listing of active
     * sync actions users can activate from the menu
     */
    public static final String BROADCAST_REQUEST_SYNC_ACTIONS = API_PACKAGE + ".REQUEST_SYNC_ACTIONS";

    /**
     * Action name for broadcast intent sending sync provider information back to Astrid
     * <li> EXTRAS_ADDON your add-on identifier
     * <li> EXTRAS_RESPONSE a {@link SyncAction} to invoke synchronization
     */
    public static final String BROADCAST_SEND_SYNC_ACTIONS = API_PACKAGE + ".SEND_SYNC_ACTIONS";

    // --- Task Decorations API

    /**
     * Action name for broadcast intent requesting task list decorations for a task
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_REQUEST_DECORATIONS = API_PACKAGE + ".REQUEST_DECORATIONS";

    /**
     * Action name for broadcast intent sending decorations back to Astrid
     * <li> EXTRAS_ADDON your add-on identifier
     * <li> EXTRAS_TASK_ID id of the task
     * <li> EXTRAS_RESPONSE a {@link TaskDecoration}
     */
    public static final String BROADCAST_SEND_DECORATIONS = API_PACKAGE + ".SEND_DECORATIONS";

    // --- Actions API

    /**
     * Action name for intents to be displayed on task context menu
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String ACTION_TASK_CONTEXT_MENU = API_PACKAGE + ".CONTEXT_MENU";

    /**
     * Action name for intents to be displayed on Astrid's task list menu
     * <li> EXTRAS_ADDON your add-on identifier
     * <li> EXTRAS_RESPONSE an array of {@link Intent}s
     */
    public static final String ACTION_TASK_LIST_MENU = API_PACKAGE + ".TASK_LIST_MENU";

    /**
     * Action name for intents to be displayed in Astrid's settings. By default,
     * your application will be put into the category named by your application,
     * but you can add a string meta-data with name "category" to override this.
     */
    public static final String ACTION_SETTINGS = API_PACKAGE + ".SETTINGS";

    // --- Events API

    /**
     * Action name for broadcast intent notifying add-ons that Astrid started up
     */
    public static final String BROADCAST_EVENT_STARTUP = API_PACKAGE + ".STARTUP";

    /**
     * Action name for broadcast intent notifying Astrid task list to refresh
     */
    public static final String BROADCAST_EVENT_REFRESH = API_PACKAGE + ".REFRESH";

    /**
     * Action name for broadcast intent notifying Astrid to clear detail cache
     * because an event occurred that potentially affects all tasks (e.g.
     * logging out of a sync provider). Use this call carefully, as loading
     * details can degrade the performance of Astrid.
     */
    public static final String BROADCAST_EVENT_FLUSH_DETAILS = API_PACKAGE + ".FLUSH_DETAILS";

    /**
     * Action name for broadcast intent notifying that task was created or
     * title was changed
     * <li> EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_LIST_UPDATED = API_PACKAGE + ".TASK_LIST_UPDATED";

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
