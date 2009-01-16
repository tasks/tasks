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

public class SyncPreferences extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sync_preferences);

        getListView().addFooterView(getLayoutInflater().inflate(
                R.layout.sync_footer, getListView(), false));

        Button syncButton = ((Button)findViewById(R.id.sync));
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Constants.RESULT_SYNCHRONIZE);
                finish();
            }
        });

        ((Button)findViewById(R.id.forget)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtilities.okCancelDialog(SyncPreferences.this,
                        getResources().getString(R.string.sync_forget_confirm),
                        new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        Synchronizer.clearUserData(SyncPreferences.this);
                    }
                }, null);
            }
        });
    }
}