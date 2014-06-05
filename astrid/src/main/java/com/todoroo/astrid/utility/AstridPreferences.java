/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;


import android.content.Context;
import android.content.SharedPreferences;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.AstridApiConstants;

public class AstridPreferences {

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

    public static boolean useTabletLayout(Context context) {
        return AndroidUtilities.isTabletSized(context);
    }
}
