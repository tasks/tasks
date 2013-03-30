/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.utility.Constants;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AddOnService {

    /** OEM preference key */
    private static final String PREF_OEM = "poem";

    /** Astrid Power Pack package */
    public static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /** Astrid Locale package */
    public static final String LOCALE_PACKAGE = "com.todoroo.astrid.locale";

    /** Astrid Power Pack label */
    public static final String POWER_PACK_LABEL = "Astrid Power Pack";

    /** Checks whether power pack should be enabled */
    public boolean hasPowerPack() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(POWER_PACK_PACKAGE, true))
            return true;
        return false;
    }

    /** Checks whether locale plugin should be enabled */
    public boolean hasLocalePlugin() {
        if (Preferences.getBoolean(PREF_OEM, false))
            return true;
        else if(isInstalled(LOCALE_PACKAGE, true))
            return true;
        return false;
    }

    /**
     * Record that a version was an OEM install
     */
    public static void recordOem() {
        Preferences.setBoolean(PREF_OEM, true);
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @return
     */
    public boolean isInstalled(AddOn addOn) {
        // it isnt installed if it is null...
        if (addOn == null)
            return false;
        return isInstalled(addOn.getPackageName(), addOn.isInternal());
    }

    /**
     * Check whether an external add-on is installed
     * @param packageName
     * @return
     */
    public boolean isInstalled(String packageName) {
        return isInstalled(packageName, false);
    }

    /**
     * Check whether a given add-on is installed
     * @param addOn
     * @param internal whether to do api sig check
     * @return
     */
    private boolean isInstalled(String packageName, boolean internal) {
        if(Constants.PACKAGE.equals(packageName))
            return true;

        Context context = ContextManager.getContext();

        String packageSignature = AndroidUtilities.getSignature(context, packageName);
        if(packageSignature == null)
            return false;
        if(!internal)
            return true;

        String astridSignature = AndroidUtilities.getSignature(context, Constants.PACKAGE);
        return packageSignature.equals(astridSignature);
    }

    /**
     * Get one AddOn-descriptor by packageName and title.
     *
     * @param packageName could be Constants.PACKAGE or one of AddOnService-constants
     * @param title the descriptive title, as in "Astrid Power Pack"
     * @return the addon-descriptor, if it is available (registered here in getAddOns), otherwise null
     */
    public AddOn getAddOn(String packageName, String title) {
        if (title == null || packageName == null)
            return null;

        AddOn addon = null;
        AddOn[] addons = getAddOns();
        for (int i = 0; i < addons.length ; i++) {
            if (packageName.equals(addons[i].getPackageName()) && title.equals(addons[i].getTitle())) {
                addon = addons[i];
            }
        }
        return addon;
    }

    /**
     * Get a list of add-ons
     *
     * @return available add-ons
     */
    public AddOn[] getAddOns() {
        Resources r = ContextManager.getContext().getResources();

        // temporary temporary
        ArrayList<AddOn> list = new ArrayList<AddOn>(3);
        if(Constants.MARKET_STRATEGY.includesPowerPack())
            list.add(new AddOn(false, true, r.getString(R.string.AOA_ppack_title), null,
                r.getString(R.string.AOA_ppack_description),
                POWER_PACK_PACKAGE,
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_pp)).getBitmap()));

        if(Constants.MARKET_STRATEGY.includesLocalePlugin())
            list.add(new AddOn(false, true, r.getString(R.string.AOA_locale_title), null,
                r.getString(R.string.AOA_locale_description),
                LOCALE_PACKAGE,
                ((BitmapDrawable)r.getDrawable(R.drawable.icon_locale)).getBitmap()));

        return list.toArray(new AddOn[list.size()]);
    }

}
