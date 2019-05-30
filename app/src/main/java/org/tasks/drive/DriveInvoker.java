package org.tasks.drive;

import static com.todoroo.andlib.utility.DateUtilities.now;

import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.DebugNetworkInterceptor;
import org.tasks.R;
import org.tasks.files.FileHelper;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class DriveInvoker {

  private static final String MIME_FOLDER = "application/vnd.google-apps.folder";

  private final Context context;
  private final Preferences preferences;
  private final GoogleAccountManager googleAccountManager;
  private final DebugNetworkInterceptor interceptor;
  private final Drive service;
  private final GoogleCredential credential = new GoogleCredential();

  @Inject
  public DriveInvoker(
      @ForApplication Context context,
      Preferences preferences,
      GoogleAccountManager googleAccountManager,
      DebugNetworkInterceptor interceptor) {
    this.context = context;
    this.preferences = preferences;
    this.googleAccountManager = googleAccountManager;
    this.interceptor = interceptor;
    service =
        new Drive.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
            .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
            .build();
  }

  private void checkToken() {
    if (Strings.isNullOrEmpty(credential.getAccessToken())) {
      String account = preferences.getStringValue(R.string.p_google_drive_backup_account);
      Bundle bundle = googleAccountManager.getAccessToken(account, DriveScopes.DRIVE_FILE);
      credential.setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
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
    return execute(
            service
                .files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, modifiedTime)"))
        .getFiles();
  }

  public File createFolder(String name) throws IOException {
    File folder = new File().setName(name).setMimeType("application/vnd.google-apps.folder");

    return execute(service.files().create(folder).setFields("id"));
  }

  public void createFile(String folderId, Uri uri) throws IOException {
    String mime = FileHelper.getMimeType(context, uri);
    File metadata =
        new File()
            .setParents(Collections.singletonList(folderId))
            .setMimeType(mime)
            .setName(FileHelper.getFilename(context, uri));
    InputStreamContent content =
        new InputStreamContent(mime, context.getContentResolver().openInputStream(uri));
    execute(service.files().create(metadata, content));
  }

  private synchronized <T> T execute(DriveRequest<T> request) throws IOException {
    return execute(request, false);
  }

  private synchronized <T> T execute(DriveRequest<T> request, boolean retry) throws IOException {
    checkToken();
    Timber.d("%s request: %s", getCaller(), request);
    T response;
    try {
      if (preferences.isFlipperEnabled()) {
        long start = now();
        HttpResponse httpResponse = request.executeUnparsed();
        response = interceptor.report(httpResponse, request.getResponseClass(), start, now());
      } else {
        response = request.execute();
      }
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == 401 && !retry) {
        googleAccountManager.invalidateToken(credential.getAccessToken());
        credential.setAccessToken(null);
        return execute(request, true);
      } else {
        throw e;
      }
    }
    Timber.d("%s response: %s", getCaller(), prettyPrint(response));
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
