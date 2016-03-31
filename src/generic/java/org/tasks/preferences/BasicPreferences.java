package org.tasks.preferences;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;

public class BasicPreferences extends BaseBasicPreferences {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPref(R.string.TLA_menu_donate).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://tasks.org/donate")));
                return false;
            }
        });

        requires(false, R.string.synchronization);
        requires(R.string.get_plugins, false, R.string.p_tesla_unread_enabled, R.string.p_purchased_tasker, R.string.p_purchased_dashclock);
        requires(R.string.privacy, false, R.string.p_collect_statistics);
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void initiateThemePurchase() {
        preferences.setBoolean(R.string.p_purchased_themes, true);
        recreate();
    }
}
