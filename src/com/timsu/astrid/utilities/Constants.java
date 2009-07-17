package com.timsu.astrid.utilities;

import java.util.Date;

import android.app.Activity;

/** Astrid constants */
public class Constants {

    // application constants

    /** URL of Astrid Help Page */
    public static final String HELP_URL = "http://code.google.com/p/android-astrid/wiki/UserGuide";

    /** URL of Astrid Contest Survey */
    public static final String SURVEY_URL = "http://bit.ly/astrid3";

    /** Expiration Date of Astrid Contest Survey link */
    public static final long SURVEY_EXPIRATION = new Date(109, 6, 19).getTime(); // 7-20-2009

    /** Flurry API KEy */
    public static final String FLURRY_KEY = "T3JAY9TV2JFMJR4YTG16";

    public static final boolean DEBUG = true;

    // result codes

    /** Return to the task list view */
    public static final int RESULT_GO_HOME = Activity.RESULT_FIRST_USER;

    /** Discard changes */
    public static final int RESULT_DISCARD = Activity.RESULT_FIRST_USER + 1;

    /** Callback to force synchronization */
    public static final int RESULT_SYNCHRONIZE = Activity.RESULT_FIRST_USER + 2;

}
