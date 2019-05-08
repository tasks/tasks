package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.todoroo.astrid.backup.TasksXmlImporter;
import javax.inject.Inject;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.backup.TasksJsonImporter;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;
import org.tasks.ui.Toaster;

public class ImportTasksDialog extends InjectingNativeDialogFragment {

  private static final String EXTRA_URI = "extra_uri";
  private static final String EXTRA_EXTENSION = "extra_extension";

  @Inject TasksXmlImporter xmlImporter;
  @Inject TasksJsonImporter jsonImporter;
  @Inject DialogBuilder dialogBuilder;
  @Inject Tracker tracker;
  @Inject @ForApplication Context context;
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
        jsonImporter.importTasks(getActivity(), data, progressDialog);
        tracker.reportEvent(Tracking.Events.IMPORT_JSON);
        break;
      case "xml":
        xmlImporter.importTasks(getActivity(), data, progressDialog);
        tracker.reportEvent(Tracking.Events.IMPORT_XML);
        break;
      default:
        throw new RuntimeException("Invalid extension: " + extension);
    }
    return progressDialog;
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }
}
