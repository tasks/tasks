package com.todoroo.astrid.files;

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
import com.todoroo.aacenc.AACToM4A;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.service.ThemeService;

public class AACRecordingActivity extends Activity {

    public static final String EXTRA_TEMP_FILE = "tempFile"; //$NON-NLS-1$
    public static final String EXTRA_TASK_ID = "taskId"; //$NON-NLS-1$
    public static final String RESULT_OUTFILE = "outfile"; //$NON-NLS-1$

    private AACRecorder recorder;
    private String tempFile;
    private long taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.aac_record_activity);

        setupUi();

        tempFile = getIntent().getStringExtra(EXTRA_TEMP_FILE);
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0L);

        recorder = new AACRecorder(this);
        recorder.startRecording(tempFile);
    }

    private void setupUi() {
        View stopRecording = findViewById(R.id.stop_recording);
        stopRecording.setBackgroundColor(getResources().getColor(ThemeService.getThemeColor()));

        stopRecording.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.err.println("On click");
                stopRecording();
            }
        });

        View dismiss = findViewById(R.id.dismiss);
        dismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stopRecording();
                finish();
            }
        });

        TextView speechBubble = (TextView) findViewById(R.id.reminder_message);
        speechBubble.setText(R.string.audio_speak_now);
    }

    @SuppressWarnings("nls")
    private void stopRecording() {
        System.err.println("Stopping...");
        recorder.stopRecording();
        System.err.println("Stopped recorder");

        ProgressDialog pd = DialogUtilities.progressDialog(this, "Encoding...");
        pd.show();
        System.err.println("Passed pd");
        try {
            StringBuilder filePathBuilder = new StringBuilder();
            filePathBuilder.append(getExternalFilesDir("audio").toString())
                    .append("/")
                    .append(taskId)
                    .append("_")
                    .append(DateUtilities.now())
                    .append("_audio.mp4");

            String outFile = filePathBuilder.toString();
            System.err.println("Converting");
            new AACToM4A().convert(this, tempFile, outFile);
            System.err.println("Finished Converting");

            Intent result = new Intent();
            result.putExtra(RESULT_OUTFILE, outFile);
            setResult(RESULT_OK, result);
            System.err.println("Finishing");
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error :(", Toast.LENGTH_LONG);
        }
        pd.dismiss();
    }


}
