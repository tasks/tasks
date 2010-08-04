/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;

/**
 * Constants and preferences for rtm plugin
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class ProducteevUtilities {

    // --- constants

    /** add-on identifier */
    public static final String IDENTIFIER = "producteev";

    // --- Preference Keys

    private static final String PREF_TOKEN = "pdv_token";

    private static final String PREF_LAST_SYNC = "pdv_last_sync";

    private static final String PREF_LAST_SYNC_SERVER = "pdv_last_sync_server";

    private static final String PREF_LAST_ATTEMPTED_SYNC = "pdv_last_attempted";

    private static final String PREF_LAST_ERROR = "pdv_last_error";

    private static final String PREF_ONGOING = "pdv_ongoing";

    // --- Preference Utility Methods

    /** Get preferences object from the context */
    private static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ContextManager.getContext());
    }

    /**
     * @return true if we have a token for this user, false otherwise
     */
    public static boolean isLoggedIn() {
        return getPrefs().getString(PREF_TOKEN, null) != null;
    }

    /** authentication token, or null if doesn't exist */
    public static String getToken() {
        return getPrefs().getString(PREF_TOKEN, null);
    }

    /** Sets the authentication token. Set to null to clear. */
    public static void setToken(String setting) {
        Editor editor = getPrefs().edit();
        editor.putString(PREF_TOKEN, setting);
        editor.commit();
    }

    /** @return Last Successful Sync Date, or 0 */
    public static long getLastSyncDate() {
        return getPrefs().getLong(PREF_LAST_SYNC, 0);
    }

    /** @return Last Attempted Sync Date, or 0 if it was successful */
    public static long getLastAttemptedSyncDate() {
        return getPrefs().getLong(PREF_LAST_ATTEMPTED_SYNC, 0);
    }

    /** @return Last Error, or null if no last error */
    public static String getLastError() {
        return getPrefs().getString(PREF_LAST_ERROR, null);
    }

    /** @return Last sync time according to producteev, or null */
    public static String getLastServerSyncTime() {
        return getPrefs().getString(PREF_LAST_SYNC_SERVER, null);
    }

    /** @return Last Error, or null if no last error */
    public static boolean isOngoing() {
        return getPrefs().getBoolean(PREF_ONGOING, false);
    }

    /** Deletes Last Successful Sync Date */
    public static void clearLastSyncDate() {
        Editor editor = getPrefs().edit();
        editor.remove(PREF_LAST_SYNC);
        editor.commit();
    }

    /** Set Last Successful Sync Date */
    public static void setLastError(String error) {
        Editor editor = getPrefs().edit();
        editor.putString(PREF_LAST_ERROR, error);
        editor.commit();
    }

    /** Set Ongoing */
    public static void stopOngoing() {
        Editor editor = getPrefs().edit();
        editor.putBoolean(PREF_ONGOING, false);
        editor.commit();
    }

    /** Set Last Successful Sync Date */
    public static void recordSuccessfulSync(String producteevTime) {
        Editor editor = getPrefs().edit();
        editor.putLong(PREF_LAST_SYNC, DateUtilities.now());
        editor.putLong(PREF_LAST_ATTEMPTED_SYNC, 0);
        editor.putString(PREF_LAST_SYNC_SERVER, producteevTime);
        editor.commit();
    }

    /** Set Last Attempted Sync Date */
    public static void recordSyncStart() {
        Editor editor = getPrefs().edit();
        editor.putLong(PREF_LAST_ATTEMPTED_SYNC, DateUtilities.now());
        editor.putString(PREF_LAST_ERROR, null);
        editor.putBoolean(PREF_ONGOING, true);
        editor.commit();
    }

    /**
     * Reads the frequency, in seconds, auto-sync should occur.
     *
     * @return seconds duration, or 0 if not desired
     */
    public static int getSyncAutoSyncFrequency() {
        String value = getPrefs().getString(
                ContextManager.getContext().getString(
                        R.string.rmilk_MPr_interval_key), null);
        if (value == null)
            return 0;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

}
