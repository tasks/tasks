package com.timsu.astrid.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TaskListNotify extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, TaskList.class);
        intent.putExtra(TaskList.VARIABLES_TAG, getIntent().getExtras());
        startActivity(intent);

        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Intent taskListIntent = new Intent(this, TaskList.class);
        taskListIntent.putExtra(TaskList.VARIABLES_TAG, intent.getExtras());
        startActivity(taskListIntent);

        finish();
    }
}
