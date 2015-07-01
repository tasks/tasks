package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.todoroo.astrid.backup.FilePickerBuilder;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

public class ImportTaskActivity extends InjectingAppCompatActivity {

    @Inject TasksXmlImporter xmlImporter;
    @Inject
    ActivityPreferences preferences;

    private boolean initiatedImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog filePicker =
                new FilePickerBuilder(this, R.string.import_file_prompt, preferences.getBackupDirectory(), preferences.getDialogTheme())
                        .setOnFilePickedListener(new FilePickerBuilder.OnFilePickedListener() {
                            @Override
                            public void onFilePicked(String filePath) {
                                initiatedImport = true;
                                xmlImporter.importTasks(ImportTaskActivity.this, filePath, new Runnable() {
                                    @Override
                                    public void run() {
                                        Flags.set(Flags.REFRESH);
                                        finish();
                                    }
                                });
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        }).show();
        filePicker.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!initiatedImport) {
                    finish();
                }
            }
        });
    }
}
