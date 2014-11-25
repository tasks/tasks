/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.ViewGroup;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.gtasks.sync.GtasksSyncV2Provider;

import org.tasks.R;
import org.tasks.injection.InjectingSyncProviderPreferences;

import java.util.HashMap;
import java.util.Set;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksPreferences extends InjectingSyncProviderPreferences {

    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksSyncV2Provider gtasksSyncV2Provider;
    @Inject GtasksScheduler gtasksScheduler;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_gtasks;
    }

    private void startSync() {
        if (!gtasksPreferenceService.isLoggedIn()) {
            startLogin();
        } else {
            syncOrImport();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            syncOrImport();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void syncOrImport() {
        setResultForSynchronize();
    }

    private void setResultForSynchronize() {
        setResult(RESULT_CODE_SYNCHRONIZE);
        finish();
    }

    private void startLogin() {
        Intent intent = new Intent(this, GtasksLoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    public void logOut() {
        gtasksSyncV2Provider.signOut();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gtasksScheduler.scheduleService();
    }

    public static final int RESULT_CODE_SYNCHRONIZE = 2;

    protected static final int REQUEST_LOGIN = 0;

    // --- implementation

    private int statusColor = Color.BLACK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {

            @Override
            public void onChildViewRemoved(View parent, View child) {
                //
            }

            @Override
            public void onChildViewAdded(View parent, View child) {
                View view = findViewById(org.tasks.R.id.status);
                if(view != null) {
                    view.setBackgroundColor(statusColor);
                }
            }
        });
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        // interval
        if (r.getString(gtasksPreferenceService.getSyncIntervalKey()).equals(
                preference.getKey())) {
            int index = AndroidUtilities.indexOf(
                    r.getStringArray(org.tasks.R.array.sync_SPr_interval_values),
                    value);
            if (index <= 0) {
                preference.setSummary(org.tasks.R.string.sync_SPr_interval_desc_disabled);
            } else {
                preference.setSummary(r.getString(
                        org.tasks.R.string.sync_SPr_interval_desc,
                        r.getStringArray(org.tasks.R.array.sync_SPr_interval_entries)[index]));
            }
        }

        // status
        else if (r.getString(org.tasks.R.string.sync_SPr_status_key).equals(preference.getKey())) {
            boolean loggedIn = gtasksPreferenceService.isLoggedIn();
            String status;
            //String subtitle = ""; //$NON-NLS-1$

            // ! logged in - display message, click -> sync
            if(!loggedIn) {
                status = r.getString(org.tasks.R.string.sync_status_loggedout);
                statusColor = Color.rgb(19, 132, 165);
            }
            // sync is occurring
            else if(gtasksPreferenceService.isOngoing()) {
                status = r.getString(org.tasks.R.string.sync_status_ongoing);
                statusColor = Color.rgb(0, 0, 100);
            }
            // last sync had errors
            else if(gtasksPreferenceService.getLastError() != null || gtasksPreferenceService.getLastAttemptedSyncDate() != 0) {
                // last sync was failure
                if(gtasksPreferenceService.getLastAttemptedSyncDate() != 0) {
                    status = r.getString(org.tasks.R.string.sync_status_failed,
                            DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                                    newDate(gtasksPreferenceService.getLastAttemptedSyncDate())));
                    statusColor = Color.rgb(100, 0, 0);
                } else {
                    long lastSyncDate = gtasksPreferenceService.getLastSyncDate();
                    String dateString = lastSyncDate > 0 ?
                            DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                                    newDate(lastSyncDate)) : ""; //$NON-NLS-1$
                    status = r.getString(org.tasks.R.string.sync_status_errors, dateString);
                    statusColor = Color.rgb(100, 100, 0);
                }
            }
            else if(gtasksPreferenceService.getLastSyncDate() > 0) {
                status = r.getString(org.tasks.R.string.sync_status_success,
                        DateUtilities.getDateStringWithTime(GtasksPreferences.this,
                                newDate(gtasksPreferenceService.getLastSyncDate())));
                statusColor = Color.rgb(0, 100, 0);
            } else {
                status = r.getString(org.tasks.R.string.sync_status_never);
                statusColor = Color.rgb(0, 0, 100);
            }
            preference.setTitle(org.tasks.R.string.sync_SPr_sync);
            preference.setSummary(r.getString(org.tasks.R.string.sync_SPr_status_subtitle, status));

            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    startSync();
                    return true;
                }
            });

            View view = findViewById(org.tasks.R.id.status);
            if(view != null) {
                view.setBackgroundColor(statusColor);
            }
        }
        else if (r.getString(org.tasks.R.string.sync_SPr_key_last_error).equals(preference.getKey())) {
            if (gtasksPreferenceService.getLastError() != null) {
                // Display error
                final String service = getTitle().toString();
                final String lastErrorFull = gtasksPreferenceService.getLastError();
                final String lastErrorDisplay = adjustErrorForDisplay(r, lastErrorFull, service);
                preference.setTitle(org.tasks.R.string.sync_SPr_last_error);
                preference.setSummary(org.tasks.R.string.sync_SPr_last_error_subtitle);

                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference pref) {
                        // Show last error
                        new AlertDialog.Builder(GtasksPreferences.this)
                                .setTitle(org.tasks.R.string.sync_SPr_last_error)
                                .setMessage(lastErrorDisplay)
                                .setPositiveButton(org.tasks.R.string.sync_SPr_send_report, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                        emailIntent.setType("plain/text")
                                                .putExtra(Intent.EXTRA_EMAIL, new String[] { "baker.alex@gmail.com"} )
                                                .putExtra(Intent.EXTRA_SUBJECT, service + " Sync Error")
                                                .putExtra(Intent.EXTRA_TEXT, lastErrorFull);
                                        startActivity(Intent.createChooser(emailIntent, r.getString(org.tasks.R.string.sync_SPr_send_report)));
                                    }
                                })
                                .setNegativeButton(org.tasks.R.string.DLG_close, null)
                                .create().show();
                        return true;
                    }
                });

            } else {
                PreferenceCategory statusCategory = (PreferenceCategory) findPreference(r.getString(org.tasks.R.string.sync_SPr_group_status));
                statusCategory.removePreference(findPreference(r.getString(org.tasks.R.string.sync_SPr_key_last_error)));
            }
        }
        // log out button
        else if (r.getString(org.tasks.R.string.sync_SPr_forget_key).equals(preference.getKey())) {
            boolean loggedIn = gtasksPreferenceService.isLoggedIn();
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    DialogUtilities.okCancelDialog(GtasksPreferences.this,
                            r.getString(org.tasks.R.string.sync_forget_confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    logOut();
                                    initializePreference(getPreferenceScreen());
                                }
                            }, null
                    );
                    return true;
                }
            });
            if(!loggedIn) {
                PreferenceCategory category = (PreferenceCategory) findPreference(r.getString(org.tasks.R.string.sync_SPr_key_options));
                category.removePreference(preference);
            }

        }
    }

    /**
     * We can define exception strings in this map that we want to replace with more user-friendly
     * messages. As we discover new exception types, we can expand the map.
     *
     * NOTE: All resources are currently required to have a single string format argument
     * for inserting the service name into the error message
     */
    private static HashMap<String, Integer> exceptionsToDisplayMessages;

    private static HashMap<String, Integer> getExceptionMap() {
        if (exceptionsToDisplayMessages == null) {
            exceptionsToDisplayMessages = new HashMap<>();
            exceptionsToDisplayMessages.put("java.net.ConnectionException", org.tasks.R.string.sync_error_offline);
            exceptionsToDisplayMessages.put("java.net.UnknownHostException", org.tasks.R.string.sync_error_offline);
        }
        return exceptionsToDisplayMessages;
    }

    private static String adjustErrorForDisplay(Resources r, String lastError, String service) {
        Set<String> exceptions = getExceptionMap().keySet();
        Integer resource = null;
        for (String key : exceptions) {
            if (lastError.contains(key)) {
                resource = getExceptionMap().get(key);
                break;
            }
        }
        if (resource == null) {
            return lastError;
        }
        return r.getString(resource, service);
    }
}
