/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

import com.todoroo.astrid.service.MarketStrategy;

public final class Constants {

    // --- general application constants

    /**
     * Application Package
     */
    public static final String PACKAGE = "org.tasks";

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

    // --- task list activity source strings

    public static final int SOURCE_OTHER = -1;
    public static final int SOURCE_DEFAULT = 0;
    public static final int SOURCE_NOTIFICATION = 1;
    public static final int SOURCE_WIDGET = 2;
    public static final int SOURCE_PPWIDGET = 3;
    public static final int SOURCE_C2DM = 4;
    public static final int SOURCE_REENGAGEMENT = 5;

    // --- notification id's

    /** Notification Manager id for timing */
    public static final int NOTIFICATION_TIMER = -2;
}
