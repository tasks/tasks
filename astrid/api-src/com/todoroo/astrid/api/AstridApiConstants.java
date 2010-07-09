/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

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
     * Astrid application package name
     */
    public static final String PACKAGE = "com.todoroo.astrid";

    /**
     * Permission for reading tasks and receiving to GET_FILTERS intent
     */
    public static final String PERMISSION_READ = PACKAGE + ".READ";

    /**
     * Permission for writing and creating tasks
     */
    public static final String PERMISSION_WRITE = PACKAGE + ".WRITE";

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

    // --- Add-ons API

    /**
     * Action name for broadcast intent requesting add-ons
     */
    public static final String BROADCAST_REQUEST_ADDONS = PACKAGE + ".REQUEST_ADDONS";

    /**
     * Action name for broadcast intent sending add-ons back to Astrid
     * @extra EXTRAS_RESPONSE an {@link Addon} object
     */
    public static final String BROADCAST_SEND_ADDONS = PACKAGE + ".SEND_ADDONS";

    // --- Filters API

    /**
     * Action name for broadcast intent requesting filters
     */
    public static final String BROADCAST_REQUEST_FILTERS = PACKAGE + ".REQUEST_FILTERS";

    /**
     * Action name for broadcast intent sending filters back to Astrid
     * @extra EXTRAS_ADDON your add-on identifier
     * @extra EXTRAS_RESPONSE an array of {@link FilterListItem}s
     */
    public static final String BROADCAST_SEND_FILTERS = PACKAGE + ".SEND_FILTERS";

    // --- Edit Controls API

    /**
     * Action name for broadcast intent requesting task edit controls
     * @extra EXTRAS_TASK_ID id of the task user is editing
     */
    public static final String BROADCAST_REQUEST_EDIT_CONTROLS = PACKAGE + ".REQUEST_EDIT_CONTROLS";

    /**
     * Action name for broadcast intent sending task edit controls back to Astrid
     * @extra EXTRAS_ADDON your add-on identifier
     * @extra EXTRAS_RESPONSE a {@link RemoteViews} with your edit controls
     */
    public static final String BROADCAST_SEND_EDIT_CONTROLS = PACKAGE + ".SEND_EDIT_CONTROLS";

    // --- Task List Details API

    /**
     * Action name for broadcast intent requesting task list details for a task
     * @extra EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_REQUEST_DETAILS = PACKAGE + ".REQUEST_DETAILS";

    /**
     * Action name for broadcast intent sending details back to Astrid
     * @extra EXTRAS_ADDON your add-on identifier
     * @extra EXTRAS_TASK_ID id of the task
     * @extra EXTRAS_RESPONSE a {@link TaskDetail} object
     */
    public static final String BROADCAST_SEND_DETAILS = PACKAGE + ".SEND_DETAILS";

    // --- Actions API

    /**
     * Action name for intents to be displayed on task context menu
     * @extra EXTRAS_TASK_ID id of the task
     */
    public static final String ACTION_TASK_CONTEXT_MENU = PACKAGE + ".CONTEXT_MENU";

    /**
     * Action name for intents to be displayed on Astrid's task list menu
     * @extra EXTRAS_ADDON your add-on identifier
     * @extra EXTRAS_RESPONSE an array of {@link Intent}s
     */
    public static final String ACTION_TASK_LIST_MENU = PACKAGE + ".TASK_LIST_MENU";

    // --- Settings API

    /**
     * Action name for intents to be displayed in Astrid's settings
     */
    public static final String ACTION_SETTINGS = PACKAGE + ".SETTINGS";

    // --- Events API

    /**
     * Action name for broadcast intent notifying that task was completed
     * @extra EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_COMPLETED = PACKAGE + ".TASK_COMPLETED";

    /**
     * Action name for broadcast intent notifying that task was created
     * @extra EXTRAS_TASK_ID id of the task
     */
    public static final String BROADCAST_EVENT_TASK_CREATED = PACKAGE + ".TASK_CREATED";

    // --- SQL Constants

    /**
     * Table name for tasks
     */
    public static final String TASK_TABLE = "tasks";

    /**
     * Table name for metadata
     */
    public static final String METADATA_TABLE = "metadata";

}
