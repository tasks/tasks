/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Chronometer;

import com.todoroo.astrid.voice.AACRecorder;
import com.todoroo.astrid.voice.AACRecorder.AACRecorderCallbacks;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AACRecordingActivity extends InjectingAppCompatActivity implements AACRecorderCallbacks {

    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$
    public static final String RESULT_FILENAME = "filename";  //$NON-NLS-1$

    private final AtomicReference<String> nameRef = new AtomicReference<>();
    private AACRecorder recorder;
    private String tempFile;

    @Inject Preferences preferences;
    @Inject PermissionRequestor permissionRequestor;

    @Bind(R.id.timer) Chronometer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (permissionRequestor.requestMic()) {
            startRecording();
        }
    }

    private void startRecording() {
        setContentView(R.layout.aac_record_activity);
        ButterKnife.bind(this);

        tempFile = preferences.getNewAudioAttachmentPath(nameRef);
        recorder = new AACRecorder();
        recorder.setListener(this);
        recorder.startRecording(tempFile);
        timer.start();
    }

    @OnClick(R.id.stop_recording)
    void stopRecording() {
        if (recorder != null) {
            recorder.stopRecording();
            timer.stop();
        }
    }

    @OnClick(R.id.dismiss)
    void dismiss() {
        if (recorder != null) {
            recorder.setListener(null);
            recorder.stopRecording();
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRecording();
    }

    @Override
    public void encodingFinished() {
        Intent result = new Intent();
        result.putExtra(RESULT_OUTFILE, tempFile);
        result.putExtra(RESULT_FILENAME, nameRef.get());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_MIC) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
