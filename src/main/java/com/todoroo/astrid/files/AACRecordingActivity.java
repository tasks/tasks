/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;

import com.todoroo.astrid.voice.AACRecorder;
import com.todoroo.astrid.voice.AACRecorder.AACRecorderCallbacks;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;
import org.tasks.preferences.Preferences;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

public class AACRecordingActivity extends InjectingActivity implements AACRecorderCallbacks {

    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$
    public static final String RESULT_FILENAME = "filename";  //$NON-NLS-1$

    private AACRecorder recorder;
    private Chronometer timer;
    private AtomicReference<String> nameRef;
    private String tempFile;

    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aac_record_activity);

        setupUi();

        nameRef = new AtomicReference<>();
        tempFile = FileUtilities.getNewAudioAttachmentPath(preferences, this, nameRef);

        recorder = new AACRecorder();
        recorder.setListener(this);
        recorder.startRecording(tempFile);
        timer.start();
    }

    private void setupUi() {
        View stopRecording = findViewById(R.id.stop_recording);

        stopRecording.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        View dismiss = findViewById(R.id.dismiss);
        dismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.setListener(null);
                recorder.stopRecording();
                finish();
            }
        });

        timer = (Chronometer) findViewById(R.id.timer);
    }

    private void stopRecording() {
        recorder.stopRecording();
        timer.stop();
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
}
