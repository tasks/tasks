/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class SyncPreferences extends PreferenceActivity {

    /** whether or not to synchronize with RTM */
	private boolean oldRtmSyncPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources r = getResources();

        oldRtmSyncPreference = Preferences.shouldSyncRTM(this);

        addPreferencesFromResource(R.xml.sync_preferences);

        // set up preferences
        findPreference(getString(R.string.p_sync_interval)).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(Preferences.getSyncRTMToken(SyncPreferences.this) == null)
                    setResult(Constants.RESULT_SYNCHRONIZE);
                return true;
            }
        });

        // set up footer
        getListView().addFooterView(getLayoutInflater().inflate(
                R.layout.sync_footer, getListView(), false));

        Button syncButton = ((Button)findViewById(R.id.sync));
        syncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setResult(Constants.RESULT_SYNCHRONIZE);
                finish();
            }
        });

        ((Button)findViewById(R.id.forget)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DialogUtilities.okCancelDialog(SyncPreferences.this,
                        getResources().getString(R.string.sync_forget_confirm),
                        new Dialog.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int which) {
                        Synchronizer.clearUserData(SyncPreferences.this);
                        // force a synchronization if sync preference is still set
                        oldRtmSyncPreference = false;
                    }
                }, null);
            }
        });

        // set up labels
        TextView lastSyncLabel = (TextView)findViewById(R.id.last_sync_label);
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm");
        String syncDate = r.getString(R.string.sync_date_never);
        Date lastSyncDate = Preferences.getSyncLastSync(this);
        if(lastSyncDate != null)
        	syncDate = formatter.format(lastSyncDate);
        lastSyncLabel.setText(r.getString(R.string.sync_last_sync, syncDate));

        syncDate = null;
        TextView lastAutoSyncLabel = (TextView)findViewById(R.id.last_auto_sync_label);
        Date lastAutoSyncDate = Preferences.getSyncLastSyncAttempt(this);
        if(lastAutoSyncDate != null && (lastSyncDate == null ||
        		(lastAutoSyncDate.getTime() > lastSyncDate.getTime())))
        	syncDate = formatter.format(lastAutoSyncDate);
        if(syncDate != null)
        	lastAutoSyncLabel.setText(r.getString(R.string.sync_last_auto_sync, syncDate));
        else
        	lastAutoSyncLabel.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // set up flurry
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		boolean newRtmSyncPreference = Preferences.shouldSyncRTM(this);
    		if(newRtmSyncPreference != oldRtmSyncPreference && newRtmSyncPreference) {
    			setResult(Constants.RESULT_SYNCHRONIZE);
    		}
    		finish();
    		return true;
    	}
    	return false;
    }
}