package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.backup.FilePickerBuilder;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public class ImportTaskActivity extends InjectingActivity {

    @Inject TasksXmlImporter xmlImporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FilePickerBuilder filePickerBuilder = new FilePickerBuilder(this,
                R.string.import_file_prompt, BackupConstants.defaultExportDirectory());
        filePickerBuilder.setOnFilePickedListener(new FilePickerBuilder.OnFilePickedListener() {
            @Override
            public void onFilePicked(String filePath) {
                xmlImporter.importTasks(ImportTaskActivity.this, filePath, new Runnable() {
                    @Override
                    public void run() {
                        Flags.set(Flags.REFRESH);
                        finish();
                    }
                });
            }
        });
        filePickerBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        filePickerBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        filePickerBuilder.show();
    }
}
