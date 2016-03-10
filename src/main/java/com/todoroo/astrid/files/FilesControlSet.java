/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;

import org.tasks.R;
import org.tasks.activities.AddAttachmentActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ForActivity;
import org.tasks.injection.FragmentComponent;
import org.tasks.ui.TaskEditControlFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class FilesControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_files_pref;

    private static final int REQUEST_ADD_ATTACHMENT = 50;
    private static final String EXTRA_UUID = "extra_uuid";

    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject DialogBuilder dialogBuilder;
    @Inject @ForActivity Context context;

    @Bind(R.id.attachment_container) LinearLayout attachmentContainer;
    @Bind(R.id.add_attachment) TextView addAttachment;

    private String taskUuid;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            taskUuid = savedInstanceState.getString(EXTRA_UUID);
        }

        final List<TaskAttachment> files = new ArrayList<>();
        taskAttachmentDao.getAttachments(taskUuid, new Callback<TaskAttachment>() {
            @Override
            public void apply(TaskAttachment attachment) {
                files.add(attachment);
                addAttachment(attachment);
            }
        });
        validateFiles(files);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_UUID, taskUuid);
    }

    @OnClick(R.id.add_attachment)
    void addAttachment(View view) {
        startActivityForResult(new Intent(context, AddAttachmentActivity.class), REQUEST_ADD_ATTACHMENT);
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
    public void initialize(boolean isNewTask, Task task) {
        taskUuid = task.getUuid();
    }

    @Override
    public void apply(Task task) {

    }

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
        View fileRow = getActivity().getLayoutInflater().inflate(R.layout.file_row, null);
        fileRow.setTag(taskAttachment);
        attachmentContainer.addView(fileRow);
        addAttachment(taskAttachment, fileRow);
    }

    private void addAttachment(final TaskAttachment taskAttachment, final View fileRow) {
        TextView nameView = (TextView) fileRow.findViewById(R.id.file_text);
        String name = taskAttachment.getName();
        nameView.setText(name);
        nameView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFile(taskAttachment);
            }
        });
        View clearFile = fileRow.findViewById(R.id.clear);
        clearFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogBuilder.newMessageDialog(R.string.premium_remove_file_confirm)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                taskAttachmentDao.delete(taskAttachment.getId());
                                if (taskAttachment.containsNonNullValue(TaskAttachment.FILE_PATH)) {
                                    File f = new File(taskAttachment.getFilePath());
                                    f.delete();
                                }
                                attachmentContainer.removeView(fileRow);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
    }

    private void validateFiles(List<TaskAttachment> files) {
        for (int i = 0; i < files.size(); i++) {
            TaskAttachment m = files.get(i);
            if (m.containsNonNullValue(TaskAttachment.FILE_PATH)) {
                File f = new File(m.getFilePath());
                if (!f.exists()) {
                    m.setFilePath(""); //$NON-NLS-1$
                    // No local file and no url -- delete the metadata
                    taskAttachmentDao.delete(m.getId());
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

    public interface PlaybackExceptionHandler {
        void playbackFailed();
    }

    private static void play(String file, PlaybackExceptionHandler handler) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(file);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
            handler.playbackFailed();
        }
    }

    private void showFile(final TaskAttachment m) {
        final String fileType = m.containsNonNullValue(TaskAttachment.CONTENT_TYPE) ? m.getContentType() : TaskAttachment.FILE_TYPE_OTHER;
        final String filePath = m.getFilePath();

        if (fileType.startsWith(TaskAttachment.FILE_TYPE_AUDIO)) {
            play(m.getFilePath(), new PlaybackExceptionHandler() {
                @Override
                public void playbackFailed() {
                    showFromIntent(filePath, fileType);
                }
            });
        } else if (fileType.startsWith(TaskAttachment.FILE_TYPE_IMAGE)) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(filePath));
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setDataAndType(uri, TaskAttachment.FILE_TYPE_IMAGE + "*");
                if (atLeastLollipop()) {
                    intent.setClipData(ClipData.newRawUri(null, uri));
                }
                getActivity().startActivity(intent);
            } catch(ActivityNotFoundException e) {
                Timber.e(e, e.getMessage());
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
                    m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                    taskAttachmentDao.saveExisting(m);
                }
            }
            showFromIntent(filePath, useType);
        }
    }

    private void showFromIntent(String file, String type) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(file)), type);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(context, R.string.file_type_unhandled, Toast.LENGTH_LONG).show();
        }
    }

    public void createNewFileAttachment(String path, String fileName, String fileType) {
        TaskAttachment attachment = TaskAttachment.createNewAttachment(taskUuid, path, fileName, fileType);
        taskAttachmentDao.createNew(attachment);
        addAttachment(attachment);
    }
}
