/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

import com.todoroo.astrid.service.MarketStrategy;

@SuppressWarnings("nls")
public final class Constants {

    // --- general application constants

    /**
     * LCL API Key
     */
    public static final String LOCALYTICS_KEY_LITE = "f3a40b93823ac2024b062f2-d96a8860-4a2c-11e2-35ca-004b50a28849";
    public static final String LOCALYTICS_KEY = "ae35a010c66a997ab129ab7-3e2adf46-8bb3-11e0-fe8b-007f58cb3154";

    /**
     * Application Package
     */
    public static final String PACKAGE = "com.timsu.astrid";

    /**
     * Whether this is an OEM installation
     */
    public static final boolean OEM = false;

    /**
     * Market selection strategy
     */
    public static final MarketStrategy MARKET_STRATEGY = new MarketStrategy.AndroidMarketStrategy();

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
     * Whether this version is Astrid Lite
     */
    public static final boolean ASTRID_LITE = false;

    /**
     * Astrid Help URL
     */
    public static final String HELP_URL = "http://weloveastrid.com/help-user-guide-astrid-v3/active-tasks/";

    // --- task list activity source strings

    public static final int SOURCE_OTHER = -1;
    public static final int SOURCE_DEFAULT = 0;
    public static final int SOURCE_NOTIFICATION = 1;
    public static final int SOURCE_WIDGET = 2;
    public static final int SOURCE_PPWIDGET = 3;
    public static final int SOURCE_C2DM = 4;
    public static final int SOURCE_REENGAGEMENT = 5;

    // --- notification id's

    /** Notification Manager id for sync notifications */
    public static final int NOTIFICATION_SYNC = -1;

    /** Notification Manager id for timing */
    public static final int NOTIFICATION_TIMER = -2;

    /** Notification Manager id for locale */
    public static final int NOTIFICATION_LOCALE = -3;

    /** Notification Manager id for astrid.com */
    public static final int NOTIFICATION_ACTFM = -5;

    // --- crittercism

    public static final String CRITTERCISM_APP_ID = "4e8a796fddf5203b6f0097c5";

    // --- amazon

    public static final String AWS_ACCESS_KEY_ID = "AKIAJTVL4FOF4PRBKBNA";

    public static final String AWS_SECRET_KEY_ID = "";

    public static final String AWS_ENDPOINT = "ecs.amazonaws.com";

}
