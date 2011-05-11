package com.todoroo.astrid.notes;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

public class NoteViewingActivity extends Activity {

    public static final String EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_linear_layout);
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        LinearLayout body = (LinearLayout) findViewById(R.id.body);

        task = getIntent().getParcelableExtra(EXTRA_TASK);
        setTitle(task.getValue(Task.TITLE));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout scrollViewBody = new LinearLayout(this);
        scrollViewBody.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(scrollViewBody);
        body.addView(scrollView);

        if(!TextUtils.isEmpty(task.getValue(Task.NOTES))) {
            TextView note = new TextView(this);
            note.setText(task.getValue(Task.NOTES));
            Linkify.addLinks(note, Linkify.ALL);
            note.setPadding(0, 0, 0, 10);
            scrollViewBody.addView(note);
        }

        TodorooCursor<Metadata> cursor = PluginServices.getMetadataService().query(
                Query.select(Metadata.PROPERTIES).where(
                        MetadataCriteria.byTaskAndwithKey(task.getId(),
                                NoteMetadata.METADATA_KEY)).orderBy(Order.desc(Metadata.CREATION_DATE)));
        Metadata metadata = new Metadata();
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                metadata.readFromCursor(cursor);

                TextView title = new TextView(this);
                title.setTypeface(Typeface.DEFAULT_BOLD);
                title.setText(metadata.getValue(NoteMetadata.TITLE));
                scrollViewBody.addView(title);

                TextView note = new TextView(this);
                note.setText(metadata.getValue(NoteMetadata.BODY));
                Linkify.addLinks(note, Linkify.ALL);
                note.setPadding(0, 0, 0, 10);
                scrollViewBody.addView(note);
            }
        } finally {
            cursor.close();
        }

        Button ok = new Button(this);
        ok.setText(android.R.string.ok);
        ok.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        body.addView(ok);
    }
}