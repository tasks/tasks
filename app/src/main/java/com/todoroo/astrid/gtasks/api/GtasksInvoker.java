package com.todoroo.astrid.gtasks.api;

import android.accounts.AccountManager;
import android.os.Bundle;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
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
import java.io.IOException;
import org.tasks.BuildConfig;
import org.tasks.gtasks.GoogleAccountManager;
import timber.log.Timber;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case of an
 * exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
public class GtasksInvoker {

  private final String account;
  private final GoogleAccountManager googleAccountManager;
  private Tasks service;
  private GoogleCredential credential;

  public GtasksInvoker(String account, GoogleAccountManager googleAccountManager) {
    this.account = account;
    this.googleAccountManager = googleAccountManager;
    initializeService();
  }

  private void initializeService() {
    Bundle bundle = googleAccountManager.getAccessToken(account, TasksScopes.TASKS);
    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
    credential = new GoogleCredential().setAccessToken(token);
    service =
        new Tasks.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
            credential)
            .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
            .build();
  }

  public TaskLists allGtaskLists(String pageToken) throws IOException {
    return execute(service.tasklists().list().setPageToken(pageToken));
  }

  public com.google.api.services.tasks.model.Tasks getAllGtasksFromListId(
      String listId,
      boolean includeDeleted,
      boolean includeHidden,
      long lastSyncDate,
      String pageToken)
      throws IOException {
    return execute(
        service
            .tasks()
            .list(listId)
            .setShowDeleted(includeDeleted)
            .setShowHidden(includeHidden)
            .setPageToken(pageToken)
            .setUpdatedMin(
                GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339()));
  }

  public Task createGtask(String listId, Task task, String parent, String priorSiblingId)
      throws IOException {
    Timber.d("createGtask: %s", prettyPrint(task));
    return execute(
        service.tasks().insert(listId, task).setParent(parent).setPrevious(priorSiblingId));
  }

  public void updateGtask(String listId, Task task) throws IOException {
    Timber.d("updateGtask: %s", prettyPrint(task));
    execute(service.tasks().update(listId, task.getId(), task));
  }

  Task moveGtask(String listId, String taskId, String parentId, String previousId)
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

  public TaskList renameGtaskList(String listId, String title) throws IOException {
    return execute(service.tasklists().patch(listId, new TaskList().setTitle(title)));
  }

  public TaskList createGtaskList(String title) throws IOException {
    return execute(service.tasklists().insert(new TaskList().setTitle(title)));
  }

  public void deleteGtask(String listId, String taskId) throws IOException {
    try {
      execute(service.tasks().delete(listId, taskId));
    } catch (HttpNotFoundException ignored) {
    }
  }

  private synchronized <T> T execute(TasksRequest<T> request) throws IOException {
    return execute(request, false);
  }

  private synchronized <T> T execute(TasksRequest<T> request, boolean retry) throws IOException {
    String caller = getCaller();
    Timber.d("%s request: %s", caller, request);
    HttpRequest httpRequest = request.buildHttpRequest();
    HttpResponse httpResponse;
    try {
      httpResponse = httpRequest.execute();
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == 401 && !retry) {
        googleAccountManager.invalidateToken(credential.getAccessToken());
        initializeService();
        return execute(request, true);
      } else {
        throw e;
      }
    }
    T response = httpResponse.parseAs(request.getResponseClass());
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
