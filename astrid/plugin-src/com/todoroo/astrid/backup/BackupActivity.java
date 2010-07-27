package com.todoroo.astrid.backup;

import android.app.Activity;
import android.os.Bundle;

import com.timsu.astrid.R;

public class BackupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_activity);
        setTitle(R.string.backup_BAc_title);


    }

}
