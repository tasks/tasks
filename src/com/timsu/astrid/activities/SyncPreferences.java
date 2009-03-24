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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.Button;

import com.timsu.astrid.R;
import com.timsu.astrid.sync.Synchronizer;
import com.timsu.astrid.utilities.Constants;
import com.timsu.astrid.utilities.DialogUtilities;

/**
 * Displays synchronization preferences and an action panel so users can
 * initiate actions from the menu.
 *
 * @author timsu
 *
 */
public class SyncPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sync_preferences);

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
                    }
                }, null);
            }
        });
    }
}