package com.todoroo.astrid.files;

import java.io.File;
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

    private final ArrayList<File> files = new ArrayList<File>();
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
        for (final File f : files) {
            TextView textView = new TextView(activity);
            String name = parseName(f.getName());
            textView.setText(name);
            textView.setTextAppearance(activity, R.style.TextAppearance_EditRowDisplay);
            if (name.contains("audio")) { //$NON-NLS-1$
                textView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecognizerApi.play(f.getAbsolutePath());
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
            Metadata metadata = new Metadata();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);
                File file = new File(metadata.getValue(FileMetadata.FILE_PATH));
                files.add(file);
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

    private String parseName(String filename) {
        String[] components = filename.split("_");
        long date = Long.parseLong(components[1]);
        String dateString = DateUtilities.getDateString(activity, new Date(date));
        return components[2] + " " + dateString;
    }

}
