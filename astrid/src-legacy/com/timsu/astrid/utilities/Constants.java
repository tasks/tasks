package com.timsu.astrid.utilities;

import android.app.Activity;

/** Astrid constants */
@Deprecated
public class Constants {

    // application constants

    /** URL of Astrid Help Page */
    public static final String HELP_URL = "http://weloveastrid.com/?page_id=59";

    /** Flurry API Key */
    public static final String FLURRY_KEY = "T3JAY9TV2JFMJR4YTG16";

    public static final boolean DEBUG = true;

    public static final long WIDGET_UPDATE_INTERVAL = 30 * 60 * 1000L;

    /**
     * Whether this is an OEM preinstallation
     */
    public static final boolean OEM = false;

    /**
     * Whether to display synchronization options
     */
    public static final boolean SYNCHRONIZE = !OEM;

    // result codes

    /** Return to the task list view */
    public static final int RESULT_GO_HOME = Activity.RESULT_FIRST_USER;

    /** Discard changes */
    public static final int RESULT_DISCARD = Activity.RESULT_FIRST_USER + 1;

    /** Callback to force synchronization */
    public static final int RESULT_SYNCHRONIZE = Activity.RESULT_FIRST_USER + 2;

}
