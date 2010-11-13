package com.todoroo.astrid.voice;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

/**
 * This class handles taking voice-input and appends the text to the registered EditText-instance.
 * You can have multiple VoiceInputAssistants per Activity, just use the additional constructor
 * to specify unique requestCodes for the RecognizerIntent (e.g. VoiceInputAssistant.VOICE_RECOGNITION_REQUEST_CODE+i).
 * If you have only one VoiceInputAssitant on an Activity, just use the normal constructor.
 * <p>
 * You can query voiceinput-capabilities by calling isVoiceInputAvailable() for external checking,
 * but the visibility for the microphone-button specified by the constructor is handled in configureMicrophoneButton(int).
 *
 * @author Arne Jans
 */
@SuppressWarnings("nls")
public class VoiceInputAssistant {

    /** requestcode for activityresult from voicerecognizer-intent */
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    /**
     * This requestcode is used to differentiate between multiple microphone-buttons on a single activity.
     * Use the mightier constructor to specify your own requestCode in this case for every additional use on an activity.
     * If you only use one microphone-button on an activity, you can leave it to its default, VOICE_RECOGNITION_REQUEST_CODE.
     */
    private int requestCode = VOICE_RECOGNITION_REQUEST_CODE;
    private final Activity activity;
    private final ImageButton voiceButton;
    private final EditText textField;
    private boolean append = false;

    private String languageModel = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH;

    /**
     * @param languageModel the languageModel to set
     */
    public void setLanguageModel(String languageModel) {
        this.languageModel = languageModel;
    }

    /**
     * @return the languageModel
     */
    public String getLanguageModel() {
        return languageModel;
    }

    /** Sets whether voice input will append into field */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Creates a new VoiceInputAssistant-instance simply for checking the availability of the
     * RecognizerService. This is used for Preferences-Screens that dont want to provide
     * a microphone-button themselves.
     */
    public VoiceInputAssistant(Activity activity) {
        Assert.assertNotNull("Each VoiceInputAssistant must be bound to an activity!", activity);
        this.activity = activity;
        this.voiceButton = null;
        this.textField = null;
    }

    /**
     * Creates a new VoiceInputAssistance-instance for use with a specified button and textfield.
     * If you need more than one microphone-button on a given Activity, use the other constructor.
     *
     * @param activity the Activity which holds the microphone-buttone and the textField to insert recognized test
     * @param voiceButton the microphone-Button
     * @param textField the textfield that should get the resulttext
     */
    public VoiceInputAssistant(Activity activity, ImageButton voiceButton, EditText textField) {
        Assert.assertNotNull("Each VoiceInputAssistant must be bound to an activity!", activity);
        Assert.assertNotNull("A VoiceInputAssistant without a voiceButton makes no sense!", voiceButton);
        Assert.assertNotNull("You have to specify a textfield that is bound to this VoiceInputAssistant!!", textField);
        this.activity = activity;
        this.voiceButton = voiceButton;
        this.textField = textField;
    }

    /**
     * The param requestCode is used to differentiate between multiple
     * microphone-buttons on a single activity.
     * Use the this constructor to specify your own requestCode in
     * this case for every additional use on an activity.
     * If you only use one microphone-button on an activity,
     * you can leave it to its default, VOICE_RECOGNITION_REQUEST_CODE.
     *
     *
     * @param activity
     * @param voiceButton
     * @param textField
     * @param requestCode has to be unique in a single Activity-context,
     *   dont use VOICE_RECOGNITION_REQUEST_CODE, this is reserved for the other constructor
     */
    public VoiceInputAssistant(Activity activity, ImageButton voiceButton, EditText textField, int requestCode) {
        this(activity, voiceButton, textField);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)
            throw new InvalidParameterException("You have to specify a unique requestCode for this VoiceInputAssistant!");
        this.requestCode = requestCode;
    }

    /**
     * Fire an intent to start the speech recognition activity.
     * This is fired by the listener on the microphone-button.
     *
     * @param prompt Specify the R.string.string_id resource for the prompt-text during voice-recognition here
     */
    public void startVoiceRecognitionActivity(int prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(prompt));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * This callback-method has to be called from Activity.onActivityResult within your activity
     * with parameters directly on passthru.<br>
     * You can check in your activity if it was really a RecognizerIntent that was handled here,
     * if so, this method returns true. In this case, you should call super.onActivityResult in your
     * activity.onActivityResult.
     * <p>
     * If this method returns false, then it wasnt a request with a RecognizerIntent, so you can handle
     * these other requests as you need.
     *
     * @param activityRequestCode if this equals the requestCode specified by constructor, then results of voice-recognition
     * @param resultCode
     * @param data
     * @return
     */
    public boolean handleActivityResult(int activityRequestCode, int resultCode, Intent data) {
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

                    if(append)
                        textField.setText((textField.getText() + " " + recognizedSpeech).trim());
                    else
                        textField.setText(recognizedSpeech);
                }
            }
        }

        return result;
    }

    /**
     * Call this to see if your phone supports voiceinput in its current configuration.
     * If this method returns false, it could also mean that Google Voicesearch is simply
     * not installed.
     * If this method returns true, internal use of it enables the registered microphone-button.
     *
     * @return whether this phone supports voiceinput
     */
    public boolean isVoiceInputAvailable() {
        // Check to see if a recognition activity is present
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        return (activities.size() != 0);
    }

    public void configureMicrophoneButton(final int prompt) {
        if (Preferences.getBoolean(R.string.p_voiceInputEnabled, true) && isVoiceInputAvailable()) {
            voiceButton.setVisibility(View.VISIBLE);
            voiceButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    startVoiceRecognitionActivity(prompt);
                }
            });
        } else {
            voiceButton.setVisibility(View.GONE);
        }
    }
}
