package com.todoroo.astrid.service;

import android.content.Context;
import android.content.pm.PackageManager;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.utility.Preferences;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AddonService {

    /** OEM preference key */
    private static final String PREF_OEM = "poem";

    /** Astrid Power Pack package */
    private static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /** cached is power pack value */
    private static Boolean isPowerPack = true;

    /** Checks whether power pack should be enabled */
    public static boolean isPowerPack() {
        if (isPowerPack != null)
            return isPowerPack;

        isPowerPack = false;
        if (Preferences.getBoolean(PREF_OEM, false))
            isPowerPack = true;
        else {
            try {
                Context context = ContextManager.getContext();
                context.getPackageManager().getApplicationInfo(
                        POWER_PACK_PACKAGE, 0);
                isPowerPack = true;
            } catch (PackageManager.NameNotFoundException e) {
                // not found
            }
        }
        return isPowerPack;
    }

    /**
     * Record that a version was an OEM install
     */
    public static void recordOem() {
        Preferences.setBoolean(PREF_OEM, true);
    }

}
