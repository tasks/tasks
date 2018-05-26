package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.backup.TasksJsonExporter;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import javax.inject.Inject;

public class ExportTasksDialog extends InjectingNativeDialogFragment {

    public static ExportTasksDialog newExportTasksDialog() {
        return new ExportTasksDialog();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject TasksJsonExporter tasksJsonExporter;
    @Inject Tracker tracker;

    private ProgressDialog progressDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        progressDialog = dialogBuilder.newProgressDialog();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();

        setCancelable(false);
        tasksJsonExporter.exportTasks(getActivity(), TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL, progressDialog);
        tracker.reportEvent(Tracking.Events.EXPORT);
        return progressDialog;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
