package org.tasks.notifications;

import android.app.Application;
import android.content.Context;
import javax.inject.Inject;

public class AudioManager {

  private final android.media.AudioManager audioManager;

  @Inject
  public AudioManager(Application context) {
    audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  public boolean notificationsMuted() {
    return audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION) == 0;
  }
}
