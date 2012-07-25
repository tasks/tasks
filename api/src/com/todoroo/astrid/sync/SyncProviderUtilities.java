/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;

/**
 * Sync Provider Utility class for accessing preferences
 */
abstract public class SyncProviderUtilities {

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

    protected static final String PREF_LAST_ERROR_TYPE = "_last_err_type"; //$NON-NLS-1$

    protected static final String PREF_ONGOING = "_ongoing"; //$NON-NLS-1$

    /** Get preferences object from the context */
    protected static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ContextManager.getContext());
    }

    /**
     * @return true if we should show sync toast when synchronizing
     */
    public boolean shouldShowToast() {
        return true;
    }

    /**
     * @return true if we have a token for this user, false otherwise
     */
    public boolean isLoggedIn() {
        return getPrefs().getString(getIdentifier() + PREF_TOKEN, null) != null;
    }

    /** authentication token, or null if doesn't exist */
    public String getToken() {
        return getPrefs().getString(getIdentifier() + PREF_TOKEN, null);
    }

    /** Returns something like "Logged in as: user@gmail.com" */
    abstract public String getLoggedInUserName();

    /** Sets the authentication token. Set to null to clear. */
    public void setToken(String setting) {
        Editor editor = getPrefs().edit();
        editor.putString(getIdentifier() + PREF_TOKEN, setting);
        editor.commit();
    }

    /** @return Last Successful Sync Date, or 0 */
    public long getLastSyncDate() {
        return getPrefs().getLong(getIdentifier() + PREF_LAST_SYNC, 0);
    }

    /** @return Last Attempted Sync Date, or 0 if it was successful */
    public long getLastAttemptedSyncDate() {
        return getPrefs().getLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, 0);
    }

    /** @return Last Error, or null if no last error */
    public String getLastError() {
        return getPrefs().getString(getIdentifier() + PREF_LAST_ERROR, null);
    }

    /** @return Last Error type, or null if no last error */
    public String getLastErrorType() {
        return getPrefs().getString(getIdentifier() + PREF_LAST_ERROR_TYPE, null);
    }

    /** @return Last Error, or null if no last error */
    public boolean isOngoing() {
        return getPrefs().getBoolean(getIdentifier() + PREF_ONGOING, false);
    }

    /** Deletes Last Successful Sync Date */
    public void clearLastSyncDate() {
        Editor editor = getPrefs().edit();
        editor.remove(getIdentifier() + PREF_LAST_SYNC);
        editor.commit();
    }

    /** Set Last Error */
    public void setLastError(String error, String type) {
        Editor editor = getPrefs().edit();
        editor.putString(getIdentifier() + PREF_LAST_ERROR, error);
        editor.putString(getIdentifier() + PREF_LAST_ERROR_TYPE, type);
        editor.commit();
    }

    /** Set Ongoing */
    public void stopOngoing() {
        Editor editor = getPrefs().edit();
        editor.putBoolean(getIdentifier() + PREF_ONGOING, false);
        editor.commit();
    }

    /** Set Last Successful Sync Date */
    public void recordSuccessfulSync() {
        Editor editor = getPrefs().edit();
        editor.putLong(getIdentifier() + PREF_LAST_SYNC, DateUtilities.now() + 1000);
        editor.putLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, 0);
        editor.commit();
    }

    /** Report last error if one exists */
    public void reportLastError() {
        String lastError = getLastError();
        if (!TextUtils.isEmpty(lastError)) {
            String type = getLastErrorType();
            reportLastErrorImpl(lastError, type);
        }
    }

    protected void reportLastErrorImpl(String lastError, String type) {
        // Subclasses can override if necessary
    }

    /** Set Last Attempted Sync Date */
    public void recordSyncStart() {
        Editor editor = getPrefs().edit();
        editor.putLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC,
                DateUtilities.now());
        editor.remove(getIdentifier() + PREF_LAST_ERROR);
        editor.remove(getIdentifier() + PREF_LAST_ERROR_TYPE);
        editor.putBoolean(getIdentifier() + PREF_ONGOING, true);
        editor.commit();
    }

    /**
     * Reads the frequency, in seconds, auto-sync should occur.
     *
     * @return seconds duration, or 0 if not desired
     */
    public int getSyncAutoSyncFrequency() {
        String value = getPrefs().getString(
                ContextManager.getContext().getString(
                        getSyncIntervalKey()), null);
        if (value == null)
            return 0;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
