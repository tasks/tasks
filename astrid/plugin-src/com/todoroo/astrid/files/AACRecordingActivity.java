/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.aacenc.AACRecorder;
import com.todoroo.aacenc.AACRecorder.AACRecorderCallbacks;
import com.todoroo.aacenc.AACToM4A;
import com.todoroo.andlib.utility.DialogUtilities;

public class AACRecordingActivity extends Activity implements AACRecorderCallbacks {

    public static final String EXTRA_TEMP_FILE = "tempFile"; //$NON-NLS-1$
    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$
    public static final String RESULT_FILENAME = "filename";  //$NON-NLS-1$

    private AACRecorder recorder;
    private Chronometer timer;
    private String tempFile;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aac_record_activity);

        setupUi();

        tempFile = getIntent().getStringExtra(EXTRA_TEMP_FILE);

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

        pd = DialogUtilities.progressDialog(this, getString(R.string.audio_encoding));
        pd.show();
    }

    @Override
    public void encodingFinished() {
        try {

            AtomicReference<String> nameRef = new AtomicReference<String>();
            String outFile = FileUtilities.getNewAudioAttachmentPath(this, nameRef);

            new AACToM4A().convert(this, tempFile, outFile);

            Intent result = new Intent();
            result.putExtra(RESULT_OUTFILE, outFile);
            result.putExtra(RESULT_FILENAME, nameRef.get());
            setResult(RESULT_OK, result);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.audio_err_encoding, Toast.LENGTH_LONG).show();
        }
        if (pd != null)
            pd.dismiss();
    }

}
