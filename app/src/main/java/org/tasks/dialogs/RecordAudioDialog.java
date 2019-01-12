package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.voice.AACRecorder;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.preferences.Preferences;
import org.tasks.themes.Theme;

public class RecordAudioDialog extends InjectingDialogFragment
    implements AACRecorder.AACRecorderCallbacks {

  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  @Inject Theme theme;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject PermissionChecker permissionChecker;

  @BindView(R.id.timer)
  Chronometer timer;

  private AACRecorder recorder;

  static RecordAudioDialog newRecordAudioDialog(FilesControlSet target, int requestCode) {
    RecordAudioDialog dialog = new RecordAudioDialog();
    dialog.setTargetFragment(target, requestCode);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater layoutInflater = theme.getLayoutInflater(getContext());
    View view = layoutInflater.inflate(R.layout.aac_record_activity, null);
    ButterKnife.bind(this, view);

    recorder = ViewModelProviders.of(this).get(AACRecorder.class);
    recorder.init(this, preferences);

    if (permissionChecker.canAccessMic()) {
      startRecording();
    } else if (savedInstanceState == null) {
      permissionRequestor.requestMic();
    }

    return dialogBuilder
        .newDialog()
        .setTitle(R.string.audio_recording_title)
        .setView(view)
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

  @OnClick(R.id.stop_recording)
  void stopRecording() {
    recorder.stopRecording();
    timer.stop();
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
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
