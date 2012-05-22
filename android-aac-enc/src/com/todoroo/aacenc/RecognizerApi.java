package com.todoroo.aacenc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class RecognizerApi implements RecognitionListener {

    private String aacFile;

    private Context context;
    
    public static interface RecognizerApiListener {
    	public void onSpeechResult(String result);
    	public void onSpeechError(int error);
    }
    
    private RecognizerApiListener mListener;
    
    public RecognizerApi(Context context) {
    	this.context = context;

    	File dir = context.getFilesDir();
    	aacFile = dir.toString() + "/audio.aac";
    	
    	sr = SpeechRecognizer.createSpeechRecognizer(context);
    }
    
    public void setTemporaryFile(String fileName) {
    	aacFile = context.getFilesDir().toString() + "/" + fileName;
    }
    
    public String getTemporaryFile() {
    	return aacFile;
    }
    
    public void setListener(RecognizerApiListener listener) {
    	this.mListener = listener;
    }

    public static void play(String file) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(file);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Toast.makeText(context, "Playing Audio", Toast.LENGTH_LONG).show();
    }

    private AACEncoder encoder = new AACEncoder();
    private long speechStarted = 0;
    private SpeechRecognizer sr;
    private ProgressDialog speakPd;
    private ProgressDialog processingPd;

    public void start() {
        sr.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.domain.app");

        speechStarted = 0;
        baos.reset();

        speakPd = new ProgressDialog(context);
        speakPd.setMessage("Speak now...");
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

    public void convert(String toFile) {
    	try {
    		new AACToM4A().convert(context, aacFile, toFile);
    		
    		Toast.makeText(context, "File Saved!", Toast.LENGTH_LONG).show();
    	} catch (IOException e) {
    		Toast.makeText(context, "Error :(", Toast.LENGTH_LONG).show();
    		Log.e("ERROR", "error converting", e);
    	}
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
        System.err.println("beginning");

    }

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
    public void onEndOfSpeech() {
        speakPd.dismiss();

        if(speechStarted == 0)
            return;
        
        processingPd = new ProgressDialog(context);
        processingPd.setMessage("Processing...");
        processingPd.setIndeterminate(true);
        processingPd.setCancelable(true);
        processingPd.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                sr.cancel();
            }
        });
        processingPd.show();

        long delta = System.currentTimeMillis() - speechStarted;

        int sampleRate = (int) (baos.size() * 1000 / delta);
        sampleRate = 8000; // THIS IS A MAGIC NUMBER@?!!?!?!
        // can i has calculate?

        System.err.println("computed sample rate: " + sampleRate);

        encoder.init(64000, 1, sampleRate, 16, aacFile);

        encoder.encode(baos.toByteArray());

        System.err.println("end");

        encoder.uninit();
    }

    @Override
    public void onError(int error) {
    	if (mListener != null)
    		mListener.onSpeechError(error);
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
        if (mListener != null)
        	mListener.onSpeechResult(strings.size() == 0 ? "" : strings.get(0));
    }

    @Override
    public void onRmsChanged(float arg0) {
    }
    
    public static class Main extends Activity implements RecognizerApiListener {
    	private RecognizerApi api;
    	
    	private static final String M4A_FILE = "/sdcard/audio.m4a";
    	
    	/** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            api = new RecognizerApi(this);
            
            setContentView(R.layout.main);

            findViewById(R.id.write).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    api.start();
                }
            });

            findViewById(R.id.play).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    api.play(M4A_FILE);
                }
            });

        }
        
        @Override
        protected void onDestroy() {
        	super.onDestroy();
        	api.destroy();
        }
        
        @Override
        public void onSpeechResult(String result) {
        	((TextView)findViewById(R.id.text)).setText(result);
        	api.convert(M4A_FILE);
        }
        
        @Override
        public void onSpeechError(int error) {
        	// TODO Auto-generated method stub
        }
    }
}