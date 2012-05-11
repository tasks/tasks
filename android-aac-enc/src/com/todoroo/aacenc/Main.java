package com.todoroo.aacenc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements RecognitionListener {

    private String AAC_FILE;
    private String M4A_FILE = "/sdcard/audio.m4a";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        File dir = getFilesDir();
        AAC_FILE = dir.toString() + "/audio.aac";


        findViewById(R.id.write).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                write();
            }
        });

        findViewById(R.id.play).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        sr = SpeechRecognizer.createSpeechRecognizer(this);
    }

    private void play() {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(M4A_FILE);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Toast.makeText(Main.this, "Playing Audio", Toast.LENGTH_LONG).show();
    }

    private AACEncoder encoder = new AACEncoder();
    private long speechStarted = 0;
    private SpeechRecognizer sr;
    private ProgressDialog pd;

    private void write() {
        sr.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.domain.app");

        speechStarted = 0;
        baos.reset();

        pd = new ProgressDialog(this);
        pd.setMessage("Speak now...");
        pd.setIndeterminate(true);
        pd.setCancelable(true);
        pd.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                sr.cancel();
                onEndOfSpeech();
            }
        });

        pd.show();
        sr.startListening(intent);

        speechStarted = System.currentTimeMillis();
    }

    @Override
    public void onBeginningOfSpeech() {
        System.err.println("beginning");

    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void onBufferReceived(byte[] buffer) {
        if(speechStarted > 0) {
            try {
                baos.write(buffer);
            } catch (IOException e) {
                //
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sr.destroy();
    }

    @Override
    public void onEndOfSpeech() {
        pd.dismiss();

        if(speechStarted == 0)
            return;

        long delta = System.currentTimeMillis() - speechStarted;

        int sampleRate = (int) (baos.size() * 1000 / delta);
        sampleRate = 8000; // THIS IS A MAGIC NUMBER@?!!?!?!
        // can i has calculate?

        System.err.println("computed sample rate: " + sampleRate);

        encoder.init(64000, 1, sampleRate, 16, AAC_FILE);

        encoder.encode(baos.toByteArray());

        System.err.println("end");

        encoder.uninit();

        try {
            new AACToM4A().convert(this, AAC_FILE, M4A_FILE);

            Toast.makeText(Main.this, "File Saved!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(Main.this, "Error :(", Toast.LENGTH_LONG).show();
            Log.e("ERROR", "error converting", e);
        }
    }

    @Override
    public void onError(int error) {
        Log.w("Speech Error", "Error code: " + error);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        //
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        onResults(partialResults);
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> strings = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        ((TextView)findViewById(R.id.text)).setText(
                strings.size() == 0 ? "" : strings.get(0));
    }

    @Override
    public void onRmsChanged(float arg0) {
    }
}