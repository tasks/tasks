package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Chronometer;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.todoroo.astrid.voice.AACRecorder;

import org.tasks.R;
import org.tasks.databinding.AacRecordActivityBinding;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;

import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecordAudioDialog extends DialogFragment implements AACRecorder.AACRecorderCallbacks {

  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject Theme theme;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;

  private Chronometer timer;
  private AACRecorder recorder;

  static RecordAudioDialog newRecordAudioDialog(Fragment target, int requestCode) {
    RecordAudioDialog dialog = new RecordAudioDialog();
    dialog.setTargetFragment(target, requestCode);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AacRecordActivityBinding binding =
            AacRecordActivityBinding.inflate(theme.getLayoutInflater(getContext()));
    timer = binding.timer;
    binding.stopRecording.setOnClickListener(v -> stopRecording());
    recorder = new ViewModelProvider(this).get(AACRecorder.class);
    recorder.init(this, preferences);

    if (permissionChecker.canAccessMic()) {
      startRecording();
    } else if (savedInstanceState == null) {
      permissionRequestor.requestMic();
    }

    return dialogBuilder
        .newDialog(R.string.audio_recording_title)
        .setView(binding.getRoot())
        .create();
  }

  private void startRecording() {
    try {
      recorder.startRecording(getContext());
      timer.setBase(recorder.getBase());
      timer.start();
    } catch (IOException e) {
      stopRecording();
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    stopRecording();
  }

  private void stopRecording() {
    recorder.stopRecording();
    timer.stop();
  }

  @Override
  public void encodingFinished(Uri uri) {
    Intent intent = new Intent();
    intent.setData(uri);
    Fragment target = getTargetFragment();
    if (target != null) {
      target.onActivityResult(getTargetRequestCode(), RESULT_OK, intent);
    }
    dismiss();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_MIC) {
      if (verifyPermissions(grantResults)) {
        startRecording();
      } else {
        dismiss();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }
}
