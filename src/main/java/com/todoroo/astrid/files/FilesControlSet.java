/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.ActFmCameraModule;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.helper.TaskEditControlSetBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.DeviceInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesControlSet extends TaskEditControlSetBase {

    private static final Logger log = LoggerFactory.getLogger(FilesControlSet.class);

    private final ArrayList<TaskAttachment> files = new ArrayList<>();
    private final LayoutInflater inflater;
    private ActivityPreferences preferences;
    private final TaskAttachmentDao taskAttachmentDao;
    private final Fragment fragment;
    private final DeviceInfo deviceInfo;
    private final ActFmCameraModule actFmCameraModule;
    private final DialogBuilder dialogBuilder;
    private LinearLayout attachmentContainer;
    private TextView addAttachment;

    public FilesControlSet(ActivityPreferences preferences, TaskAttachmentDao taskAttachmentDao,
                           Fragment fragment, DeviceInfo deviceInfo, ActFmCameraModule actFmCameraModule) {
        super(fragment.getActivity(), R.layout.control_set_files);
        this.preferences = preferences;
        this.taskAttachmentDao = taskAttachmentDao;
        this.fragment = fragment;
        this.deviceInfo = deviceInfo;
        this.actFmCameraModule = actFmCameraModule;
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
                startAttachFile();
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
            log.error(e.getMessage(), e);
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
                activity.startActivity(new Intent(Intent.ACTION_VIEW) {{
                    setDataAndType(Uri.fromFile(new File(filePath)), fileType);
                }});
            } catch(ActivityNotFoundException e) {
                log.error(e.getMessage(), e);
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
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            log.error(e.getMessage(), e);
            Toast.makeText(activity, R.string.file_type_unhandled, Toast.LENGTH_LONG).show();
        }
    }

    private void startAttachFile() {
        final List<Runnable> runnables = new ArrayList<>();
        List<String> options = new ArrayList<>();

        if (deviceInfo.hasCamera() || deviceInfo.hasGallery()) {
            runnables.add(new Runnable() {
                @Override
                public void run() {
                    actFmCameraModule.showPictureLauncher(null);
                }
            });
            options.add(activity.getString(R.string.file_add_picture));
        }
        runnables.add(new Runnable() {
            @Override
            public void run() {
                Intent attachFile = new Intent(activity, FileExplore.class);
                fragment.startActivityForResult(attachFile, TaskEditFragment.REQUEST_CODE_ATTACH_FILE);
            }
        });
        options.add(activity.getString(R.string.file_add_sdcard));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                options.toArray(new String[options.size()]));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                runnables.get(which).run();
            }
        };

        // show a menu of available options
        dialogBuilder.newDialog()
                .setAdapter(adapter, listener)
                .show()
                .setOwnerActivity(activity);
    }

    public void attachFile(String file) {
        File src = new File(file);
        if (!src.exists()) {
            Toast.makeText(activity, R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return;
        }

        File dst = new File(preferences.getAttachmentsDirectory() + File.separator + src.getName());
        try {
            AndroidUtilities.copyFile(src, dst);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Toast.makeText(activity, R.string.file_err_copy, Toast.LENGTH_LONG).show();
            return;
        }

        String path = dst.getAbsolutePath();
        String name = dst.getName();
        String extension = AndroidUtilities.getFileExtension(name);

        String type = TaskAttachment.FILE_TYPE_OTHER;
        if (!TextUtils.isEmpty(extension)) {
            MimeTypeMap map = MimeTypeMap.getSingleton();
            String guessedType = map.getMimeTypeFromExtension(extension);
            if (!TextUtils.isEmpty(guessedType)) {
                type = guessedType;
            }
        }

        createNewFileAttachment(path, name, type);
    }

    public void createNewFileAttachment(String path, String fileName, String fileType) {
        TaskAttachment attachment = TaskAttachment.createNewAttachment(model.getUuid(), path, fileName, fileType);
        taskAttachmentDao.createNew(attachment);
        refreshMetadata();
        addAttachment(attachment);
    }
}
