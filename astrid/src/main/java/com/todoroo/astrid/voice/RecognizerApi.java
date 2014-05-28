package com.todoroo.astrid.voice;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@TargetApi(8)
public class RecognizerApi implements RecognitionListener {

    public static interface PlaybackExceptionHandler {
        public void playbackFailed();
    }

    private Context context;

    public static interface RecognizerApiListener {
        public void onSpeechResult(String result);

        public void onSpeechError(int error);
    }

    private RecognizerApiListener mListener;

    public RecognizerApi(Context context) {
        this.context = context;

        sr = SpeechRecognizer.createSpeechRecognizer(context);
    }

    public void setListener(RecognizerApiListener listener) {
        this.mListener = listener;
    }

    public static void play(String file, PlaybackExceptionHandler handler) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(file);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            handler.playbackFailed();
        }
    }

    private long speechStarted = 0;
    private SpeechRecognizer sr;
    private ProgressDialog speakPd;
    private ProgressDialog processingPd;

    public void start(String callingPackage, String speakNowMessage) {
        sr.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, callingPackage);

        speechStarted = 0;
        baos.reset();

        speakPd = new ProgressDialog(context);
        speakPd.setMessage(speakNowMessage);
        speakPd.setIndeterminate(true);
        speakPd.setCancelable(true);
        speakPd.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                sr.cancel();
                onEndOfSpeech();
            }
        });

        speakPd.show();
        sr.startListening(intent);

        speechStarted = System.currentTimeMillis();
    }

    public void cancel() {
        sr.cancel();
    }

    public void destroy() {
        sr.setRecognitionListener(null);
        sr.destroy();
    }

    // --- RecognitionListener methods --- //

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        if (speechStarted > 0) {
            try {
                baos.write(buffer);
            } catch (IOException e) {
                //
            }
        }
    }

    @Override
    public void onEndOfSpeech() {
        speakPd.dismiss();

        if (speechStarted == 0)
            return;

        processingPd = new ProgressDialog(context);
        processingPd.setMessage("");
        processingPd.setIndeterminate(true);
        processingPd.setCancelable(true);
        processingPd.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                sr.cancel();
            }
        });
        processingPd.show();
    }

    @Override
    public void onError(int error) {
        if (mListener != null) {
            mListener.onSpeechError(error);
        }
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
        processingPd.dismiss();
        ArrayList<String> strings = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (mListener != null) {
            mListener.onSpeechResult(strings == null || strings.size() == 0 ? "" : strings.get(0));
        }
    }

    @Override
    public void onRmsChanged(float arg0) {
    }

}