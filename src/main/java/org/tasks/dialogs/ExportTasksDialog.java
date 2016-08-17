package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import javax.inject.Inject;

public class ExportTasksDialog extends InjectingNativeDialogFragment {

    public static ExportTasksDialog newExportTasksDialog() {
        return new ExportTasksDialog();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject TasksXmlExporter tasksXmlExporter;

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
        tasksXmlExporter.exportTasks(getActivity(), TasksXmlExporter.ExportType.EXPORT_TYPE_MANUAL, progressDialog);
        return progressDialog;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
