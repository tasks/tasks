/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;

import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Methods for working with GTasks preferences
 *
 * @author timsu
 *
 */
@Singleton
public class GtasksPreferenceService {

    private Context context;
    private final Preferences preferences;
    private PermissionChecker permissionChecker;

    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    private static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$
    private static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$

    @Inject
    public GtasksPreferenceService(@ForApplication Context context, Preferences preferences,
                                   PermissionChecker permissionChecker) {
        this.context = context;
        this.preferences = preferences;
        this.permissionChecker = permissionChecker;
    }

    public String getDefaultList() {
        return preferences.getStringValue(PREF_DEFAULT_LIST);
    }

    public void setDefaultList(String defaultList) {
        preferences.setString(PREF_DEFAULT_LIST, defaultList);
    }

    public String getUserName() {
        return preferences.getStringValue(PREF_USER_NAME);
    }

    public void setUserName(String userName) {
        preferences.setString(PREF_USER_NAME, userName);
    }

    protected static final String PREF_LAST_SYNC = "_last_sync"; //$NON-NLS-1$

    protected static final String PREF_ONGOING = "_ongoing"; //$NON-NLS-1$

    /**
     * @return true if we have a token for this user, false otherwise
     */
    public boolean isLoggedIn() {
        return context.getResources().getBoolean(R.bool.sync_enabled) &&
                preferences.getStringValue(PREF_USER_NAME) != null &&
                permissionChecker.canAccessAccounts();
    }

    /** @return Last Successful Sync Date, or 0 */
    public long getLastSyncDate() {
        return preferences.getLong(IDENTIFIER + PREF_LAST_SYNC, 0);
    }

    /** @return Last Error, or null if no last error */
    public boolean isOngoing() {
        return preferences.getBoolean(IDENTIFIER + PREF_ONGOING, false);
    }

    /** Deletes Last Successful Sync Date */
    public void clearLastSyncDate() {
        preferences.clear(IDENTIFIER + PREF_LAST_SYNC);
    }

    /** Set Ongoing */
    public void stopOngoing() {
        preferences.setBoolean(IDENTIFIER + PREF_ONGOING, false);
    }

    /** Set Last Successful Sync Date */
    public void recordSuccessfulSync() {
        preferences.setLong(IDENTIFIER + PREF_LAST_SYNC, DateUtilities.now() + 1000);
    }

    /** Set Last Attempted Sync Date */
    public void recordSyncStart() {
        preferences.setBoolean(IDENTIFIER + PREF_ONGOING, true);
    }
}
