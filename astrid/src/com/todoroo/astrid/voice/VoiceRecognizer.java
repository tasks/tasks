package com.todoroo.astrid.voice;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.widget.EditText;
import android.widget.ImageButton;

import com.timsu.astrid.R;
import com.todoroo.aacenc.RecognizerApi;
import com.todoroo.aacenc.RecognizerApi.RecognizerApiListener;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;

public class VoiceRecognizer {

    protected RecognizerApi recognizerApi;
    protected VoiceInputAssistant voiceInputAssistant;

    public static boolean speechRecordingAvailable(Context context) {
        return ActFmPreferenceService.isPremiumUser() &&
                AndroidUtilities.getSdkVersion() >= 8 &&
                SpeechRecognizer.isRecognitionAvailable(context);
    }

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

    private VoiceRecognizer() {
        //
    }

    private static VoiceRecognizer instance = null;

    public static VoiceRecognizer instantiateVoiceRecognizer(Context context, RecognizerApiListener listener,
            Fragment fragment, ImageButton voiceAddButton, EditText quickAddBox) {
        synchronized(VoiceRecognizer.class) {
            if (instance == null)
                instance = new VoiceRecognizer();
        }

        if (speechRecordingAvailable(context)) {
            if (instance.recognizerApi != null)
                instance.recognizerApi.destroy();

            instance.recognizerApi = new RecognizerApi(context);
            instance.recognizerApi.setListener(listener);
        } else {
            instance.voiceInputAssistant = new VoiceInputAssistant(fragment,
                    voiceAddButton, quickAddBox);
        }
        return instance;
    }

    public void startVoiceRecognition(Context context, String currentVoiceFile) {
        if (speechRecordingAvailable(context)) {
            recognizerApi.setTemporaryFile(currentVoiceFile);
            recognizerApi.start();
        } else {
            int prompt = R.string.voice_edit_title_prompt;
            if (Preferences.getBoolean(R.string.p_voiceInputCreatesTask, false))
                prompt = R.string.voice_create_prompt;
            voiceInputAssistant.startVoiceRecognitionActivity(prompt);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (instance != null && instance.voiceInputAssistant != null)
            return instance.voiceInputAssistant.handleActivityResult(requestCode, resultCode, data);
        return false;
    }

    public void destroyRecognizerApi() {
        if (instance != null && instance.recognizerApi != null) {
            instance.recognizerApi.destroy();
            instance.recognizerApi = null;
        }
    }

    public void cancel() {
        if (instance != null && instance.recognizerApi != null)
            instance.recognizerApi.cancel();
    }

    public void convert(String filePath) {
        if (instance != null && instance.recognizerApi != null)
            instance.recognizerApi.convert(filePath);
    }
}
