package com.timsu.astrid.utilities;

import android.app.Activity;

/** Astrid constants */
public class Constants {

    // application constants

    /** URL of Astrid Help Page */
    public static final String HELP_URL = "http://code.google.com/p/android-astrid/wiki/UserGuide";

    /** URL of Astrid Feature Survey */
    public static final String SURVEY_URL = "http://www.haveasec.com/survey/m/detail/welcome/bf25e0/";

    public static final boolean DEBUG = false;

    // result codes

    /** Return to the task list view */
    public static final int RESULT_GO_HOME = Activity.RESULT_FIRST_USER;

    /** Callback to force synchronization */
    public static final int RESULT_SYNCHRONIZE = Activity.RESULT_FIRST_USER + 1;

}
