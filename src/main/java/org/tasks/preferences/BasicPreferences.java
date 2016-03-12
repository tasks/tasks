package org.tasks.preferences;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.billing.IabHelper;
import org.tasks.billing.IabResult;
import org.tasks.billing.Purchase;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.receivers.TeslaUnreadReceiver;

import javax.inject.Inject;

public class BasicPreferences extends InjectingPreferenceActivity implements IabHelper.OnIabPurchaseFinishedListener {

    private static final String EXTRA_RESULT = "extra_result";
    private static final int RC_PREFS = 10001;
    private static final int REQUEST_PURCHASE_TESLA_UNREAD = 10002;
    private static final int REQUEST_PURCHASE_TASKER = 10003;

    private Bundle result;

    @Inject Tracker tracker;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;
    @Inject Preferences preferences;
    @Inject IabHelper iabHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        addPreferencesFromResource(R.xml.preferences);
        if (!getResources().getBoolean(R.bool.sync_supported)) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.synchronization)));
        }
        if (getResources().getBoolean(R.bool.google_play_store_available)) {
            addPreferencesFromResource(R.xml.preferences_addons);
            addPreferencesFromResource(R.xml.preferences_privacy);

            findPreference(getString(R.string.p_tesla_unread_enabled)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null) {
                        if ((boolean) newValue && !preferences.getBoolean(R.string.p_purchased_tesla_unread, BuildConfig.DEBUG)) {
                            iabHelper.launchPurchaseFlow(BasicPreferences.this, getString(R.string.sku_tesla_unread), REQUEST_PURCHASE_TESLA_UNREAD, BasicPreferences.this);
                        } else {
                            teslaUnreadReceiver.setEnabled((boolean) newValue);
                            return true;
                        }
                    }
                    return false;
                }
            });

            findPreference(getString(R.string.p_tasker_enabled)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null) {
                        if ((boolean) newValue && !preferences.getBoolean(R.string.p_purchased_tasker, BuildConfig.DEBUG)) {
                            iabHelper.launchPurchaseFlow(BasicPreferences.this, getString(R.string.sku_tasker), REQUEST_PURCHASE_TASKER, BasicPreferences.this);
                        } else {
                            toggleTasker(BasicPreferences.this, (boolean) newValue);
                            return true;
                        }
                    }
                    return false;
                }
            });

            findPreference(getString(R.string.p_collect_statistics)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null) {
                        tracker.setTrackingEnabled((boolean) newValue);
                        return true;
                    }
                    return false;
                }
            });
        }
        setupActivity(R.string.EPr_appearance_header, AppearancePreferences.class);
        setupActivity(R.string.notifications, ReminderPreferences.class);
        setupActivity(R.string.EPr_manage_header, OldTaskPreferences.class);
    }

    public static void toggleTasker(Context context, boolean enabled) {
        ComponentName componentName = new ComponentName(context, "com.twofortyfouram.locale.example.setting.toast.ui.activity.PluginActivity");
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
    }

    private void setupActivity(int key, final Class<?> target) {
        findPreference(getString(key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(BasicPreferences.this, target), RC_PREFS);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(EXTRA_RESULT, result);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PREFS) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                result.putAll(data.getExtras());
                setResult(Activity.RESULT_OK, new Intent() {{
                    putExtras(result);
                }});
            }
        } else if (requestCode == REQUEST_PURCHASE_TESLA_UNREAD || requestCode == REQUEST_PURCHASE_TASKER) {
            iabHelper.handleActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, final Purchase info) {
        if (result.isSuccess()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (info.getSku().equals(getString(R.string.sku_tasker))) {
                        preferences.setBoolean(R.string.p_purchased_tasker, true);
                        findPreference(getString(R.string.p_tasker_enabled)).setEnabled(true);
                    } else if (info.getSku().equals(getString(R.string.sku_tesla_unread))) {
                        preferences.setBoolean(R.string.p_purchased_tesla_unread, true);
                        findPreference(getString(R.string.p_tesla_unread_enabled)).setEnabled(true);
                    }
                }
            });
        }
    }
}
