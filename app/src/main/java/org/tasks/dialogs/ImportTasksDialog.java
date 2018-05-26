package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import com.todoroo.astrid.backup.TasksXmlImporter;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.backup.TasksJsonImporter;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

public class ImportTasksDialog extends InjectingNativeDialogFragment {

  private static final String EXTRA_PATH = "extra_path";
  @Inject TasksXmlImporter xmlImporter;
  @Inject TasksJsonImporter jsonImporter;
  @Inject DialogBuilder dialogBuilder;
  @Inject Tracker tracker;

  public static ImportTasksDialog newImportTasksDialog(String path) {
    ImportTasksDialog importTasksDialog = new ImportTasksDialog();
    Bundle args = new Bundle();
    args.putString(EXTRA_PATH, path);
    importTasksDialog.setArguments(args);
    return importTasksDialog;
  }

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
    if (path.endsWith(".xml")) {
      xmlImporter.importTasks(getActivity(), path, progressDialog);
      tracker.reportEvent(Tracking.Events.IMPORT_XML);
    } else {
      jsonImporter.importTasks(getActivity(), path, progressDialog);
      tracker.reportEvent(Tracking.Events.IMPORT_JSON);
    }
    return progressDialog;
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }
}
