package com.todoroo.astrid.utility;

public final class Constants {

    /**
     * Flurry API Key
     */
    public static final String FLURRY_KEY = "T3JAY9TV2JFMJR4YTG16"; //$NON-NLS-1$

    /**
     * Application Package
     */
    public static final String PACKAGE = "com.timsu.astrid"; //$NON-NLS-1$

    /**
     * Whether this is an OEM installation
     */
    public static final boolean OEM = false;

    /**
     * Whether to display synchronization options
     */
    public static final boolean SYNCHRONIZE = !OEM;

    /**
     * Interval to update the widget (in order to detect hidden tasks
     * becoming visible)
     */
    public static final long WIDGET_UPDATE_INTERVAL = 30 * 60 * 1000L;

    /**
     * Whether to turn on debugging logging and UI
     */
    public static final boolean DEBUG = false;
}
