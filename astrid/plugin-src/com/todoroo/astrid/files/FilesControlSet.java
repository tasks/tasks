package com.todoroo.astrid.files;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.aacenc.RecognizerApi;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

public class FilesControlSet extends PopupControlSet {

    @Autowired
    private MetadataService metadataService;

    private final ArrayList<Metadata> files = new ArrayList<Metadata>();
    private final LinearLayout fileDisplayList;
    private LinearLayout fileList;
    private final LayoutInflater inflater;

    public FilesControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);

        displayText.setText(activity.getString(R.string.TEA_control_files));
        fileDisplayList = (LinearLayout) getDisplayView().findViewById(R.id.files_list);
        inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected void refreshDisplayView() {
        fileDisplayList.removeAllViews();
        for (final Metadata m : files) {
            View fileRow = inflater.inflate(R.layout.file_display_row, null);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.RIGHT;
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
        TodorooCursor<Metadata> cursor = metadataService.query(
                Query.select(Metadata.PROPERTIES)
                     .where(MetadataCriteria.byTaskAndwithKey(model.getId(), FileMetadata.METADATA_KEY)));
        try {
            files.clear();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata metadata = new Metadata();
                metadata.readFromCursor(cursor);
                files.add(metadata);
            }
        } finally {
            cursor.close();
        }
        if (initialized)
            afterInflate();
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // Nothing to write
        return null;
    }

    @Override
    protected void afterInflate() {
        fileList = (LinearLayout) getView().findViewById(R.id.files_list);
        final LinearLayout finalList = fileList;
        fileList.removeAllViews();
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        for (final Metadata m : files) {
            final View fileRow = inflater.inflate(R.layout.file_row, null);

            setUpFileRow(m, fileRow, fileList, lp);
            View name = fileRow.findViewById(R.id.file_text);
            View clearFile = fileRow.findViewById(R.id.remove_file);
            clearFile.setVisibility(View.VISIBLE);
            if (m.getValue(FileMetadata.FILE_TYPE) == FileMetadata.FILE_TYPE_AUDIO) {
                name.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecognizerApi.play(m.getValue(FileMetadata.FILE_PATH));
                    }
                });
            }
            clearFile.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogUtilities.okCancelDialog(activity, activity.getString(R.string.premium_remove_file_confirm),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            File f = new File(m.getValue(FileMetadata.FILE_PATH));
                            if (f.delete()) {
                                metadataService.delete(m);
                                files.remove(m);
                                refreshDisplayView();
                                finalList.removeView(fileRow);
                            }
                        }
                    }, null);
                }
            });
        }
    }

    private void setUpFileRow(Metadata m, View row, LinearLayout parent, LayoutParams lp) {
        TextView nameView = (TextView) row.findViewById(R.id.file_text);
        TextView typeView = (TextView) row.findViewById(R.id.file_type);
        String name = getNameString(m);
        String type = getTypeString(m);
        nameView.setText(name);

        if (TextUtils.isEmpty(type))
            typeView.setVisibility(View.GONE);
        else
            typeView.setText(type);

        parent.addView(row, lp);
    }

    private String getNameString(Metadata metadata) {
        if (metadata.getValue(FileMetadata.FILE_TYPE) == FileMetadata.FILE_TYPE_AUDIO) {
            Date date = new Date(metadata.getValue(FileMetadata.ATTACH_DATE));
            return DateUtilities.getDateStringWithTime(activity, date);
        } else {
            File f = new File(metadata.getValue(FileMetadata.FILE_PATH));
            String name = f.getName();
            int extension = name.lastIndexOf('.');
            if (extension < 0)
                return name;
            return name.substring(0, extension);
        }
    }

    @SuppressWarnings("nls")
    private String getTypeString(Metadata metadata) {
        File f = new File(metadata.getValue(FileMetadata.FILE_PATH));
        String name = f.getName();

        int extension = name.lastIndexOf('.');
        if (extension < 0 || extension + 1 >= name.length())
            return "";
        return name.substring(extension + 1).toUpperCase();

    }

}
