package com.todoroo.astrid.backup;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;

public class BackupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setContext(this);
        setContentView(R.layout.backup_activity);
        setTitle(R.string.backup_BAc_title);

        ((Button)findViewById(R.id.importButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                importTasks();
            }
        });

        ((Button)findViewById(R.id.exportButton)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                exportTasks();
            }
        });
    }

    private void importTasks() {
        FilePickerBuilder.OnFilePickedListener listener = new FilePickerBuilder.OnFilePickedListener() {
            @Override
            public void onFilePicked(String filePath) {
                TasksXmlImporter.importTasks(BackupActivity.this, filePath,
                        new Runnable() {
                    @Override
                    public void run() {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        };
        new FilePickerBuilder(this,
                getString(R.string.import_file_prompt),
                BackupConstants.defaultExportDirectory(),
                listener).show();
    }

    private void exportTasks() {
        TasksXmlExporter.exportTasks(this, false, new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_OK);
                finish();
            }
        }, null);
    }

}
