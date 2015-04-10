package org.tasks.activities;

import android.os.Bundle;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public class ExportTaskActivity extends InjectingActivity {

    @Inject TasksXmlExporter xmlExporter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        xmlExporter.exportTasks(ExportTaskActivity.this, TasksXmlExporter.ExportType.EXPORT_TYPE_MANUAL);
    }
}
