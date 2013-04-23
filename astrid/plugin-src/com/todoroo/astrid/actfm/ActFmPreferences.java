/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.billing.BillingActivity;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.PremiumUnlockService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProviderPreferences;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.Constants;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class ActFmPreferences extends SyncProviderPreferences {

    @Autowired ActFmPreferenceService actFmPreferenceService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    @Override
    public int getPreferenceResource() {
        return R.xml.preferences_actfm;
    }

    @Override
    public void startSync() {
        if (!actFmPreferenceService.isLoggedIn()) {
            if (gtasksPreferenceService.isLoggedIn()) {
                DialogUtilities.okCancelDialog(this, getString(R.string.DLG_warning), getString(R.string.actfm_dual_sync_warning),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startLogin();
                            }
                        }, null);
            } else {
                startLogin();
            }
        } else {
            setResult(RESULT_CODE_SYNCHRONIZE);
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen screen = getPreferenceScreen();
        Preference inAppBilling = findPreference(getString(R.string.actfm_inapp_billing));
        if (Constants.ASTRID_LITE || Preferences.getBoolean(PremiumUnlockService.PREF_KILL_SWITCH, false))
            screen.removePreference(inAppBilling);
        else
            inAppBilling.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    handleInAppBillingClicked();
                    return true;
                }
            });

        findPreference(getString(R.string.actfm_account_type)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startSync();
                return true;
            }
        });
    }

    private void startLogin() {
        Intent intent = new Intent(this, ActFmLoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    @Override
    public void logOut() {
        new ActFmSyncV2Provider().signOut(this);
    }

    @Override
    public SyncProviderUtilities getUtilities() {
        return actFmPreferenceService;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Preference premiumUpgrade = findPreference(getString(R.string.actfm_inapp_billing));
        if (premiumUpgrade != null &&
                (!Constants.MARKET_STRATEGY.billingSupported() || !actFmPreferenceService.isLoggedIn() || ActFmPreferenceService.isPremiumUser())) {
            getPreferenceScreen().removePreference(premiumUpgrade);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        new ActFmBackgroundService().scheduleService();
    }

    @Override
    public void updatePreferences(Preference preference, Object value) {
        final Resources r = getResources();

        boolean loggedIn = getUtilities().isLoggedIn();
        PreferenceCategory status = (PreferenceCategory) findPreference(r.getString(R.string.sync_SPr_group_status));

        if (loggedIn) {
            String title = actFmPreferenceService.getLoggedInUserName();
            String email = Preferences.getStringValue(ActFmPreferenceService.PREF_EMAIL);
            if (!TextUtils.isEmpty(email)) {
                if (!TextUtils.isEmpty(title))
                    title += "\n"; //$NON-NLS-1$
                title += email;
            }
            status.setTitle(getString(R.string.actfm_status_title_logged_in, title));
        }
        else
            status.setTitle(R.string.sync_SPr_group_status);

        if (r.getString(R.string.actfm_https_key).equals(preference.getKey())) {
            if ((Boolean)value)
                preference.setSummary(R.string.actfm_https_enabled);
            else
                preference.setSummary(R.string.actfm_https_disabled);
        } else if (r.getString(R.string.actfm_account_type).equals(preference.getKey())) {
            if (ActFmPreferenceService.isPremiumUser()) {
                // Premium user
                preference.setSummary(R.string.actfm_account_premium);
            } else if (actFmPreferenceService.isLoggedIn()) {
                // Non premium user
                preference.setSummary(R.string.actfm_account_basic);
            } else {
                // Not logged in
                preference.setEnabled(true);
                preference.setTitle(R.string.account_type_title_not_logged_in);
                preference.setSummary(R.string.account_type_summary_not_logged_in);
            }
        } else if (r.getString(R.string.sync_SPr_forget_key).equals(preference.getKey())) {
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference p) {
                    DialogUtilities.okCancelDialog(ActFmPreferences.this,
                            r.getString(R.string.sync_forget_confirm), new OnClickListener() {
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
            if(!loggedIn) {
                getPreferenceScreen().removePreference(preference);
            }

        } else {
            super.updatePreferences(preference, value);
        }
    }

    private void handleInAppBillingClicked() {
        if (ActFmPreferenceService.isPremiumUser()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + Constants.PACKAGE)); //$NON-NLS-1$
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.market_unavailable, Toast.LENGTH_LONG).show();
            }
        } else {
            Intent intent = new Intent(this, BillingActivity.class);
            startActivity(intent);
            StatisticsService.reportEvent(StatisticsConstants.PREMIUM_PAGE_VIEWED);
        }
    }

}
