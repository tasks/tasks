package com.todoroo.astrid.notes;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import com.timsu.astrid.R;
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

        TextView linkifiedTextView = new TextView(this);
        linkifiedTextView.setText(task.getValue(Task.NOTES) + "\n"); //$NON-NLS-1$
        Linkify.addLinks(linkifiedTextView, Linkify.ALL);

        scrollView.addView(linkifiedTextView);
        body.addView(scrollView);

        Button ok = new Button(this);
        ok.setText(android.R.string.ok);
        ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        body.addView(ok);
    }
}