package com.todoroo.astrid.voice;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.SystemClock;
import androidx.lifecycle.ViewModel;
import java.io.IOException;
import org.tasks.files.FileHelper;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;
import timber.log.Timber;

public class AACRecorder extends ViewModel {

  private MediaRecorder mediaRecorder;

  private boolean recording;
  private AACRecorderCallbacks listener;
  private Preferences preferences;
  private long base;
  private Uri uri;

  public synchronized void startRecording(Context context) throws IOException {
    if (recording) {
      return;
    }

    uri =
        FileHelper.newFile(
            context,
            preferences.getCacheDirectory(),
            "audio/m4a",
            new DateTime().toString("yyyyMMddHHmm"),
            ".m4a");

    mediaRecorder = new MediaRecorder();
    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    mediaRecorder.setOutputFile(uri.getPath());
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
      listener.encodingFinished(uri);
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

    void encodingFinished(Uri uri);
  }
}
