/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Context;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.utility.Constants;

/**
 * Astrid Service for managing add-ons
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AddOnService {

    /** Astrid Power Pack package */
    public static final String POWER_PACK_PACKAGE = "com.todoroo.astrid.ppack";

    /**
     * Check whether a given add-on is installed
     */
    public boolean isInstalled(AddOn addOn) {
        // it isnt installed if it is null...
        if (addOn == null) {
            return false;
        }
        return isInstalled(addOn.getPackageName(), addOn.isInternal());
    }

    /**
     * Check whether an external add-on is installed
     */
    public boolean isInstalled(String packageName) {
        return isInstalled(packageName, false);
    }

    /**
     * Check whether a given add-on is installed
     * @param internal whether to do api sig check
     */
    private boolean isInstalled(String packageName, boolean internal) {
        if(Constants.PACKAGE.equals(packageName)) {
            return true;
        }

        Context context = ContextManager.getContext();

        String packageSignature = AndroidUtilities.getSignature(context, packageName);
        if(packageSignature == null) {
            return false;
        }
        if(!internal) {
            return true;
        }

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
        if (title == null || packageName == null) {
            return null;
        }

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
        return new AddOn[0];
    }

}
