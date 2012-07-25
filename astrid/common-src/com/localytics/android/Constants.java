/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.localytics.android;

import android.text.format.DateUtils;

/**
 * Build constants for the Localytics library.
 * <p>
 * This is not a public API.
 */
/* package */final class Constants
{

    /**
     * Version number of this library. This number is primarily important in terms of changes to the upload format.
     */
    //@formatter:off
    /*
     * Version history:
     *
     * 1.6: Fixed network type reporting.  Added reporting of app signature, device SDK level, device manufacturer, serial number.
     * 2.0: New upload format.
     */
    //@formatter:on
    public static final String LOCALYTICS_CLIENT_LIBRARY_VERSION = "android_2.2"; //$NON-NLS-1$

    /**
     * The package name of the Localytics library.
     */
    /*
     * Note: This value cannot be changed without significant consequences to the data in the database.
     */
    public static final String LOCALYTICS_PACKAGE_NAME = "com.localytics.android"; //$NON-NLS-1$

    /**
     * Maximum number of sessions to store on disk.
     */
    public static final int MAX_NUM_SESSIONS = 10;

    /**
     * Maximum number of attributes per event session.
     */
    public static final int MAX_NUM_ATTRIBUTES = 10;

    /**
     * Maximum characters in an event name or attribute key/value.
     */
    public static final int MAX_NAME_LENGTH = 128;

    /**
     * Milliseconds after which a session is considered closed and cannot be reattached to.
     * <p>
     * For example, if the user opens an app, presses home, and opens the app again in less than {@link #SESSION_EXPIRATION}
     * milliseconds, that will count as one session rather than two sessions.
     */
    public static long SESSION_EXPIRATION = 15 * DateUtils.SECOND_IN_MILLIS;

    /**
     * logcat log tag
     */
    public static final String LOG_TAG = "Localytics"; //$NON-NLS-1$

    /**
     * Boolean indicating whether logcat messages are enabled.
     * <p>
     * Before releasing a production version of an app, this should be set to false for privacy and performance reasons. When
     * logging is enabled, sensitive information such as the device ID may be printed to the log.
     */
    public static boolean IS_LOGGABLE = false;

    /**
     * Flag indicating whether runtime method parameter checking is performed.
     */
    public static boolean ENABLE_PARAMETER_CHECKING = true;

    /**
     * Cached copy of the current Android API level
     * 
     * @see DatapointHelper#getApiLevel()
     */
    /*package*/ static final int CURRENT_API_LEVEL = DatapointHelper.getApiLevel();

    /**
     * Private constructor prevents instantiation
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private Constants()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
}
