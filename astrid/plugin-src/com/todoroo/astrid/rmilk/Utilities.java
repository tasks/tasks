/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.rmilk.data.MilkList;

/**
 * Utility constants
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Utilities {

    // --- constants

    /** add-on identifier */
    public static final String IDENTIFIER = "rmilk";

    // --- helper classes

    /**
     * Helper class for describing RTM lists
     *
     * @author Tim Su <tim@todoroo.com>
     */
    public static class ListContainer {
        public ListContainer(MilkList list) {
            this(list.getValue(MilkList.ID), list.getValue(MilkList.NAME));
        }

        public ListContainer(long id, String name) {
            this.id = id;
            this.name = name;
            this.count = 0;
        }

        @Override
        public String toString() {
            return name;
        }

        public long id;
        public String name;
        public int count;
    }

    // --- Preference Keys

    private static final String PREF_TOKEN = "rmilk_token";

    private static final String PREF_LAST_SYNC = "rmilk_last_sync";

    private static final String PREF_LAST_ATTEMPTED_SYNC = "rmilk_last_attempted";

    private static final String PREF_LAST_ERROR = "rmilk_last_error";

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

    /** RTM authentication token, or null if doesn't exist */
    public static String getToken() {
        return getPrefs().getString(PREF_TOKEN, null);
    }

    /** Sets the RTM authentication token. Set to null to clear. */
    public static void setToken(String setting) {
        Editor editor = getPrefs().edit();
        editor.putString(PREF_TOKEN, setting);
        editor.commit();
    }

    /** @return RTM Last Successful Sync Date, or 0 */
    public static long getLastSyncDate() {
        return getPrefs().getLong(PREF_LAST_SYNC, 0);
    }

    /** @return RTM Last Attempted Sync Date, or 0 if it was successful */
    public static long getLastAttemptedSyncDate() {
        return getPrefs().getLong(PREF_LAST_ATTEMPTED_SYNC, 0);
    }

    /** @return RTM Last Error, or null if no last error */
    public static String getLastError() {
        return getPrefs().getString(PREF_LAST_ERROR, null);
    }

    /** Deletes RTM Last Successful Sync Date */
    public static void clearLastSyncDate() {
        Editor editor = getPrefs().edit();
        editor.remove(PREF_LAST_SYNC);
        editor.commit();
    }

    /** Set RTM Last Successful Sync Date */
    public static void setLastError(String error) {
        Editor editor = getPrefs().edit();
        editor.putString(PREF_LAST_ERROR, error);
        editor.commit();
    }

    /** Set RTM Last Successful Sync Date */
    public static void recordSuccessfulSync() {
        Editor editor = getPrefs().edit();
        editor.putLong(PREF_LAST_SYNC, DateUtilities.now());
        editor.putLong(PREF_LAST_ATTEMPTED_SYNC, 0);
        editor.commit();
    }

    /** Set RTM Last Attempted Sync Date */
    public static void recordSyncStart() {
        Editor editor = getPrefs().edit();
        editor.putLong(PREF_LAST_ATTEMPTED_SYNC, DateUtilities.now());
        editor.putString(PREF_LAST_ERROR, null);
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
