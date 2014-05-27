/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.voice.RecognizerApi;
import com.todoroo.astrid.voice.RecognizerApi.PlaybackExceptionHandler;

import org.tasks.R;
import org.tasks.injection.Injector;

import java.io.File;
import java.util.ArrayList;

import javax.inject.Inject;

public class FilesControlSet extends PopupControlSet {

    @Inject TaskAttachmentDao taskAttachmentDao;

    private final ArrayList<TaskAttachment> files = new ArrayList<>();
    private final LinearLayout fileDisplayList;
    private final LayoutInflater inflater;
    private final ImageView image;

    public FilesControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        ((Injector) activity.getApplication()).inject(this);

        fileDisplayList = (LinearLayout) getDisplayView().findViewById(R.id.files_list);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
        inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void refreshDisplayView() {
        fileDisplayList.removeAllViews();
        if (files != null && files.size() > 0) {
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_files, R.drawable.tea_icn_files_lightblue));
        } else {
            image.setImageResource(R.drawable.tea_icn_files_gray);
        }
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

    public void refreshMetadata() {
        if (model != null) {
            TodorooCursor<TaskAttachment> cursor = taskAttachmentDao.query(
                    Query.select(TaskAttachment.PROPERTIES)
                    .where(Criterion.and(TaskAttachment.TASK_UUID.eq(model.getUuid()),
                            TaskAttachment.DELETED_AT.eq(0))));
            try {
                files.clear();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    TaskAttachment attachment = new TaskAttachment();
                    attachment.readFromCursor(cursor);
                    files.add(attachment);
                }
            } finally {
                cursor.close();
            }
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
                    if (m.containsNonNullValue(TaskAttachment.URL)) { // We're ok, just the local file was deleted
                        taskAttachmentDao.saveExisting(m);
                    } else { // No local file and no url -- delete the metadata
                        taskAttachmentDao.delete(m.getId());
                        files.remove(i);
                        i--;
                    }
                }
            }

        }
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // Nothing to write
    }

    @Override
    protected void afterInflate() {
        LinearLayout fileList = (LinearLayout) getView().findViewById(R.id.files_list);
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

    private void showFile(final TaskAttachment m) {
        final String fileType = m.containsNonNullValue(TaskAttachment.CONTENT_TYPE) ? m.getContentType() : TaskAttachment.FILE_TYPE_OTHER;
        final String filePath = m.getFilePath();

        if (fileType.startsWith(TaskAttachment.FILE_TYPE_AUDIO)) {
            RecognizerApi.play(m.getFilePath(), new PlaybackExceptionHandler() {
                @Override
                public void playbackFailed(String file) {
                    showFromIntent(filePath, fileType);
                }
            });
        } else if (fileType.startsWith(TaskAttachment.FILE_TYPE_IMAGE)) {
            AlertDialog image = new AlertDialog.Builder(activity).create();
            ImageView imageView = new ImageView(activity);
            imageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            Bitmap bitmap = AndroidUtilities.readScaledBitmap(filePath);

            if (bitmap == null) {
                Toast.makeText(activity, R.string.file_err_memory, Toast.LENGTH_LONG).show();
                return;
            }

            imageView.setImageBitmap(bitmap);
            image.setView(imageView);

            image.setButton(activity.getString(R.string.DLG_close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                }
            });
            image.show();
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
