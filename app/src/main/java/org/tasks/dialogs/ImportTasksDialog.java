package org.tasks.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.backup.TasksXmlImporter;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.backup.TasksJsonImporter;
import org.tasks.backup.TasksJsonImporter.ImportResult;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ActivityContext;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.ui.Toaster;

public class ImportTasksDialog extends InjectingDialogFragment {

  private static final String EXTRA_URI = "extra_uri";
  private static final String EXTRA_EXTENSION = "extra_extension";

  @Inject TasksXmlImporter xmlImporter;
  @Inject TasksJsonImporter jsonImporter;
  @Inject DialogBuilder dialogBuilder;
  @Inject @ActivityContext Context context;
  @Inject Toaster toaster;

  public static ImportTasksDialog newImportTasksDialog(Uri data, String extension) {
    ImportTasksDialog importTasksDialog = new ImportTasksDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_URI, data);
    args.putString(EXTRA_EXTENSION, extension);
    importTasksDialog.setArguments(args);
    return importTasksDialog;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle arguments = getArguments();
    Uri data = arguments.getParcelable(EXTRA_URI);
    String extension = arguments.getString(EXTRA_EXTENSION);
    ProgressDialog progressDialog = dialogBuilder.newProgressDialog();
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setCancelable(false);
    progressDialog.setIndeterminate(true);
    progressDialog.show();
    setCancelable(false);
    switch (extension) {
      case "json":
        Handler handler = new Handler();
        new Thread(
                () -> {
                  ImportResult result =
                      jsonImporter.importTasks(getActivity(), data, progressDialog);
                  handler.post(() -> {
                    if (progressDialog.isShowing()) {
                      DialogUtilities.dismissDialog((Activity) context, progressDialog);
                    }
                    showSummary(result);
                  });
                })
            .start();
        break;
      case "xml":
        xmlImporter.importTasks(getActivity(), data, progressDialog);
        break;
      default:
        throw new RuntimeException("Invalid extension: " + extension);
    }
    return progressDialog;
  }

  private void showSummary(ImportResult result) {
    Resources r = context.getResources();
    dialogBuilder
        .newDialog(R.string.import_summary_title)
        .setMessage(
            context.getString(
                R.string.import_summary_message,
                "",
                r.getQuantityString(R.plurals.Ntasks, result.taskCount, result.taskCount),
                r.getQuantityString(R.plurals.Ntasks, result.importCount, result.importCount),
                r.getQuantityString(R.plurals.Ntasks, result.skipCount, result.skipCount),
                r.getQuantityString(R.plurals.Ntasks, 0, 0)))
        .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss())
        .show();
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
