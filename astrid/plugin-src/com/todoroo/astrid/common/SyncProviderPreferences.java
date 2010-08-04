package com.todoroo.astrid.common;

import java.util.Date;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.widget.TodorooPreferences;

/**
 * Utility class for common synchronization action: displaying synchronization
 * preferences and an action panel so users can initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com
 *
 */
abstract public class SyncProviderPreferences extends TodorooPreferences {

    // --- interface

    /**
     * @return your plugin identifier
     */
    abstract public String getIdentifier();

    /**
     * @return your preference resource
     */
    @Override
    abstract public int getPreferenceResource();

    /**
     * @return key for sync interval
     */
    abstract public int getSyncIntervalKey();

    /**
     * kick off synchronization
     */
    abstract public void startSync();

    /**
     * log user out
     */
    abstract public void logOut();

    // --- preference utility methods

    private static final String PREF_TOKEN = "_token"; //$NON-NLS-1$

    private static final String PREF_LAST_SYNC = "_last_sync"; //$NON-NLS-1$

    private static final String PREF_LAST_ATTEMPTED_SYNC = "_last_attempted"; //$NON-NLS-1$

    private static final String PREF_LAST_ERROR = "_last_error"; //$NON-NLS-1$

    private static final String PREF_ONGOING = "_ongoing"; //$NON-NLS-1$

    /** Get preferences object from the context */
    protected static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ContextManager.getContext());
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
        return getPrefs().getString(PREF_LAST_ERROR, null);
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

    /** Set Last Successful Sync Date */
    public void setLastError(String error) {
        Editor editor = getPrefs().edit();
        editor.putString(getIdentifier() + PREF_LAST_ERROR, error);
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
        editor.putLong(getIdentifier() + PREF_LAST_SYNC, DateUtilities.now());
        editor.putLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, 0);
        editor.commit();
    }

    /** Set Last Attempted Sync Date */
    public void recordSyncStart() {
        Editor editor = getPrefs().edit();
        editor.putLong(getIdentifier() + PREF_LAST_ATTEMPTED_SYNC, DateUtilities.now());
        editor.putString(getIdentifier() + PREF_LAST_ERROR, null);
        editor.putBoolean(getIdentifier() + PREF_ONGOING, true);
        editor.commit();
    }

    /**
     * Reads the frequency, in seconds, auto-sync should occur.
     *
     * @return seconds duration, or 0 if not desired
     */
    public static int getSyncAutoSyncFrequency() {
        String value = getPrefs().getString(
                ContextManager.getContext().getString(
                        R.string.rmilk_MPr_interval_key), null);
        if (value == null)
            return 0;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    // --- implementation

    @Autowired
    private DialogUtilities dialogUtilities;

    private int statusColor = Color.BLACK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setOnHierarchyChangeListener(new OnHierarchyChangeListener() {

            @Override
            public void onChildViewRemoved(View parent, View child) {
                //
            }

            @Override
            public void onChildViewAdded(View parent, View child) {
                View view = findViewById(R.id.status);
                if(view != null)
                    view.setBackgroundColor(statusColor);
            }
        });
    }

    /**
     *
     * @param resource
     *            if null, updates all resources
     */
    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        // interval
        if (r.getString(getSyncIntervalKey()).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(
                    r.getStringArray(R.array.rmilk_MPr_interval_values),
                    (String) value);
            if (index <= 0)
                preference.setSummary(R.string.rmilk_MPr_interval_desc_disabled);
            else
                preference.setSummary(r.getString(
                        R.string.rmilk_MPr_interval_desc,
                        r.getStringArray(R.array.rmilk_MPr_interval_entries)[index]));
        }

        // status
        else if (r.getString(R.string.rmilk_MPr_status_key).equals(preference.getKey())) {
            boolean loggedIn = isLoggedIn();
            String status;
            String subtitle = ""; //$NON-NLS-1$

            // ! logged in - display message, click -> sync
            if(!loggedIn) {
                status = r.getString(R.string.rmilk_status_loggedout);
                statusColor = Color.RED;
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        startSync();
                        finish();
                        return true;
                    }
                });
            }
            // sync is occurring
            else if(isOngoing()) {
                status = r.getString(R.string.rmilk_status_ongoing);
                statusColor = Color.rgb(0, 0, 100);
            }
            // last sync was error
            else if(getLastAttemptedSyncDate() != 0) {
                status = r.getString(R.string.rmilk_status_failed,
                        DateUtilities.getDateWithTimeFormat(SyncProviderPreferences.this).
                        format(new Date(getLastAttemptedSyncDate())));
                if(getLastSyncDate() > 0) {
                    subtitle = r.getString(R.string.rmilk_status_failed_subtitle,
                            DateUtilities.getDateWithTimeFormat(SyncProviderPreferences.this).
                            format(new Date(getLastSyncDate())));
                }
                statusColor = Color.rgb(100, 0, 0);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        String error = getLastError();
                        if(error != null)
                            dialogUtilities.okDialog(SyncProviderPreferences.this, error, null);
                        return true;
                    }
                });
            } else if(getLastSyncDate() > 0) {
                status = r.getString(R.string.rmilk_status_success,
                        DateUtilities.getDateWithTimeFormat(SyncProviderPreferences.this).
                        format(new Date(getLastSyncDate())));
                statusColor = Color.rgb(0, 100, 0);
            } else {
                status = r.getString(R.string.rmilk_status_never);
                statusColor = Color.rgb(0, 0, 100);
                preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference p) {
                        startSync();
                        finish();
                        return true;
                    }
                });
            }
            preference.setTitle(status);
            preference.setSummary(subtitle);

            View view = findViewById(R.id.status);
            if(view != null)
                view.setBackgroundColor(statusColor);
        }

        // sync button
        else if (r.getString(R.string.rmilk_MPr_sync_key).equals(preference.getKey())) {
            boolean loggedIn = isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    startSync();
                    finish();
                    return true;
                }
            });
            if(!loggedIn)
                preference.setTitle(R.string.rmilk_MPr_sync_log_in);
        }

        // log out button
        else if (r.getString(R.string.rmilk_MPr_forget_key).equals(preference.getKey())) {
            boolean loggedIn = isLoggedIn();
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    dialogUtilities.okCancelDialog(SyncProviderPreferences.this,
                            r.getString(R.string.rmilk_forget_confirm), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            logOut();
                            initializePreference(getPreferenceScreen());
                        }
                    }, null);
                    return true;
                }
            });
            if(!loggedIn)
                preference.setEnabled(false);
        }
    }

}