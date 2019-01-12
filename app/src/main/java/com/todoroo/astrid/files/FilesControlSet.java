/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.files;

import static android.app.Activity.RESULT_OK;
import static org.tasks.data.TaskAttachment.createNewAttachment;
import static org.tasks.dialogs.AddAttachmentDialog.REQUEST_AUDIO;
import static org.tasks.dialogs.AddAttachmentDialog.REQUEST_CAMERA;
import static org.tasks.dialogs.AddAttachmentDialog.REQUEST_GALLERY;
import static org.tasks.dialogs.AddAttachmentDialog.REQUEST_STORAGE;
import static org.tasks.dialogs.AddAttachmentDialog.newAddAttachmentDialog;
import static org.tasks.files.FileHelper.copyToUri;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.OnClick;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.files.FileHelper;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.preferences.Preferences;
import org.tasks.ui.TaskEditControlFragment;

public class FilesControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_files_pref;

  private static final String FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog";
  private static final char LEFT_TO_RIGHT_MARK = '\u200e';

  @Inject TaskAttachmentDao taskAttachmentDao;
  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject Preferences preferences;

  @BindView(R.id.attachment_container)
  LinearLayout attachmentContainer;

  @BindView(R.id.add_attachment)
  TextView addAttachment;

  private String taskUuid;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    taskUuid = task.getUuid();

    for (TaskAttachment attachment : taskAttachmentDao.getAttachments(taskUuid)) {
      addAttachment(attachment);
    }

    if (savedInstanceState == null) {
      if (task.hasTransitory(TaskAttachment.KEY)) {
        for (Uri uri : (ArrayList<Uri>) task.getTransitory(TaskAttachment.KEY)) {
          copyToAttachmentDirectory(uri);
        }
      }
    }

    return view;
  }

  @OnClick(R.id.add_attachment)
  void addAttachment(View view) {
    newAddAttachmentDialog(this).show(getFragmentManager(), FRAG_TAG_ADD_ATTACHMENT_DIALOG);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_files;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_outline_attachment_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public void apply(Task task) {}

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CAMERA || requestCode == REQUEST_AUDIO) {
      if (resultCode == RESULT_OK) {
        Uri uri = data.getData();
        copyToAttachmentDirectory(uri);
        FileHelper.delete(context, uri);
      }
    } else if (requestCode == REQUEST_STORAGE || requestCode == REQUEST_GALLERY) {
      if (resultCode == RESULT_OK) {
        copyToAttachmentDirectory(data.getData());
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
                      FileHelper.delete(context, taskAttachment.parseUri());
                      attachmentContainer.removeView(fileRow);
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @SuppressLint("NewApi")
  private void showFile(final TaskAttachment m) {
    FileHelper.startActionView(getActivity(), m.parseUri());
  }

  private void copyToAttachmentDirectory(Uri input) {
    Uri output = copyToUri(context, preferences.getAttachmentsDirectory(), input);
    TaskAttachment attachment =
        createNewAttachment(taskUuid, output, FileHelper.getFilename(context, output));
    taskAttachmentDao.createNew(attachment);
    addAttachment(attachment);
  }
}
