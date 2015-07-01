package org.tasks.activities;

import android.os.Bundle;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingAppCompatActivity;

import javax.inject.Inject;

public class ExportTaskActivity extends InjectingAppCompatActivity {

    @Inject TasksXmlExporter xmlExporter;
    @Inject DialogBuilder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        xmlExporter.exportTasks(ExportTaskActivity.this, TasksXmlExporter.ExportType.EXPORT_TYPE_MANUAL, dialogBuilder);
    }
}
