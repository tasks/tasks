package com.todoroo.astrid.files;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.todoroo.aacenc.RecognizerApi;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.ui.PopupControlSet;

public class FilesControlSet extends PopupControlSet {

    @Autowired
    private MetadataService metadataService;

    private final ArrayList<File> files = new ArrayList<File>();

    public FilesControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void refreshDisplayView() {
        LinearLayout display = (LinearLayout) getDisplayView();
        display.removeAllViews();
        for (final File f : files) {
            TextView textView = new TextView(activity);
            String name = f.getName();
            textView.setText(name);
            if (name.contains("audio")) {
                textView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecognizerApi.play(f.getAbsolutePath());
                    }
                });
            }
            display.addView(textView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
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

}
