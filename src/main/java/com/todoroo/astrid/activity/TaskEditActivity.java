package com.todoroo.astrid.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TaskEditActivity extends Activity {

    private static final String TOKEN_ID = "id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long taskId = getIntent().getLongExtra(TOKEN_ID, 0);

        startActivity(new Intent(this, TaskListActivity.class) {{
            putExtra(TaskListActivity.OPEN_TASK, taskId);
        }});

        finish();
    }
}
