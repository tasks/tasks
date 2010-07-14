/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk;

import java.util.Date;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.rmilk.data.MilkList;

/**
 * Utility constants
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Utilities {

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
            this.count = -1;
        }

        @Override
        public String toString() {
            return name;
        }

        public long id;
        public String name;
        public int count;
    }

    // --- Metadata keys
    // NOTE: no sql escaping is provided for keys

    public static final String KEY_LIST_ID = "rmilk_listId";

    public static final String KEY_TASK_SERIES_ID = "rmilk_taskSeriesId";

    public static final String KEY_TASK_ID = "rmilk_taskId";

    public static final String KEY_REPEAT = "rmilk_repeat";

    public static final String KEY_UPDATED = "rmilk_updated";

    // --- Preference Keys

    private static final String PREF_TOKEN = "rmilk_token";

    private static final String PREF_LAST_SYNC = "rmilk_last_sync";

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

    /** RTM Last Successful Sync Date, or null */
    public static Date getLastSyncDate() {
        Long value = getPrefs().getLong(PREF_LAST_SYNC, 0);
        if (value == 0)
            return null;
        return new Date(value);
    }

    /** Set RTM Last Successful Sync Date */
    public static void setLastSyncDate(Date date) {
        Editor editor = getPrefs().edit();
        if (date == null) {
            editor.remove(PREF_LAST_SYNC);
        } else {
            editor.putLong(PREF_LAST_SYNC, date.getTime());
        }
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
