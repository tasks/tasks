package org.tasks.notifications;

import android.content.Context;

import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class AudioManager {

    private final android.media.AudioManager audioManager;

    @Inject
    public AudioManager(@ForApplication Context context) {
        audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public int getAlarmVolume() {
        return audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM);
    }

    public void setMaxAlarmVolume() {
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM), 0);
    }

    public boolean notificationsMuted() {
        return audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION) == 0;
    }

    public boolean isRingtoneMode() {
        return audioManager.getMode() == android.media.AudioManager.MODE_RINGTONE;
    }

    public void setAlarmVolume(int volume) {
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, volume, 0);
    }
}
