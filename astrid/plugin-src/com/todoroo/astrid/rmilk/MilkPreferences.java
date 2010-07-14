package com.todoroo.astrid.rmilk;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.ListView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.rmilk.sync.RTMSyncProvider;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class MilkPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_rmilk);

        PreferenceScreen screen = getPreferenceScreen();
        initializePreference(screen);

        // status
        final String status = "Please Log In To RTM!"; //$NON-NLS-1$
        final int statusColor = Color.RED;

        Resources r = getResources();
        Preference preference = screen.findPreference(r.getString(R.string.rmilk_MPr_status_key));
        preference.setTitle(status);

        getListView().setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            public void onChildViewRemoved(View arg0, View arg1) {
                //
            }
            public void onChildViewAdded(View parent, View child) {
                if(((ListView)parent).getChildCount() == 2) {
                    child.setBackgroundColor(statusColor);
                }
            }
        });

        // action buttons
        Preference syncAction = screen.getPreferenceManager().findPreference(
                getString(R.string.rmilk_MPr_sync_key));
        syncAction.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                new RTMSyncProvider().synchronize(MilkPreferences.this);
                return true;
            }
        });
        /*Preference clearDataPreference = screen.getPreferenceManager().findPreference(
                getString(R.string.rmilk_MPr_forget_key));*/
    }

    private void initializePreference(Preference preference) {
        if(preference instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup)preference;
            for(int i = 0; i < group.getPreferenceCount(); i++) {
                initializePreference(group.getPreference(i));
            }
        } else {
            Object value = null;
            if(preference instanceof ListPreference)
                value = ((ListPreference)preference).getValue();
            else if(preference instanceof CheckBoxPreference)
                value = ((CheckBoxPreference)preference).isChecked();
            else if(preference instanceof EditTextPreference)
                value = ((EditTextPreference)preference).getText();
            else
                return;

            updatePreferences(preference, value);

            preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference myPreference, Object newValue) {
                    return updatePreferences(myPreference, newValue);
                }
            });
        }
    }

    /**
     *
     * @param resource if null, updates all resources
     */
    protected boolean updatePreferences(Preference preference, Object value) {
        Resources r = getResources();

        // interval
        if(r.getString(R.string.rmilk_MPr_interval_key).equals(preference.getKey())) {
            int index = AndroidUtilities.indexOf(r.getStringArray(R.array.rmilk_MPr_interval_values), (String)value);
            if(index <= 0)
                preference.setSummary(R.string.rmilk_MPr_interval_desc_disabled);
            else
                preference.setSummary(r.getString(R.string.rmilk_MPr_interval_desc,
                        r.getStringArray(R.array.rmilk_MPr_interval_entries)[index]));
        }

        // shortcut
        else if(r.getString(R.string.rmilk_MPr_shortcut_key).equals(preference.getKey())) {
            if((Boolean)value) {
                preference.setSummary(R.string.rmilk_MPr_shortcut_desc_enabled);
            } else {
                preference.setSummary(R.string.rmilk_MPr_shortcut_desc_disabled);
            }
        }

        return true;
    }
}