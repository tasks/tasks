/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;

import com.todoroo.andlib.data.Callback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class handles taking voice-input and appends the text to the registered EditText-instance.
 * You can have multiple VoiceInputAssistants per Fragment, just use the additional constructor
 * to specify unique requestCodes for the RecognizerIntent (e.g. VoiceInputAssistant.VOICE_RECOGNITION_REQUEST_CODE+i).
 * If you have only one VoiceInputAssitant on an Fragment, just use the normal constructor.
 * <p>
 * You can query voiceinput-capabilities by calling isVoiceInputAvailable() for external checking,
 * but the visibility for the microphone-button specified by the constructor is handled in configureMicrophoneButton(int).
 *
 * @author Arne Jans
 */
@Singleton
public class VoiceInputAssistant {

    /** requestcode for activityresult from voicerecognizer-intent */
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    /**
     * Call this to see if your phone supports voiceinput in its current configuration.
     * If this method returns false, it could also mean that Google Voicesearch is simply
     * not installed.
     * If this method returns true, internal use of it enables the registered microphone-button.
     *
     * @return whether this phone supports voiceinput
     */
    public static boolean voiceInputAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        return (activities.size() != 0);
    }

    /**
     * This requestcode is used to differentiate between multiple microphone-buttons on a single fragment.
     * Use the mightier constructor to specify your own requestCode in this case for every additional use on an fragment.
     * If you only use one microphone-button on an fragment, you can leave it to its default, VOICE_RECOGNITION_REQUEST_CODE.
     */
    private int requestCode = VOICE_RECOGNITION_REQUEST_CODE;
    private Activity activity;

    /**
     * This constructor can be called from a widget with a voice-button calling a dummy-activity.
     *
     * @param activity dummy-activity that starts the voice-request.
     */
    @Inject
    public VoiceInputAssistant(Activity activity) {
        this.activity = activity;
    }

    /**
     * Fire an intent to start the speech recognition activity.
     * This is fired by the listener on the microphone-button.
     *
     * @param prompt Specify the R.string.string_id resource for the prompt-text during voice-recognition here
     */
    public void startVoiceRecognitionActivity(int prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(prompt));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * This callback-method has to be called from Fragment.onActivityResult within your fragment
     * with parameters directly on passthru.<br>
     * You can check in your fragment if it was really a RecognizerIntent that was handled here,
     * if so, this method returns true. In this case, you should call super.onActivityResult in your
     * fragment.onActivityResult.
     * <p>
     * If this method returns false, then it wasnt a request with a RecognizerIntent, so you can handle
     * these other requests as you need.
     *
     * @param activityRequestCode if this equals the requestCode specified by constructor, then results of voice-recognition
     */
    public boolean handleActivityResult(int activityRequestCode, int resultCode, Intent data, Callback<String> onVoiceRecognition) {
        boolean result = false;
        // handle the result of voice recognition, put it into the textfield
        if (activityRequestCode == this.requestCode) {
            // this was handled here, even if voicerecognition fails for any reason
            // so your program flow wont get chaotic if you dont explicitly state
            // your own requestCodes.
            result = true;
            if (resultCode == Activity.RESULT_OK) {
                // Fill the quickAddBox-view with the string the recognizer thought it could have heard
                ArrayList<String> match = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                // make sure we only do this if there is SomeThing (tm) returned
                if (match != null && match.size() > 0 && match.get(0).length() > 0) {
                    String recognizedSpeech = match.get(0);
                    recognizedSpeech = recognizedSpeech.substring(0, 1).toUpperCase() +
                        recognizedSpeech.substring(1).toLowerCase();

                    onVoiceRecognition.apply(recognizedSpeech);
                }
            }
        }

        return result;
    }
}
