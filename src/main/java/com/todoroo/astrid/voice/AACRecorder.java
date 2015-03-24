package com.todoroo.astrid.voice;

import android.annotation.TargetApi;
import android.media.MediaRecorder;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AACRecorder {

    private static final Logger log = LoggerFactory.getLogger(AACRecorder.class);

    private MediaRecorder mediaRecorder;

    private boolean recording;
    private AACRecorderCallbacks listener;

    public interface AACRecorderCallbacks {
        public void encodingFinished();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    public synchronized void startRecording(final String tempFile) {
        if (recording) {
            return;
        }

        mediaRecorder = new MediaRecorder() {{
            setAudioSource(AudioSource.MIC);
            setOutputFormat(OutputFormat.MPEG_4);
            setAudioEncoder(AudioEncoder.AAC);
            setOutputFile(tempFile);
            setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    log.error("mediaRecorder.onError(mr, {}, {})", what, extra);
                }
            });
            setOnInfoListener(new OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    log.info("mediaRecorder.onInfo(mr, {}, {})", what, extra);
                }
            });
        }};

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        recording = true;
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
            listener.encodingFinished();
        }
    }

    public void setListener(AACRecorderCallbacks listener) {
        this.listener = listener;
    }
}
