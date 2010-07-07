/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

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

    /**
     * Extras name for Task id
     */
    public static final String EXTRAS_TASK_ID = "task";

    /**
     * Extras name for an array of response items
     */
    public static final String EXTRAS_ITEMS = "items";

    /**
     * Extras name for plug-in object
     */
    public static final String EXTRAS_PLUGIN = "plugin";

    // --- Plug-ins API

    /**
     * Action name for broadcast intent requesting filters
     */
    public static final String BROADCAST_REQUEST_PLUGINS = PACKAGE + ".REQUEST_PLUGINS";

    /**
     * Action name for broadcast intent sending filters back to Astrid
     */
    public static final String BROADCAST_SEND_PLUGINS = PACKAGE + ".SEND_PLUGINS";

    // --- Filters API

    /**
     * Action name for broadcast intent requesting filters
     */
    public static final String BROADCAST_REQUEST_FILTERS = PACKAGE + ".REQUEST_FILTERS";

    /**
     * Action name for broadcast intent sending filters back to Astrid
     */
    public static final String BROADCAST_SEND_FILTERS = PACKAGE + ".SEND_FILTERS";

    // --- Edit Operations API

    /**
     * Action name for broadcast intent requesting task edit operations
     */
    public static final String BROADCAST_REQUEST_EDIT_OPERATIONS = PACKAGE + ".REQUEST_EDIT_OPERATIONS";

    /**
     * Action name for broadcast intent sending task edit operations back to Astrid
     */
    public static final String BROADCAST_SEND_EDIT_OPERATIONS = PACKAGE + ".SEND_EDIT_OPERATIONS";

    // --- Task List Details API

    /**
     * Action name for broadcast intent requesting task list details for a task
     */
    public static final String BROADCAST_REQUEST_DETAILS = PACKAGE + ".REQUEST_DETAILS";

    /**
     * Action name for broadcast intent sending details back to Astrid
     */
    public static final String BROADCAST_SEND_DETAILS = PACKAGE + ".SEND_DETAILS";

    // --- Actions API

    /**
     * Action name for intents to be displayed on task context menu
     */
    public static final String ACTION_TASK_CONTEXT_MENU = PACKAGE + ".CONTEXT_MENU";

    /**
     * Action name for intents to be displayed on Astrid's task list menu
     */
    public static final String ACTION_TASK_LIST_MENU = PACKAGE + ".TASK_LIST_MENU";

    /**
     * Action name for intents to be displayed in Astrid's settings
     */
    public static final String ACTION_SETTINGS = PACKAGE + ".SETTINGS";

    // --- Events API

    /**
     * Action name for broadcast intent notifying that task was completed
     */
    public static final String BROADCAST_EVENT_TASK_COMPLETED = PACKAGE + ".TASK_COMPLETED";

    /**
     * Action name for broadcast intent notifying that task was created
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
