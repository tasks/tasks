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

    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra(MainActivity.OPEN_TASK, taskId);
    startActivity(intent);

    finish();
  }
}
