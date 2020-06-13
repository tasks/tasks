package org.tasks.notifications;

import android.content.Context;
import javax.inject.Inject;
import org.tasks.injection.ApplicationContext;

public class AudioManager {

  private final android.media.AudioManager audioManager;

  @Inject
  public AudioManager(@ApplicationContext Context context) {
    audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  public boolean notificationsMuted() {
    return audioManager.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION) == 0;
  }
}
