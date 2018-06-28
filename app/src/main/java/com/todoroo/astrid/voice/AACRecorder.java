package com.todoroo.astrid.voice;

import android.arch.lifecycle.ViewModel;
import android.media.MediaRecorder;
import android.os.SystemClock;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class AACRecorder extends ViewModel {

  private MediaRecorder mediaRecorder;
  private final AtomicReference<String> nameRef = new AtomicReference<>();

  private boolean recording;
  private AACRecorderCallbacks listener;
  private Preferences preferences;
  private long base;
  private String tempFile;

  public synchronized void startRecording() {
    if (recording) {
      return;
    }

    tempFile = preferences.getNewAudioAttachmentPath(nameRef);

    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mediaRecorder.setOutputFile(tempFile);
    mediaRecorder.setOnErrorListener(
        (mr, what, extra) -> Timber.e("mediaRecorder.onError(mr, %s, %s)", what, extra));
    mediaRecorder.setOnInfoListener(
        (mr, what, extra) -> Timber.i("mediaRecorder.onInfo(mr, %s, %s)", what, extra));

    try {
      mediaRecorder.prepare();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    recording = true;
    base = SystemClock.elapsedRealtime();
    mediaRecorder.start();
  }

  public synchronized void stopRecording() {
    if (!recording) {
      return;
    }

    try {
      // media recorder was cutting off end of audio
      // this is a hack to keep recording
      Thread.sleep(500);
    } catch (InterruptedException ignored) {
    }

    mediaRecorder.stop();
    mediaRecorder.release();
    recording = false;
    if (listener != null) {
      listener.encodingFinished(tempFile);
    }
  }

  public long getBase() {
    return base;
  }

  public void init(AACRecorderCallbacks listener, Preferences preferences) {
    this.listener = listener;
    this.preferences = preferences;
  }

  public interface AACRecorderCallbacks {

    void encodingFinished(String path);
  }
}
