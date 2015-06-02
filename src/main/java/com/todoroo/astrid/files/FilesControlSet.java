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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.utility.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.io.File;
import java.util.ArrayList;

public class FilesControlSet extends PopupControlSet {

    private static final Logger log = LoggerFactory.getLogger(FilesControlSet.class);

    private final ArrayList<TaskAttachment> files = new ArrayList<>();
    private final LinearLayout fileDisplayList;
    private final LayoutInflater inflater;
    private final TaskAttachmentDao taskAttachmentDao;

    public FilesControlSet(ActivityPreferences preferences, TaskAttachmentDao taskAttachmentDao, Activity activity) {
        super(preferences, activity, R.layout.control_set_files_dialog, R.layout.control_set_files, R.string.TEA_control_files);
        this.taskAttachmentDao = taskAttachmentDao;
        fileDisplayList = (LinearLayout) getView().findViewById(R.id.files_list);
        inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void refreshDisplayView() {
        fileDisplayList.removeAllViews();
        for (final TaskAttachment m : files) {
            View fileRow = inflater.inflate(R.layout.file_display_row, null);
            LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            setUpFileRow(m, fileRow, fileDisplayList, lp);
        }
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);

        refreshMetadata();
        refreshDisplayView();
    }

    @Override
    public int getIcon() {
        return R.attr.ic_action_attachment;
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
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // Nothing to write
    }

    @Override
    protected void afterInflate() {
        LinearLayout fileList = (LinearLayout) getDialogView().findViewById(R.id.files_list);
        final LinearLayout finalList = fileList;
        fileList.removeAllViews();
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        for (final TaskAttachment m : files) {
            final View fileRow = inflater.inflate(R.layout.file_row, null);

            setUpFileRow(m, fileRow, fileList, lp);
            View name = fileRow.findViewById(R.id.file_text);
            View clearFile = fileRow.findViewById(R.id.remove_file);

            name.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFile(m);
                }
            });

            clearFile.setVisibility(View.VISIBLE);
            clearFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogUtilities.okCancelDialog(activity, activity.getString(R.string.premium_remove_file_confirm),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            if (RemoteModel.isValidUuid(m.getUUID())) {
                                // TODO: delete
                                m.setDeletedAt(DateUtilities.now());
                                taskAttachmentDao.saveExisting(m);
                            } else {
                                taskAttachmentDao.delete(m.getId());
                            }

                            if (m.containsNonNullValue(TaskAttachment.FILE_PATH)) {
                                File f = new File(m.getFilePath());
                                f.delete();
                            }
                            files.remove(m);
                            refreshDisplayView();
                            finalList.removeView(fileRow);
                        }
                    }, null);
                }
            });
        }
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
            activity.startActivity(new Intent(Intent.ACTION_VIEW) {{
                setDataAndType(Uri.fromFile(new File(filePath)), fileType);
            }});
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
            handleActivityNotFound(type);
        }
    }

    private void handleActivityNotFound(String fileType) {
        if (fileType.startsWith(TaskAttachment.FILE_TYPE_AUDIO)) {
            searchMarket("com.clov4r.android.nil", R.string.search_market_audio_title, R.string.search_market_audio); //$NON-NLS-1$
        } else if (fileType.equals(TaskAttachment.FILE_TYPE_PDF)) {
            searchMarket("com.adobe.reader", R.string.search_market_pdf_title, R.string.search_market_pdf); //$NON-NLS-1$
        } else if (AndroidUtilities.indexOf(TaskAttachment.MS_FILETYPES, fileType) >= 0) {
            searchMarket("com.dataviz.docstogo", R.string.search_market_ms_title, R.string.search_market_ms); //$NON-NLS-1$
        } else {
            DialogUtilities.okDialog(activity, activity.getString(R.string.file_type_unhandled_title),
                    0, activity.getString(R.string.file_type_unhandled));
        }
    }

    private void searchMarket(final String packageName, int title, int body) {
        DialogUtilities.okCancelDialog(activity, activity.getString(title),
                activity.getString(body), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                Intent marketIntent = Constants.MARKET_STRATEGY.generateMarketLink(packageName);
                try {
                    if (marketIntent == null) {
                        throw new ActivityNotFoundException("No market link supplied"); //$NON-NLS-1$
                    }
                    activity.startActivity(marketIntent);
                } catch (ActivityNotFoundException anf) {
                    log.error(anf.getMessage(), anf);
                    DialogUtilities.okDialog(activity,
                            activity.getString(R.string.market_unavailable),
                            null);
                }
            }
        });
    }

    private void setUpFileRow(TaskAttachment m, View row, LinearLayout parent, LayoutParams lp) {
        TextView nameView = (TextView) row.findViewById(R.id.file_text);
        nameView.setTextColor(themeColor);
        TextView typeView = (TextView) row.findViewById(R.id.file_type);
        String name = getNameString(m);
        String type = getTypeString(m.getName());
        nameView.setText(name);

        if (TextUtils.isEmpty(type)) {
            typeView.setVisibility(View.GONE);
        } else {
            typeView.setText(type);
        }

        parent.addView(row, lp);
    }

    private String getNameString(TaskAttachment metadata) {
        String name = metadata.getName();
        int extension = name.lastIndexOf('.');
        if (extension < 0) {
            return name;
        }
        return name.substring(0, extension);
    }

    private String getTypeString(String name) {
        int extension = name.lastIndexOf('.');
        if (extension < 0 || extension + 1 >= name.length()) {
            return "";
        }
        return name.substring(extension + 1).toUpperCase();
    }
}
