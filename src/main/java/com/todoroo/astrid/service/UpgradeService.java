/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.activity.AstridActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.TaskAttachment;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class UpgradeService {

    public static final int CURRENT = BuildConfig.VERSION_CODE;
    public static final int V4_8_0 = 376;
    public static final int V3_0_0 = 136;

    @Inject
    public UpgradeService() {
    }

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     */
    public void performUpgrade(final Activity context, final int from) {
        if(from < CURRENT) {
            Intent upgrade = new Intent(context, UpgradeActivity.class);
            upgrade.putExtra(UpgradeActivity.TOKEN_FROM_VERSION, from);
            context.startActivityForResult(upgrade, 0);
        }
    }

    public static class UpgradeActivity extends InjectingAppCompatActivity {
        private ProgressDialog dialog;

        public static final String TOKEN_FROM_VERSION = "from_version"; //$NON-NLS-1$
        private int from;
        private boolean finished = false;

        @Inject DialogBuilder dialogBuilder;
        @Inject Preferences preferences;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            from = getIntent().getIntExtra(TOKEN_FROM_VERSION, -1);
            if (from > 0) {
                dialog = dialogBuilder.newProgressDialog(R.string.DLG_upgrading);
                new Thread() {
                    @Override
                    public void run() {
                        //noinspection EmptyTryBlock
                        try {
                            if (from < V4_8_0) {
                                performMarshmallowMigration();
                            }
                        } finally {
                            finished = true;
                            DialogUtilities.dismissDialog(UpgradeActivity.this, dialog);
                            sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                            setResult(AstridActivity.RESULT_RESTART_ACTIVITY);
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
            // preserve pre-marshmallow default attachment and backup locations
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (!preferences.isStringValueSet(R.string.p_backup_dir)) {
                    String directory = String.format("%s/astrid",
                            Environment.getExternalStorageDirectory());
                    File file = new File(directory);
                    if (file.exists() && file.isDirectory()) {
                        preferences.setString(R.string.p_backup_dir, directory);
                    }
                }

                if (!preferences.isStringValueSet(R.string.p_attachment_dir)) {
                    String directory = String.format("%s/Android/data/%s/files/%s",
                            Environment.getExternalStorageDirectory(),
                            BuildConfig.APPLICATION_ID,
                            TaskAttachment.FILES_DIRECTORY_DEFAULT);
                    File file = new File(directory);
                    if (file.exists() && file.isDirectory()) {
                        preferences.setString(R.string.p_attachment_dir, directory);
                    }
                }
            }
        }
    }
}
