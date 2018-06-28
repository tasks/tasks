package org.tasks.dialogs;

import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.dialogs.RecordAudioDialog.newRecordAudioDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.NonNull;
import com.todoroo.astrid.files.FilesControlSet;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.CameraActivity;
import org.tasks.files.FileExplore;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Device;
import org.tasks.preferences.Preferences;

public class AddAttachmentDialog extends InjectingDialogFragment {

  private static final String FRAG_TAG_RECORD_AUDIO = "frag_tag_record_audio";
  public static final int REQUEST_CAMERA = 12120;
  public static final int REQUEST_GALLERY = 12121;
  public static final int REQUEST_STORAGE = 12122;
  public static final int REQUEST_AUDIO = 12123;

  public static final String EXTRA_PATH = "extra_path";
  public static final String EXTRA_TYPE = "extra_type";

  @Inject @ForActivity Context context;
  @Inject DialogBuilder dialogBuilder;
  @Inject Device device;
  @Inject Preferences preferences;

  public static AddAttachmentDialog newAddAttachmentDialog(FilesControlSet target) {
    AddAttachmentDialog dialog = new AddAttachmentDialog();
    dialog.setTargetFragment(target, 0);
    return dialog;
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<String> entries = newArrayList();
    final List<Runnable> actions = newArrayList();
    if (device.hasCamera()) {
      entries.add(getString(R.string.take_a_picture));
      actions.add(this::takePicture);
    }
    entries.add(getString(R.string.premium_record_audio));
    actions.add(this::recordNote);
    if (device.hasGallery()) {
      entries.add(getString(R.string.pick_from_gallery));
      actions.add(this::pickFromGallery);
    }
    entries.add(getString(R.string.pick_from_storage));
    actions.add(this::pickFromStorage);
    return dialogBuilder
        .newDialog()
        .setItems(entries, (dialog, which) -> actions.get(which).run())
        .show();
  }

  private void takePicture() {
    getTargetFragment().startActivityForResult(new Intent(context, CameraActivity.class), REQUEST_CAMERA);
  }

  private void recordNote() {
    newRecordAudioDialog((FilesControlSet) getTargetFragment(), REQUEST_AUDIO)
        .show(getFragmentManager(), FRAG_TAG_RECORD_AUDIO);
  }

  private void pickFromGallery() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(Media.EXTERNAL_CONTENT_URI, "image/*");
    if (intent.resolveActivity(context.getPackageManager()) != null) {
      getTargetFragment().startActivityForResult(intent, REQUEST_GALLERY);
    }
  }

  public void pickFromStorage() {
    getTargetFragment().startActivityForResult(new Intent(context, FileExplore.class), REQUEST_STORAGE);
  }
}
