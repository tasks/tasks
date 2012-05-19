package com.todoroo.astrid.files;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.view.Gravity;
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
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

public class FilesControlSet extends PopupControlSet {

    @Autowired
    private MetadataService metadataService;

    private final ArrayList<Metadata> files = new ArrayList<Metadata>();
    private final LinearLayout fileList;

    public FilesControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);

        displayText.setText(activity.getString(R.string.TEA_control_files));
        fileList = (LinearLayout) getDisplayView().findViewById(R.id.files_list);
    }

    @Override
    protected void refreshDisplayView() {
        fileList.removeAllViews();
        for (final Metadata m : files) {
            TextView textView = new TextView(activity);
            String name = parseName(m);
            textView.setText(name);
            textView.setTextAppearance(activity, R.style.TextAppearance_EditRowDisplay);
            if (m.getValue(FileMetadata.FILE_TYPE) == FileMetadata.FILE_TYPE_AUDIO) {
                textView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecognizerApi.play(m.getValue(FileMetadata.FILE_PATH));
                    }
                });
            }

            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.RIGHT;
            fileList.addView(textView, lp);
        }
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
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
        refreshDisplayView();
    }

    @Override
    protected void readFromTaskOnInitialize() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void afterInflate() {
        // TODO Auto-generated method stub
    }

    @Override
    protected OnClickListener getDisplayClickListener() {
        return null;
    }

    @SuppressWarnings("nls")
    private String parseName(Metadata metadata) {
        int prefix = 0;
        switch(metadata.getValue(FileMetadata.FILE_TYPE)) {
        case FileMetadata.FILE_TYPE_AUDIO:
            prefix = R.string.files_type_audio;
            break;
        case FileMetadata.FILE_TYPE_PDF:
            prefix = R.string.files_type_pdf;
            break;
        }

        String prefixStr = "";
        if (prefix > 0) {
            prefixStr = activity.getString(prefix) + " ";
        }

        long date = metadata.getValue(FileMetadata.ATTACH_DATE);
        return prefixStr + DateUtilities.getDateString(activity, new Date(date));
    }

}
