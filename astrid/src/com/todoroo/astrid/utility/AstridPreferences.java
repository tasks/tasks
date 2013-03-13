/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;


import android.content.Context;
import android.content.SharedPreferences;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.tags.reusable.FeaturedListFilterExposer;

public class AstridPreferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$

    private static final String P_CURRENT_VERSION_NAME = "cvname"; //$NON-NLS-1$

    public static final String P_FIRST_TASK = "ft"; //$NON-NLS-1$

    public static final String P_FIRST_LIST = "fl"; //$NON-NLS-1$

    public static final String P_UPGRADE_FROM = "uf"; //$NON-NLS-1$

    public static final String P_FIRST_LAUNCH = "fltime";  //$NON-NLS-1$

    public static final String P_LAST_POPOVER = "lpopover";  //$NON-NLS-1$

    private static final long MIN_POPOVER_TIME = 3 * 1000L;

    public static final String P_SUBTASKS_HELP = "sthelp"; //$NON-NLS-1$

    /** Set preference defaults, if unset. called at startup */
    public static void setPreferenceDefaults() {
        AstridPreferenceSpec spec;
        if (Constants.ASTRID_LITE)
            spec = new AstridLitePreferenceSpec();
        else
            spec = new AstridDefaultPreferenceSpec();

        spec.setIfUnset();

        setShowFriendsView();

        setShowFeaturedLists();

    }

    public static void resetToDefaults() {
        AstridPreferenceSpec spec;
        if (Constants.ASTRID_LITE)
            spec = new AstridLitePreferenceSpec();
        else
            spec = new AstridDefaultPreferenceSpec();

        spec.resetDefaults();
    }

    /**
     * Reset preferences based on archived AB tests
     * @param fromVersion
     */
    public static void resetPreferencesFromAbTests(long fromVersion) {
        Context context = ContextManager.getContext();
        if (fromVersion < UpgradeService.V4_4_2) {
            Preferences.clear(context.getString(R.string.p_calendar_reminders));
        }
    }

    private static void setShowFriendsView() {
        // Show friends view if necessary
        boolean showFriends = false;
        TodorooCursor<User> users = PluginServices.getUserDao().query(Query.select(User.ID).limit(1));
        try {
            showFriends = users.getCount() > 0;
        } finally {
            users.close();
        }
        Preferences.setBoolean(R.string.p_show_friends_view, showFriends);
    }

    private static void setShowFeaturedLists() {
        Preferences.setBoolean(FeaturedListFilterExposer.PREF_SHOULD_SHOW_FEATURED_LISTS, false);
    }

    /* ======================================================================
     * ========================================================= public prefs
     * ====================================================================== */

    /** Get publicly readable preferences */
    public static SharedPreferences getPublicPrefs(Context context) {
        context = context.getApplicationContext();
        return context.getSharedPreferences(AstridApiConstants.PUBLIC_PREFS,
                Context.MODE_WORLD_READABLE);
    }

    /* ======================================================================
     * ========================================================= system prefs
     * ====================================================================== */

	/** CurrentVersion: the currently installed version of Astrid */
    public static int getCurrentVersion() {
        return Preferences.getInt(P_CURRENT_VERSION, 0);
    }

    /** CurrentVersion: the currently installed version of Astrid */
    public static void setCurrentVersion(int version) {
        Preferences.setInt(P_CURRENT_VERSION, version);
    }

    /** The name (e.g. 4.0.1) of the currently installed version of astrid*/
    public static String getCurrentVersionName() {
        String versionName = Preferences.getStringValue(P_CURRENT_VERSION_NAME);
        if (versionName == null)
            versionName = "0"; //$NON-NLS-1$
        return versionName;
    }

    public static void setCurrentVersionName(String versionName) {
        Preferences.setString(P_CURRENT_VERSION_NAME, versionName);
    }

    /** If true, can show a help popover. If false, another one was recently shown */
    public static boolean canShowPopover() {
        long last = Preferences.getLong(P_LAST_POPOVER, 0);
        if(System.currentTimeMillis() - last < MIN_POPOVER_TIME)
            return false;
        Preferences.setLong(P_LAST_POPOVER, System.currentTimeMillis());
        return true;
    }

    public static boolean useTabletLayout(Context context) {
        return AndroidUtilities.isTabletSized(context) && !Preferences.getBoolean(R.string.p_force_phone_layout, false);
    }

}
