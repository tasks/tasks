package org.tasks.jobs;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.newArrayList;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.net.ssl.SSLException;
import org.tasks.R;
import org.tasks.drive.DriveInvoker;
import org.tasks.injection.ApplicationContext;
import org.tasks.injection.InjectingWorker;
import org.tasks.injection.JobComponent;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class DriveUploader extends InjectingWorker {

  private static final String FOLDER_NAME = "Tasks Backups";
  private static final String EXTRA_URI = "extra_uri";
  private static final String EXTRA_PURGE = "extra_purge";
  private static final Comparator<File> DRIVE_FILE_COMPARATOR =
      (f1, f2) -> Long.compare(f2.getModifiedTime().getValue(), f1.getModifiedTime().getValue());

  @Inject @ApplicationContext Context context;
  @Inject DriveInvoker drive;
  @Inject Preferences preferences;

  public DriveUploader(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  static Data getInputData(Uri uri, boolean purge) {
    return new Data.Builder()
        .putString(EXTRA_URI, uri.toString())
        .putBoolean(EXTRA_PURGE, purge)
        .build();
  }

  private static List<File> getDeleteList(List<File> files) {
    Collections.sort(files, DRIVE_FILE_COMPARATOR);
    return newArrayList(skip(files, BackupWork.DAYS_TO_KEEP_BACKUP));
  }

  @Override
  protected Result run() {
    Data inputData = getInputData();
    Uri uri = Uri.parse(inputData.getString(EXTRA_URI));
    try {
      File folder = getFolder();
      preferences.setString(R.string.p_google_drive_backup_folder, folder.getId());
      drive.createFile(folder.getId(), uri);

      if (inputData.getBoolean(EXTRA_PURGE, false)) {
        List<File> files = drive.getFilesByPrefix(folder.getId(), "auto.");
        for (File file : getDeleteList(files)) {
          try {
            drive.delete(file);
          } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
              Timber.e(e);
            } else {
              throw e;
            }
          }
        }
      }

      return Result.success();
    } catch (SocketTimeoutException | SSLException | ConnectException | UnknownHostException e) {
      Timber.e(e);
      return Result.retry();
    } catch (IOException e) {
      firebase.reportException(e);
      return Result.failure();
    }
  }

  private File getFolder() throws IOException {
    String folderId = preferences.getStringValue(R.string.p_google_drive_backup_folder);
    File file = null;
    if (!isNullOrEmpty(folderId)) {
      try {
        file = drive.getFile(folderId);
      } catch (GoogleJsonResponseException e) {
        if (e.getStatusCode() != 404) {
          throw e;
        }
      }
    }

    return file == null || file.getTrashed() ? drive.createFolder(FOLDER_NAME) : file;
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }
}
