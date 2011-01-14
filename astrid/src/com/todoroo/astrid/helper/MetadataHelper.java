package com.todoroo.astrid.helper;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * @author joshuagross
 */
public class MetadataHelper {
    public static String resolveActivityCategoryName (ResolveInfo resolveInfo, PackageManager pm) {
        // category - either from metadata, or the application name
        String category = null;
        String categoryKey = "category";
        if (resolveInfo.activityInfo.metaData != null && resolveInfo.activityInfo.metaData.containsKey(categoryKey)) {
            int resource = resolveInfo.activityInfo.metaData.getInt(
                    categoryKey, -1);
            if (resource > -1) {
                // category stored as integer in Manifest
                try {
                    category = pm.getResourcesForApplication(
                            resolveInfo.activityInfo.applicationInfo).getString(
                            resource);
                } catch (Exception e) {
                    //
                }
            } else {
                // category stored as String in Manifest
                category = resolveInfo.activityInfo.metaData.getString(categoryKey);
            }
        }
        // If category is null at this point, we use the name of the application this activity is found in
        if (category == null) {
            category = resolveInfo.activityInfo.applicationInfo.loadLabel(pm).toString();
        }

        return category;
    }
}
