/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.tasks.R;
import org.tasks.activities.AddAttachmentActivity;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.ActivityPreferences;

import java.io.File;
import java.util.ArrayList;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class FilesControlSet extends TaskEditControlSetBase {

    private final ArrayList<TaskAttachment> files = new ArrayList<>();
    private final LayoutInflater inflater;
    private final TaskAttachmentDao taskAttachmentDao;
    private final Fragment fragment;
    private final DialogBuilder dialogBuilder;
    private LinearLayout attachmentContainer;
    private TextView addAttachment;

    public FilesControlSet(ActivityPreferences preferences, TaskAttachmentDao taskAttachmentDao,
                           Fragment fragment) {
        super(fragment.getActivity(), R.layout.control_set_files);
        this.taskAttachmentDao = taskAttachmentDao;
        this.fragment = fragment;
        this.dialogBuilder = new DialogBuilder(activity, preferences);
        inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    private void addAttachment(TaskAttachment taskAttachment) {
        View fileRow = inflater.inflate(R.layout.file_row, null);
        fileRow.setTag(taskAttachment);
        attachmentContainer.addView(fileRow);
        addAttachment(taskAttachment, fileRow);
    }

    private void addAttachment(final TaskAttachment taskAttachment, final View fileRow) {
        TextView nameView = (TextView) fileRow.findViewById(R.id.file_text);
        nameView.setTextColor(themeColor);
        String name = taskAttachment.getName();
        nameView.setText(name);
        nameView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFile(taskAttachment);
            }
        });
        View clearFile = fileRow.findViewById(R.id.remove_file);
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
                                files.remove(taskAttachment);
                                attachmentContainer.removeView(fileRow);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_attachment_24dp;
    }

    public void refreshMetadata() {
        if (model != null) {
            files.clear();
            taskAttachmentDao.getAttachments(model.getUuid(), new Callback<TaskAttachment>() {
                @Override
                public void apply(TaskAttachment attachment) {
                    files.add(attachment);
                }
            });
            validateFiles();
            if (initialized) {
                afterInflate();
            }
        }
    }

    private void validateFiles() {
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

    @Override
    protected void readFromTaskOnInitialize() {
        attachmentContainer.removeAllViews();
        taskAttachmentDao.getAttachments(model.getUuid(), new Callback<TaskAttachment>() {
            @Override
            public void apply(TaskAttachment entry) {
                addAttachment(entry);
            }
        });
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // Nothing to write
    }

    @Override
    protected void afterInflate() {
        attachmentContainer = (LinearLayout) getView().findViewById(R.id.attachment_container);
        addAttachment = (TextView) getView().findViewById(R.id.add_attachment);
        addAttachment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                fragment.startActivityForResult(new Intent(activity, AddAttachmentActivity.class), TaskEditFragment.REQUEST_ADD_ATTACHMENT);
            }
        });
    }

    public void hideAddAttachmentButton() {
        addAttachment.setVisibility(View.GONE);
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
                activity.startActivity(intent);
            } catch(ActivityNotFoundException e) {
                Timber.e(e, e.getMessage());
                Toast.makeText(activity, R.string.no_application_found, Toast.LENGTH_SHORT).show();
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
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(activity, R.string.file_type_unhandled, Toast.LENGTH_LONG).show();
        }
    }

    public void createNewFileAttachment(String path, String fileName, String fileType) {
        TaskAttachment attachment = TaskAttachment.createNewAttachment(model.getUuid(), path, fileName, fileType);
        taskAttachmentDao.createNew(attachment);
        refreshMetadata();
        addAttachment(attachment);
    }
}
