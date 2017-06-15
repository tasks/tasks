package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;

import com.todoroo.astrid.voice.AACRecorder;

import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecordAudioDialog extends InjectingDialogFragment implements AACRecorder.AACRecorderCallbacks {

    public static RecordAudioDialog newRecordAudioDialog() {
        return new RecordAudioDialog();
    }

    public interface RecordAudioDialogCallback {
        void finished(String path);
    }

    @Inject Preferences preferences;
    @Inject DialogBuilder dialogBuilder;
    @Inject Theme theme;

    @BindView(R.id.timer) Chronometer timer;

    private final AtomicReference<String> nameRef = new AtomicReference<>();
    private AACRecorder recorder;
    private String tempFile;
    private RecordAudioDialogCallback callback;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = theme.getLayoutInflater(getContext());
        View view = layoutInflater.inflate(R.layout.aac_record_activity, null);
        ButterKnife.bind(this, view);

        startRecording();

        return dialogBuilder.newDialog()
                .setTitle(R.string.audio_recording_title)
                .setView(view)
                .create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        callback = (RecordAudioDialogCallback) activity;
    }

    @Override
    public void onPause() {
        super.onPause();

        stopRecording();
    }

    @OnClick(R.id.stop_recording)
    void stopRecording() {
        if (recorder != null) {
            recorder.stopRecording();
            timer.stop();
        }
    }

    private void startRecording() {
        tempFile = preferences.getNewAudioAttachmentPath(nameRef);
        recorder = new AACRecorder();
        recorder.setListener(this);
        recorder.startRecording(tempFile);
        timer.start();
    }

    @Override
    protected void inject(DialogFragmentComponent component) {
        component.inject(this);
    }

    @Override
    public void encodingFinished() {
        callback.finished(tempFile);
    }
}
