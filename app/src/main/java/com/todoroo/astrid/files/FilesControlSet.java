/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Task;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.AddAttachmentActivity;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.files.FileHelper;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.ui.TaskEditControlFragment;
import timber.log.Timber;

public class FilesControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_files_pref;

  private static final char LEFT_TO_RIGHT_MARK = '\u200e';
  private static final int REQUEST_ADD_ATTACHMENT = 50;

  @Inject TaskAttachmentDao taskAttachmentDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;

  @BindView(R.id.attachment_container)
  LinearLayout attachmentContainer;

  @BindView(R.id.add_attachment)
  TextView addAttachment;

  private String taskUuid;

  private static void play(String file, PlaybackExceptionHandler handler) {
    MediaPlayer mediaPlayer = new MediaPlayer();

    try {
      mediaPlayer.setDataSource(file);
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch (Exception e) {
      Timber.e(e);
      handler.playbackFailed();
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    taskUuid = task.getUuid();

    final List<TaskAttachment> files = new ArrayList<>();
    for (TaskAttachment attachment : taskAttachmentDao.getAttachments(taskUuid)) {
      files.add(attachment);
      addAttachment(attachment);
    }
    validateFiles(files);
    return view;
  }

  @OnClick(R.id.add_attachment)
  void addAttachment(View view) {
    startActivityForResult(
        new Intent(context, AddAttachmentActivity.class), REQUEST_ADD_ATTACHMENT);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_files;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_attachment_24dp;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public void apply(Task task) {}

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ADD_ATTACHMENT) {
      if (resultCode == Activity.RESULT_OK) {
        String path = data.getStringExtra(AddAttachmentActivity.EXTRA_PATH);
        String type = data.getStringExtra(AddAttachmentActivity.EXTRA_TYPE);
        File file = new File(path);
        createNewFileAttachment(path, file.getName(), type);
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void addAttachment(TaskAttachment taskAttachment) {
    View fileRow =
        getActivity().getLayoutInflater().inflate(R.layout.file_row, attachmentContainer, false);
    fileRow.setTag(taskAttachment);
    attachmentContainer.addView(fileRow);
    addAttachment(taskAttachment, fileRow);
  }

  private void addAttachment(final TaskAttachment taskAttachment, final View fileRow) {
    TextView nameView = fileRow.findViewById(R.id.file_text);
    String name = LEFT_TO_RIGHT_MARK + taskAttachment.getName();
    nameView.setText(name);
    nameView.setOnClickListener(v -> showFile(taskAttachment));
    View clearFile = fileRow.findViewById(R.id.clear);
    clearFile.setOnClickListener(
        v ->
            dialogBuilder
                .newMessageDialog(R.string.premium_remove_file_confirm)
                .setPositiveButton(
                    android.R.string.ok,
                    (dialog, which) -> {
                      taskAttachmentDao.delete(taskAttachment);
                      if (!Strings.isNullOrEmpty(taskAttachment.getPath())) {
                        File f = new File(taskAttachment.getPath());
                        f.delete();
                      }
                      attachmentContainer.removeView(fileRow);
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
  }

  private void validateFiles(List<TaskAttachment> files) {
    for (int i = 0; i < files.size(); i++) {
      TaskAttachment m = files.get(i);
      if (!Strings.isNullOrEmpty(m.getPath())) {
        File f = new File(m.getPath());
        if (!f.exists()) {
          m.setPath(""); // $NON-NLS-1$
          // No local file and no url -- delete the metadata
          taskAttachmentDao.delete(m);
          files.remove(i);
          i--;
        }
      }
    }
  }

  public void hideAddAttachmentButton() {
    addAttachment.setVisibility(View.GONE);
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @SuppressLint("NewApi")
  private void showFile(final TaskAttachment m) {
    final String fileType =
        !Strings.isNullOrEmpty(m.getContentType())
            ? m.getContentType()
            : TaskAttachment.FILE_TYPE_OTHER;
    final String filePath = m.getPath();

    if (fileType.startsWith(TaskAttachment.FILE_TYPE_AUDIO)) {
      play(m.getPath(), () -> showFromIntent(filePath, fileType));
    } else if (fileType.startsWith(TaskAttachment.FILE_TYPE_IMAGE)) {
      try {
        Intent intent =
            FileHelper.getReadableActionView(
                context, filePath, TaskAttachment.FILE_TYPE_IMAGE + "*");
        getActivity().startActivity(intent);
      } catch (ActivityNotFoundException e) {
        Timber.e(e);
        Toast.makeText(context, R.string.no_application_found, Toast.LENGTH_SHORT).show();
      }
    } else {
      String useType = fileType;
      if (fileType.equals(TaskAttachment.FILE_TYPE_OTHER)) {
        String extension = AndroidUtilities.getFileExtension(filePath);

        MimeTypeMap map = MimeTypeMap.getSingleton();
        String guessedType = map.getMimeTypeFromExtension(extension);
        if (!TextUtils.isEmpty(guessedType)) {
          useType = guessedType;
        }
        if (!useType.equals(guessedType)) {
          m.setContentType(useType);
          taskAttachmentDao.update(m);
        }
      }
      showFromIntent(filePath, useType);
    }
  }

  private void showFromIntent(String file, String type) {
    try {
      Intent intent = FileHelper.getReadableActionView(context, file, type);
      getActivity().startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Timber.e(e);
      Toast.makeText(context, R.string.file_type_unhandled, Toast.LENGTH_LONG).show();
    }
  }

  private void createNewFileAttachment(String path, String fileName, String fileType) {
    TaskAttachment attachment =
        TaskAttachment.createNewAttachment(taskUuid, path, fileName, fileType);
    taskAttachmentDao.createNew(attachment);
    addAttachment(attachment);
  }

  interface PlaybackExceptionHandler {

    void playbackFailed();
  }
}
