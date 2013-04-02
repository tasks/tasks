/**
 *
 */
package com.todoroo.astrid.voice;

import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.voice.VoiceOutputService.VoiceOutputAssistant;

/**
 * @author Arne Jans
 *
 */
@SuppressWarnings("nls")
public class Api6VoiceOutputAssistant implements OnInitListener, VoiceOutputAssistant {

    private static final int MY_DATA_CHECK_CODE = 2534;
    private static final String TAG = "Astrid.VoiceOutputAssistant";
    private final Context context;
    private TextToSpeech mTts;
    private boolean isTTSInitialized;
    private boolean retryLastSpeak;
    private String lastTextToSpeak;
    private static final HashMap<String, String> ttsParams = new HashMap<String, String>();

    static {
        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                String.valueOf(AudioManager.STREAM_NOTIFICATION));
    }

    Api6VoiceOutputAssistant() {
        this.context = ContextManager.getContext().getApplicationContext();
    }

    public void checkIsTTSInstalled() {
        if (!isTTSInitialized && context instanceof Activity) {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            ((Activity) context).startActivityForResult(checkIntent,
                    MY_DATA_CHECK_CODE);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                initTTS();
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                context.startActivity(installIntent);
            }

            return true;
        }

        return false;
    }

    private void initTTS() {
        mTts = new TextToSpeech(context, (OnInitListener)this);
    }

    public void queueSpeak(String textToSpeak) {
        if (mTts != null && isTTSInitialized) {
            mTts.speak(textToSpeak, TextToSpeech.QUEUE_ADD, ttsParams);
            while (mTts.isSpeaking()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            retryLastSpeak = true;
            this.lastTextToSpeak = textToSpeak;
            initTTS();
        }
    }

    @Override
    public void onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS && mTts != null) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mTts.setLanguage(Locale.getDefault());
            // Try this someday for some interesting results.
            // int result mTts.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
               // Language data is missing or the language is not supported.
                Log.e(TAG, "Language is not available.");
            } else {
                // Check the documentation for other possible result codes.
                // For example, the language may be available for the locale,
                // but not for the specified country and variant.

                mTts.speak("", 0, null);

                // The TTS engine has been successfully initialized.
                isTTSInitialized = true;
                // if this request came from queueSpeak, then speak it and reset the memento
                if (retryLastSpeak && this.lastTextToSpeak != null) {
                    this.queueSpeak(this.lastTextToSpeak);
                    retryLastSpeak = false;
                    lastTextToSpeak = null;
                }
            }
        } else {
            // Initialization failed.
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
    }

    /**
     * Has to be called in onDestroy of the activity that uses this instance of VoiceOutputAssistant.
     */
    public void onDestroy() {
        if (mTts != null && isTTSInitialized) {
            mTts.shutdown();
            mTts = null;
            isTTSInitialized = false;
        }
    }

}
