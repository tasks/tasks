package com.todoroo.astrid.gtasks.api;

import android.content.Context;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
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
import java.util.Collections;
import org.tasks.BuildConfig;
import org.tasks.gtasks.GoogleTasksUnsuccessfulResponseHandler;
import org.tasks.gtasks.PlayServices;
import timber.log.Timber;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case of an
 * exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
public class GtasksInvoker {

  private final GoogleAccountCredential credential;
  private final PlayServices playServices;
  private final Tasks service;

  public GtasksInvoker(Context context, PlayServices playServices, String account) {
    this.playServices = playServices;
    credential =
        GoogleAccountCredential.usingOAuth2(context, Collections.singletonList(TasksScopes.TASKS))
            .setSelectedAccountName(account);
    service =
        new Tasks.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
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

  public Task moveGtask(String listId, String taskId, String parentId, String previousId)
      throws IOException {
    return execute(
        service.tasks().move(listId, taskId).setParent(parentId).setPrevious(previousId));
  }

  public void deleteGtaskList(String listId) throws IOException {
    execute(service.tasklists().delete(listId));
  }

  public TaskList renameGtaskList(String listId, String title) throws IOException {
    return execute(service.tasklists().patch(listId, new TaskList().setTitle(title)));
  }

  public TaskList createGtaskList(String title) throws IOException {
    return execute(service.tasklists().insert(new TaskList().setTitle(title)));
  }

  public void clearCompleted(String listId) throws IOException {
    execute(service.tasks().clear(listId));
  }

  public void deleteGtask(String listId, String taskId) throws IOException {
    try {
      execute(service.tasks().delete(listId, taskId));
    } catch (HttpNotFoundException ignored) {

    }
  }

  private synchronized <T> T execute(TasksRequest<T> request) throws IOException {
    String caller = getCaller();
    Timber.d("%s request: %s", caller, request);
    HttpRequest httpRequest = request.buildHttpRequest();
    httpRequest.setUnsuccessfulResponseHandler(
        new GoogleTasksUnsuccessfulResponseHandler(playServices, credential));
    HttpResponse httpResponse = httpRequest.execute();
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
