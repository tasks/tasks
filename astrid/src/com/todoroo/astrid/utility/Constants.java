package com.todoroo.astrid.utility;


public final class Constants {

    // --- general application constants

    /**
     * LCL API Key
     */
    public static final String LOCALYTICS_KEY = "ae35a010c66a997ab129ab7-3e2adf46-8bb3-11e0-fe8b-007f58cb3154"; //$NON-NLS-1$

    /**
     * Application Package
     */
    public static final String PACKAGE = "com.timsu.astrid"; //$NON-NLS-1$

    /**
     * Whether this is an OEM installation
     */
    public static final boolean OEM = false;

    /**
     * Whether this is an Android Market-disabled build
     */
    public static final boolean MARKET_DISABLED = false;

    /**
     * Interval to update the widget (in order to detect hidden tasks
     * becoming visible)
     */
    public static final long WIDGET_UPDATE_INTERVAL = 30 * 60 * 1000L;

    /**
     * Whether to turn on debugging logging and UI
     */
    public static final boolean DEBUG = false;

    /**
     * Whether to turn on debugging for an emulated zte blade error (in the FilterReceiver)
     * Only for testing purposes. Remember to set this to false for a standard release!
     */
    public static final boolean DEBUG_BLADE = false;

    /**
     * Astrid Help URL
     */
    public static final String HELP_URL = "http://weloveastrid.com/help-user-guide-astrid-v3/active-tasks/"; //$NON-NLS-1$

    // --- task list activity source strings

    public static final int SOURCE_OTHER = -1;
    public static final int SOURCE_DEFAULT = 0;
    public static final int SOURCE_NOTIFICATION = 1;
    public static final int SOURCE_WIDGET = 2;
    public static final int SOURCE_PPWIDGET = 3;
    public static final int SOURCE_C2DM = 4;

    // --- notification id's

    /** Notification Manager id for sync notifications */
    public static final int NOTIFICATION_SYNC = -1;

    /** Notification Manager id for timing */
    public static final int NOTIFICATION_TIMER = -2;

    /** Notification Manager id for locale */
    public static final int NOTIFICATION_LOCALE = -3;

    /** Notification Manager id for producteev notifications*/
    public static final int NOTIFICATION_PRODUCTEEV_NOTIFICATIONS = -4;

    /** Notification Manager id for astrid.com */
    public static final int NOTIFICATION_ACTFM = -5;

    // --- crittercism

    public static final String CRITTERCISM_APP_ID = "4e8a796fddf5203b6f0097c5"; //$NON-NLS-1$

    public static final String CRITTERCISM_SECRET = "9mhdwlu85lc6sovpxkabq1cbzzmxe2oi"; //$NON-NLS-1$

    public static final String CRITTERCISM_OATH_KEY = "4e8a796fddf5203b6f0097c5nn35ziwt"; //$NON-NLS-1$
}
