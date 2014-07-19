/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.R;
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

    private final Preferences preferences;

    public static final String IDENTIFIER = "gtasks"; //$NON-NLS-1$

    private static final String PREF_DEFAULT_LIST = IDENTIFIER + "_defaultlist"; //$NON-NLS-1$
    private static final String PREF_USER_NAME = IDENTIFIER + "_user"; //$NON-NLS-1$

    @Inject
    public GtasksPreferenceService(Preferences preferences) {
        this.preferences = preferences;
    }

    public String getIdentifier() {
        return IDENTIFIER;
    }

    public int getSyncIntervalKey() {
        return R.string.gtasks_GPr_interval_key;
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

    protected static final String PREF_TOKEN = "_token"; //$NON-NLS-1$

    protected static final String PREF_LAST_SYNC = "_last_sync"; //$NON-NLS-1$

    protected static final String PREF_LAST_ATTEMPTED_SYNC = "_last_attempted"; //$NON-NLS-1$

    protected static final String PREF_LAST_ERROR = "_last_err"; //$NON-NLS-1$

    protected static final String PREF_ONGOING = "_ongoing"; //$NON-NLS-1$

    /**
     * @return true if we have a token for this user, false otherwise
     */
    public boolean isLoggedIn() {
        return preferences.getStringValue(getIdentifier() + PREF_TOKEN) != null;
    }

    /** authentication token, or null if doesn't exist */
    public String getToken() {
        return preferences.getStringValue(getIdentifier() + PREF_TOKEN);
    }

    /** Sets the authentication token. Set to null to clear. */
    public void setToken(String setting) {
        preferences.setString(getIdentifier() + PREF_TOKEN, setting);
    }

    /** @return Last Successful Sync Date, or 0 */
    public long getLastSyncDate() {
        return preferences.getLong(getIdentifier() + PREF_LAST_SYNC, 0);
    }

    /** @return Last Attempted Sync Date, or 0 if it was successful */
    public long getLastAttemptedSyncDate() {
        return preferences.getLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, 0);
    }

    /** @return Last Error, or null if no last error */
    public String getLastError() {
        return preferences.getStringValue(getIdentifier() + PREF_LAST_ERROR);
    }

    /** @return Last Error, or null if no last error */
    public boolean isOngoing() {
        return preferences.getBoolean(getIdentifier() + PREF_ONGOING, false);
    }

    /** Deletes Last Successful Sync Date */
    public void clearLastSyncDate() {
        preferences.clear(getIdentifier() + PREF_LAST_SYNC);
    }

    /** Set Last Error */
    public void setLastError(String error) {
        preferences.setString(getIdentifier() + PREF_LAST_ERROR, error);
    }

    /** Set Ongoing */
    public void stopOngoing() {
        preferences.setBoolean(getIdentifier() + PREF_ONGOING, false);
    }

    /** Set Last Successful Sync Date */
    public void recordSuccessfulSync() {
        preferences.setLong(getIdentifier() + PREF_LAST_SYNC, DateUtilities.now() + 1000);
        preferences.setLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, 0);
    }

    /** Set Last Attempted Sync Date */
    public void recordSyncStart() {
        preferences.setLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, DateUtilities.now());
        preferences.clear(getIdentifier() + PREF_LAST_ERROR);
        preferences.setBoolean(getIdentifier() + PREF_ONGOING, true);
    }
}
