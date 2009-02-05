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
}
