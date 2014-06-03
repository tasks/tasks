/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;


import android.content.Context;
import android.content.SharedPreferences;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.R;

public class AstridPreferences {

    private static final String P_CURRENT_VERSION = "cv"; //$NON-NLS-1$

    private static final String P_CURRENT_VERSION_NAME = "cvname"; //$NON-NLS-1$

    public static final String P_FIRST_LIST = "fl"; //$NON-NLS-1$

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

    public static void setCurrentVersionName(String versionName) {
        Preferences.setString(P_CURRENT_VERSION_NAME, versionName);
    }

    public static boolean useTabletLayout(Context context) {
        return AndroidUtilities.isTabletSized(context) && !Preferences.getBoolean(R.string.p_force_phone_layout, false);
    }
}
