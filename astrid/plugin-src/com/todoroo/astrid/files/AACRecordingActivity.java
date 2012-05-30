package com.todoroo.astrid.files;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.aacenc.AACRecorder;
import com.todoroo.aacenc.AACRecorder.AACRecorderCallbacks;
import com.todoroo.aacenc.AACToM4A;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.service.ThemeService;

public class AACRecordingActivity extends Activity implements AACRecorderCallbacks {

    public static final String EXTRA_TEMP_FILE = "tempFile"; //$NON-NLS-1$
    public static final String EXTRA_TASK_ID = "taskId"; //$NON-NLS-1$
    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$

    private AACRecorder recorder;
    private String tempFile;
    private long taskId;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aac_record_activity);

        setupUi();

        tempFile = getIntent().getStringExtra(EXTRA_TEMP_FILE);
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0L);

        recorder = new AACRecorder();
        recorder.setListener(this);
        recorder.startRecording(tempFile);
    }

    private void setupUi() {
        View stopRecording = findViewById(R.id.stop_recording);
        stopRecording.setBackgroundColor(getResources().getColor(ThemeService.getThemeColor()));

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

        TextView speechBubble = (TextView) findViewById(R.id.reminder_message);
        speechBubble.setText(R.string.audio_speak_now);
    }

    private void stopRecording() {
        recorder.stopRecording();

        pd = DialogUtilities.progressDialog(this, getString(R.string.audio_encoding));
        pd.show();
    }

    @SuppressWarnings("nls")
    @Override
    public void encodingFinished() {
        try {
            StringBuilder filePathBuilder = new StringBuilder();
            filePathBuilder.append(getExternalFilesDir(FileMetadata.FILES_DIRECTORY).toString())
                    .append(File.separator)
                    .append(taskId)
                    .append("_")
                    .append(DateUtilities.now())
                    .append("_audio.m4a");

            String outFile = filePathBuilder.toString();
            new AACToM4A().convert(this, tempFile, outFile);

            Intent result = new Intent();
            result.putExtra(RESULT_OUTFILE, outFile);
            setResult(RESULT_OK, result);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.audio_err_encoding, Toast.LENGTH_LONG);
        }
        if (pd != null)
            pd.dismiss();
    }

}
