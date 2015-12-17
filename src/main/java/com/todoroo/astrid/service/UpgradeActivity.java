/**
 * Copyright (c) 2012 Todoroo Inc
 * <p/>
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.io.File;

import javax.inject.Inject;

public class UpgradeActivity extends InjectingAppCompatActivity {

    public static final String TOKEN_FROM_VERSION = "from_version"; //$NON-NLS-1$

    public static final String EXTRA_RESTART = "extra_restart";

    public static final int V4_8_0 = 380;
    public static final int V3_0_0 = 136;

    @Inject DialogBuilder dialogBuilder;
    @Inject Preferences preferences;

    private ProgressDialog dialog;
    private int from;
    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        from = getIntent().getIntExtra(TOKEN_FROM_VERSION, -1);
        if (from > 0) {
            dialog = dialogBuilder.newProgressDialog(R.string.DLG_upgrading);
            new Thread() {
                @Override
                public void run() {
                    boolean restartRequired = false;
                    try {
                        if (from < V4_8_0) {
                            performMarshmallowMigration();
                        }
                    } finally {
                        finished = true;
                        DialogUtilities.dismissDialog(UpgradeActivity.this, dialog);
                        sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                        Intent data = new Intent();
                        data.putExtra(EXTRA_RESTART, restartRequired);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                }
            }.start();
        } else {
            finished = true;
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow the back button to finish this activity before things are done
        if (finished) {
            super.onBackPressed();
        }
    }

    private void performMarshmallowMigration() {
        // preserve pre-marshmallow default backup location
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (!preferences.isStringValueSet(R.string.p_backup_dir)) {
                String directory = String.format("%s/astrid",
                        Environment.getExternalStorageDirectory());
                File file = new File(directory);
                if (file.exists() && file.isDirectory()) {
                    preferences.setString(R.string.p_backup_dir, directory);
                }
            }
        }
    }
}
