/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.voice;


import android.content.Intent;

import com.todoroo.andlib.utility.AndroidUtilities;




/**
 * All API versions-friendly voice input / output.
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class VoiceOutputService {

    private static final int MIN_TTS_VERSION = 6;
    private static VoiceOutputAssistant outputAssistant;

    // --- voice output

    public interface VoiceOutputAssistant {
        public void checkIsTTSInstalled();

        public boolean handleActivityResult(int requestCode, int resultCode, Intent data);

        public void queueSpeak(String textToSpeak);

        public void onDestroy();
    }

    public static class NullVoiceOutputAssistant implements VoiceOutputAssistant {

        @Override
        public void checkIsTTSInstalled() {
            //
        }

        @Override
        public boolean handleActivityResult(int requestCode, int resultCode,
                Intent data) {
            return false;
        }

        @Override
        public void queueSpeak(String textToSpeak) {
            //
        }

        @Override
        public void onDestroy() {
            //
        }

    }

    public static VoiceOutputAssistant getVoiceOutputInstance() {
        if(AndroidUtilities.getSdkVersion() >= MIN_TTS_VERSION) {
            if (outputAssistant == null)
                outputAssistant = new Api6VoiceOutputAssistant();
        } else {
            if(outputAssistant == null)
                outputAssistant = new NullVoiceOutputAssistant();
        }

        return outputAssistant;
    }

}
