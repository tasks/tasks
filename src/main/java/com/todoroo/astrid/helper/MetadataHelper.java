/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joshuagross
 */
public class MetadataHelper {

    private static final Logger log = LoggerFactory.getLogger(MetadataHelper.class);

    private static final String CATEGORY_KEY = "category"; //$NON-NLS-1$

    public static String resolveActivityCategoryName (ResolveInfo resolveInfo, PackageManager pm) {
        // category - either from metadata, or the application name
        String category = null;
        if (resolveInfo.activityInfo.metaData != null && resolveInfo.activityInfo.metaData.containsKey(CATEGORY_KEY)) {
            int resource = resolveInfo.activityInfo.metaData.getInt(
                    CATEGORY_KEY, -1);
            if (resource > -1) {
                // category stored as integer in Manifest
                try {
                    category = pm.getResourcesForApplication(
                            resolveInfo.activityInfo.applicationInfo).getString(
                            resource);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                // category stored as String in Manifest
                category = resolveInfo.activityInfo.metaData.getString(CATEGORY_KEY);
            }
        }
        // If category is null at this point, we use the name of the application this activity is found in
        if (category == null) {
            category = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();
        }

        return category;
    }
}
