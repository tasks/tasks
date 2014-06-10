/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.preferences.Preferences;

/**
 * Sync Provider Utility class for accessing preferences
 */
abstract public class SyncProviderUtilities {

    protected final Preferences preferences;

    public SyncProviderUtilities(Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * @return your plugin identifier
     */
    abstract public String getIdentifier();

    /**
     * @return key for sync interval
     */
    abstract public int getSyncIntervalKey();

    // --- implementation

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
