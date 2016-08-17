package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.todoroo.astrid.backup.TasksXmlImporter;

import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import javax.inject.Inject;

public class ImportTasksDialog extends InjectingNativeDialogFragment {

    public static ImportTasksDialog newImportTasksDialog(String path) {
        ImportTasksDialog importTasksDialog = new ImportTasksDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_PATH, path);
        importTasksDialog.setArguments(args);
        return importTasksDialog;
    }

    private static final String EXTRA_PATH = "extra_path";

    @Inject TasksXmlImporter xmlImporter;
    @Inject DialogBuilder dialogBuilder;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        String path = arguments.getString(EXTRA_PATH);
        ProgressDialog progressDialog = dialogBuilder.newProgressDialog();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        setCancelable(false);
        xmlImporter.importTasks(getActivity(), path, progressDialog);
        return progressDialog;
    }

    @Override
    protected void inject(NativeDialogFragmentComponent component) {
        component.inject(this);
    }
}
