/**
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.service;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.Constants;

public class StatisticsService {

    /**
     * Indicate session started
     *
     * @param context
     */
    public static void sessionStart(Context context) {
        if(dontCollectStatistics())
            return;

        FlurryAgent.onStartSession(context, Constants.FLURRY_KEY);
    }

    /**
     * Indicate session ended
     *
     * @param context
     */
    public static void sessionStop(Context context) {
        if(dontCollectStatistics())
            return;

        FlurryAgent.onEndSession(context);
    }

    public static void reportError(String name, String message, String trace) {
        if(dontCollectStatistics())
            return;

        FlurryAgent.onError(name, message, trace);
    }

    public static void reportEvent(String event) {
        if(dontCollectStatistics())
            return;

        FlurryAgent.onEvent(event);
    }

    private static boolean dontCollectStatistics() {
        return !Preferences.getBoolean(R.string.p_statistics, true);
    }

}
