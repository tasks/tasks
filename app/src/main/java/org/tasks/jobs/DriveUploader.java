package org.tasks.jobs;

import android.content.Context;
import android.net.Uri;

import com.google.api.services.drive.model.File;
import com.google.common.base.Strings;

import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.drive.DriveInvoker;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.preferences.Preferences;

import java.io.IOException;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

public class DriveUploader extends InjectingWorker {

  private static final String FOLDER_NAME = "Tasks Backups";
  private static final String EXTRA_URI = "extra_uri";

  @Inject @ForApplication Context context;
  @Inject DriveInvoker drive;
  @Inject Preferences preferences;
  @Inject Tracker tracker;

  static Data getInputData(Uri uri) {
    return new Data.Builder().putString(EXTRA_URI, uri.toString()).build();
  }

  public DriveUploader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @Override
  protected Result run() {
    Data inputData = getInputData();
    Uri uri = Uri.parse(inputData.getString(EXTRA_URI));
    try {
      File folder = getFolder();
      preferences.setString(R.string.p_google_drive_backup_folder, folder.getId());
      drive.createFile(folder.getId(), uri);
      return Result.SUCCESS;
    } catch (IOException e) {
      tracker.reportException(e);
      return Result.FAILURE;
    }
  }

  private File getFolder() throws IOException {
    String folderId = preferences.getStringValue(R.string.p_google_drive_backup_folder);
    File file = Strings.isNullOrEmpty(folderId) ? null : drive.getFile(folderId);
    return file == null || file.getTrashed() ? drive.createFolder(FOLDER_NAME) : file;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
