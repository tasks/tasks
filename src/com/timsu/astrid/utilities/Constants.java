package com.timsu.astrid.utilities;

import android.app.Activity;

/** Astrid constants */
public class Constants {

    // application constants

    public static final String HELP_URL = "http://code.google.com/p/android-astrid/wiki/UserGuide";

    // result codes

    /** Return to the task list view */
    public static final int RESULT_GO_HOME = Activity.RESULT_FIRST_USER;

    /** Callback from synchronization */
    public static final int RESULT_SYNCHRONIZE = Activity.RESULT_FIRST_USER + 1;
}
