/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.voice;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.utility.Constants;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;

import java.security.InvalidParameterException;
import java.util.ArrayList;

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
public class VoiceInputAssistant {

    private static final Logger log = LoggerFactory.getLogger(VoiceInputAssistant.class);

    /** requestcode for activityresult from voicerecognizer-intent */
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    /**
     * This requestcode is used to differentiate between multiple microphone-buttons on a single fragment.
     * Use the mightier constructor to specify your own requestCode in this case for every additional use on an fragment.
     * If you only use one microphone-button on an fragment, you can leave it to its default, VOICE_RECOGNITION_REQUEST_CODE.
     */
    private int requestCode = VOICE_RECOGNITION_REQUEST_CODE;
    private Activity activity;
    private final ImageButton voiceButton;
    private boolean append = false;

    private String languageModel = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH;

    /**
     * @param languageModel the languageModel to set
     */
    public void setLanguageModel(String languageModel) {
        this.languageModel = languageModel;
    }

    /** Sets whether voice input will append into field */
    public void setAppend() {
        this.append = true;
    }

    /**
     * This constructor can be called from a widget with a voice-button calling a dummy-activity.
     *
     * @param activity dummy-activity that starts the voice-request.
     */
    public VoiceInputAssistant(Activity activity) {
        this.activity = activity;
        this.voiceButton = null;
    }

    /**
     * Creates a new VoiceInputAssistance-instance for use with a specified button and textfield.
     * If you need more than one microphone-button on a given fragment, use the other constructor.
     *
     * @param voiceButton the microphone-Button
     */
    public VoiceInputAssistant(ImageButton voiceButton) {
        Assert.assertNotNull("A VoiceInputAssistant without a voiceButton makes no sense!", voiceButton);
        this.voiceButton = voiceButton;
    }

    /**
     * The param requestCode is used to differentiate between multiple
     * microphone-buttons on a single fragment.
     * Use the this constructor to specify your own requestCode in
     * this case for every additional use on a fragment.
     * If you only use one microphone-button on a fragment,
     * you can leave it to its default, VOICE_RECOGNITION_REQUEST_CODE.
     *
     *
     * @param requestCode has to be unique in a single fragment-context,
     *   dont use VOICE_RECOGNITION_REQUEST_CODE, this is reserved for the other constructor
     */
    public VoiceInputAssistant(ImageButton voiceButton, int requestCode) {
        this(voiceButton);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            throw new InvalidParameterException("You have to specify a unique requestCode for this VoiceInputAssistant!");
        }
        this.requestCode = requestCode;
    }

    /**
     * Fire an intent to start the speech recognition activity.
     * This is fired by the listener on the microphone-button.
     *
     * @param prompt Specify the R.string.string_id resource for the prompt-text during voice-recognition here
     */
    public void startVoiceRecognitionActivity(Fragment fragment, int prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, ContextManager.getContext().getString(prompt));
        String detailMessage = "Error! No Fragment or Activity was registered to handle this voiceinput-request!";
        if (activity != null) {
            activity.startActivityForResult(intent, requestCode);
        } else if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode);
        } else {
            log.error(detailMessage, new IllegalStateException(detailMessage));
        }
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
    public boolean handleActivityResult(int activityRequestCode, int resultCode, Intent data, EditText textField) {
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

                    if(append) {
                        textField.setText((textField.getText() + " " + recognizedSpeech).trim());
                    } else {
                        textField.setText(recognizedSpeech);
                    }
                }
            }
        }

        return result;
    }

    public void configureMicrophoneButton(final Fragment fragment, final int prompt) {
        voiceButton.setVisibility(View.VISIBLE);
        voiceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognitionActivity(fragment, prompt);
            }
        });
    }

    public void hideVoiceButton() {
        voiceButton.setVisibility(View.GONE);
    }

    public void showVoiceInputMarketSearch(DialogInterface.OnClickListener onFail) {
        String packageName;
        if(AndroidUtilities.getSdkVersion() <= 7) {
            packageName = "com.google.android.voicesearch.x";
        } else {
            packageName = "com.google.android.voicesearch";
        }

        // User wants to install voice search, take them to the market
        Intent marketIntent = Constants.MARKET_STRATEGY.generateMarketLink(packageName);
        if (activity != null) {
            try {
                if (marketIntent == null) {
                    throw new ActivityNotFoundException("No market link supplied"); //$NON-NLS-1$
                }
                activity.startActivity(marketIntent);
            } catch (ActivityNotFoundException ane) {
                log.error(ane.getMessage(), ane);
                DialogUtilities.okDialog(activity,
                        activity.getString(R.string.EPr_marketUnavailable_dlg),
                        onFail);
            }
        }
    }
}
