package org.tasks.dialogs;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.MimeTypeMap;

import com.todoroo.astrid.backup.TasksXmlImporter;

import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.backup.TasksJsonImporter;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingNativeDialogFragment;
import org.tasks.injection.NativeDialogFragmentComponent;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;

public class ImportTasksDialog extends InjectingNativeDialogFragment {

  private static final String EXTRA_URI = "extra_uri";
  @Inject TasksXmlImporter xmlImporter;
  @Inject TasksJsonImporter jsonImporter;
  @Inject DialogBuilder dialogBuilder;
  @Inject Tracker tracker;
  @Inject @ForApplication Context context;

  public static ImportTasksDialog newImportTasksDialog(Uri data) {
    ImportTasksDialog importTasksDialog = new ImportTasksDialog();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_URI, data);
    importTasksDialog.setArguments(args);
    return importTasksDialog;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle arguments = getArguments();
    Uri data = arguments.getParcelable(EXTRA_URI);
    ProgressDialog progressDialog = dialogBuilder.newProgressDialog();
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setCancelable(false);
    progressDialog.setIndeterminate(true);
    progressDialog.show();
    setCancelable(false);
    try {
      String extension = MimeTypeMap.getFileExtensionFromUrl(data.getPath());
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
          throw new IOException("Invalid file type");
      }
      return progressDialog;
    } catch (IOException e) {
      Timber.e(e);
    }
    return null;
  }

  @Override
  protected void inject(NativeDialogFragmentComponent component) {
    component.inject(this);
  }
}
