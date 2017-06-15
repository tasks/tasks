/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import org.tasks.dialogs.RecordAudioDialog;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPermissionRequestor;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.themes.Theme;

import javax.inject.Inject;

import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.dialogs.RecordAudioDialog.newRecordAudioDialog;

public class AACRecordingActivity extends InjectingAppCompatActivity implements RecordAudioDialog.RecordAudioDialogCallback {

    private static final String FRAG_TAG_RECORD_AUDIO = "frag_tag_record_audio";

    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$

    @Inject ActivityPermissionRequestor permissionRequestor;
    @Inject Theme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme.applyToContext(this);

        if (permissionRequestor.requestMic()) {
            showDialog();
        }
    }

    private void showDialog() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        RecordAudioDialog dialog = (RecordAudioDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_RECORD_AUDIO);
        if (dialog == null) {
            dialog = newRecordAudioDialog();
            dialog.show(supportFragmentManager, FRAG_TAG_RECORD_AUDIO);
        }
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_MIC) {
            if (verifyPermissions(grantResults)) {
                showDialog();
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void finished(String path) {
        Intent result = new Intent();
        result.putExtra(RESULT_OUTFILE, path);
        setResult(RESULT_OK, result);
        finish();
    }
}
