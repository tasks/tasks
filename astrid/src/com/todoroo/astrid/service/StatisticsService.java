/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.service;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;

import com.localytics.android.LocalyticsSession;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.Constants;

public class StatisticsService {

    private static LocalyticsSession localyticsSession;

    /**
     * Indicate session started
     *
     * @param context
     */
    public static void sessionStart(Context context) {
        if(dontCollectStatistics())
            return;

        if(localyticsSession != null) {
            localyticsSession.open(); // Multiple calls to open are ok, we just need to make sure it gets reopened after pause
        } else {
            localyticsSession = new LocalyticsSession(context.getApplicationContext(),
                    Constants.LOCALYTICS_KEY);
            localyticsSession.open();
            localyticsSession.upload();
        }

        if (context instanceof Activity)
            localyticsSession.tagScreen(context.getClass().getSimpleName());
    }

    /**
     * Indicate session ended
     *
     * @param context
     */
    public static void sessionStop(Context context) {
        if(dontCollectStatistics())
            return;

        if(localyticsSession != null)
            localyticsSession.upload();
    }

    /**
     * Indicate session was paused
     */
    public static void sessionPause() {
        if(dontCollectStatistics())
            return;

        if(localyticsSession != null) {
            localyticsSession.close();
        }
    }

    /**
     * Indicates an error occurred
     * @param name
     * @param message
     * @param trace
     */
    public static void reportError(String name, String message, String trace) {
        // no reports yet
    }

    /**
     * Indicates an event should be reported
     * @param event
     */
    public static void reportEvent(String event, String... attributes) {
        if(dontCollectStatistics())
            return;

        if(localyticsSession != null) {
            if(attributes.length > 0) {
                HashMap<String, String> attrMap = new HashMap<String, String>();
                for(int i = 1; i < attributes.length; i += 2) {
                    if(attributes[i] != null)
                        attrMap.put(attributes[i-1], attributes[i]);
                }
                localyticsSession.tagEvent(event, attrMap);
            } else
                localyticsSession.tagEvent(event);
        }
    }

    public static boolean dontCollectStatistics() {
        return !Preferences.getBoolean(R.string.p_statistics, true);
    }

}
