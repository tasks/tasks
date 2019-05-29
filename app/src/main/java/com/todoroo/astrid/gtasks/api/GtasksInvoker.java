package com.todoroo.astrid.gtasks.api;

import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksRequest;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.common.base.Strings;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.DebugNetworkInterceptor;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case of an
 * exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
public class GtasksInvoker {

  private final Context context;
  private final GoogleAccountManager googleAccountManager;
  private final Preferences preferences;
  private final DebugNetworkInterceptor interceptor;
  private final GoogleCredential credential = new GoogleCredential();
  private final String account;
  private final Tasks service;

  @Inject
  public GtasksInvoker(
      @ForApplication Context context,
      GoogleAccountManager googleAccountManager,
      Preferences preferences,
      DebugNetworkInterceptor interceptor) {
    this.context = context;
    this.googleAccountManager = googleAccountManager;
    this.preferences = preferences;
    this.interceptor = interceptor;
    account = null;
    service = null;
  }

  private GtasksInvoker(
      Context context,
      GoogleAccountManager googleAccountManager,
      Preferences preferences,
      DebugNetworkInterceptor interceptor,
      String account) {
    this.context = context;
    this.googleAccountManager = googleAccountManager;
    this.preferences = preferences;
    this.interceptor = interceptor;
    this.account = account;

    service =
        new Tasks.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
            .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
            .build();
  }

  public GtasksInvoker forAccount(String account) {
    return new GtasksInvoker(context, googleAccountManager, preferences, interceptor, account);
  }

  private void checkToken() {
    if (Strings.isNullOrEmpty(credential.getAccessToken())) {
      Bundle bundle = googleAccountManager.getAccessToken(account, TasksScopes.TASKS);
      credential.setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
    }
  }

  public @Nullable TaskLists allGtaskLists(@Nullable String pageToken) throws IOException {
    return execute(service.tasklists().list().setMaxResults(100L).setPageToken(pageToken));
  }

  public @Nullable com.google.api.services.tasks.model.Tasks getAllGtasksFromListId(
      String listId, long lastSyncDate, @Nullable String pageToken) throws IOException {
    return execute(
        service
            .tasks()
            .list(listId)
            .setMaxResults(100L)
            .setShowDeleted(true)
            .setShowHidden(true)
            .setPageToken(pageToken)
            .setUpdatedMin(
                GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339()));
  }

  public com.google.api.services.tasks.model.Tasks getAllPositions(
      String listId, @Nullable String pageToken) throws IOException {
    return execute(
        service
            .tasks()
            .list(listId)
            .setMaxResults(100L)
            .setShowDeleted(false)
            .setShowHidden(true)
            .setPageToken(pageToken)
            .setFields("items(id,parent,position)"));
  }

  public @Nullable Task createGtask(
      String listId, Task task, @Nullable String parent, @Nullable String previous)
      throws IOException {
    return execute(service.tasks().insert(listId, task).setParent(parent).setPrevious(previous));
  }

  public void updateGtask(String listId, Task task) throws IOException {
    execute(service.tasks().update(listId, task.getId(), task));
  }

  @Nullable
  public Task moveGtask(String listId, String taskId, String parentId, String previousId)
      throws IOException {
    return execute(
        service.tasks().move(listId, taskId).setParent(parentId).setPrevious(previousId));
  }

  public void deleteGtaskList(String listId) throws IOException {
    try {
      execute(service.tasklists().delete(listId));
    } catch (HttpNotFoundException ignored) {
    }
  }

  public @Nullable TaskList renameGtaskList(String listId, String title) throws IOException {
    return execute(service.tasklists().patch(listId, new TaskList().setTitle(title)));
  }

  public @Nullable TaskList createGtaskList(String title) throws IOException {
    return execute(service.tasklists().insert(new TaskList().setTitle(title)));
  }

  public void deleteGtask(String listId, String taskId) throws IOException {
    try {
      execute(service.tasks().delete(listId, taskId));
    } catch (HttpNotFoundException ignored) {
    }
  }

  private synchronized @Nullable <T> T execute(TasksRequest<T> request) throws IOException {
    return execute(request, false);
  }

  private synchronized @Nullable <T> T execute(TasksRequest<T> request, boolean retry)
      throws IOException {
    checkToken();
    T response;
    try {
      HttpRequest httpRequest = request.buildHttpRequest();
      Timber.d("%s", httpRequest.getUrl());
      if (preferences.isFlipperEnabled()) {
        response = interceptor.execute(httpRequest, request.getResponseClass());
      } else {
        response = httpRequest.execute().parseAs(request.getResponseClass());
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
    Timber.d("%s response: %s", getCaller(retry), prettyPrint(response));
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

  private String getCaller(boolean retry) {
    if (BuildConfig.DEBUG) {
      try {
        return Thread.currentThread().getStackTrace()[retry ? 6 : 5].getMethodName();
      } catch (Exception e) {
        Timber.e(e);
      }
    }
    return "";
  }
}
