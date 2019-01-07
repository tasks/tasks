package org.tasks.drive;

import android.content.Context;
import android.net.Uri;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.files.FileHelper;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class DriveInvoker {

  private static final String MIME_FOLDER = "application/vnd.google-apps.folder";

  private final Drive service;
  private final Context context;

  @Inject
  public DriveInvoker(@ForApplication Context context, Preferences preferences) {
    this.context = context;
    if (preferences.getBoolean(R.string.p_google_drive_backup, false)) {
      GoogleAccountCredential credential =
          GoogleAccountCredential.usingOAuth2(
              context, Collections.singletonList(DriveScopes.DRIVE_FILE))
              .setBackOff(new ExponentialBackOff.Builder().build())
              .setSelectedAccountName(
                  preferences.getStringValue(R.string.p_google_drive_backup_account));
      service =
          new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
              .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
              .build();
    } else {
      service = null;
    }
  }

  public File getFile(String folderId) throws IOException {
    return execute(service.files().get(folderId).setFields("id, trashed"));
  }

  public void delete(File file) throws IOException {
    execute(service.files().delete(file.getId()));
  }

  public List<File> getFilesByPrefix(String folderId, String prefix) throws IOException {
    String query =
        String.format(
            "'%s' in parents and name contains '%s' and trashed = false and mimeType != '%s'",
            folderId, prefix, MIME_FOLDER);
    return execute(service.files().list().setQ(query).setSpaces("drive")).getFiles();
  }

  public File createFolder(String name) throws IOException {
    File folder = new File()
        .setName(name)
        .setMimeType("application/vnd.google-apps.folder");

    return execute(service.files().create(folder).setFields("id"));
  }

  public void createFile(String folderId, Uri uri) throws IOException {
    String mime = FileHelper.getMimeType(context, uri);
    File metadata = new File()
        .setParents(Collections.singletonList(folderId))
        .setMimeType(mime)
        .setName(FileHelper.getFilename(context, uri));
    InputStreamContent content =
        new InputStreamContent(mime, context.getContentResolver().openInputStream(uri));
    execute(service.files().create(metadata, content));
  }

  private synchronized <T> T execute(DriveRequest<T> request) throws IOException {
    String caller = getCaller();
    Timber.d("%s request: %s", caller, request);
    T response = request.execute();
    Timber.d("%s response: %s", caller, prettyPrint(response));
    return response;
  }

  private <T> Object prettyPrint(T object) throws IOException {
    if (BuildConfig.DEBUG) {
      if (object instanceof GenericJson) {
        return ((GenericJson) object).toPrettyString();
      }
    }
    return object;
  }

  private String getCaller() {
    if (BuildConfig.DEBUG) {
      try {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
      } catch (Exception e) {
        Timber.e(e);
      }
    }
    return "";
  }
}
