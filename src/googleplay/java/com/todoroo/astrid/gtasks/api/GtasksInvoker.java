package com.todoroo.astrid.gtasks.api;

import android.content.Context;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
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
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import org.tasks.AccountManager;
import org.tasks.BuildConfig;
import org.tasks.injection.ForApplication;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case
 * of an exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
@Singleton
public class GtasksInvoker {

    private AccountManager accountManager;
    private final GoogleAccountCredential credential;
    private Tasks service;

    @Inject
    public GtasksInvoker(@ForApplication Context context, GtasksPreferenceService preferenceService, AccountManager accountManager) {
        this.accountManager = accountManager;
        credential = GoogleAccountCredential.usingOAuth2(context, Collections.singletonList(TasksScopes.TASKS));
        setUserName(preferenceService.getUserName());
        service = new Tasks.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName(String.format("Tasks/%s", BuildConfig.VERSION_NAME))
                .build();
    }

    public void setUserName(String username) {
        credential.setSelectedAccountName(username);
    }

    //If we get a 401 or 403, try revalidating the auth token before bailing
    private synchronized void handleException(IOException e) throws IOException {
        Timber.e(e, e.getMessage());
        if (e instanceof HttpResponseException) {
            HttpResponseException h = (HttpResponseException) e;
            int statusCode = h.getStatusCode();
            if (statusCode == 401 || statusCode == 403) {
                accountManager.clearToken(credential);
            } else if (statusCode == 400 || statusCode == 500) {
                throw h;
            } else if (statusCode == 404) {
                throw new HttpNotFoundException(h);
            } else {
                Timber.e(e, "%s: %s", statusCode, h.getStatusMessage());
            }
            // 503 errors are generally either 1) quota limit reached or 2) problems on Google's end
        }
    }

    public TaskLists allGtaskLists() throws IOException {
        return execute(service
                .tasklists()
                .list());
    }

    public TaskList getGtaskList(String id) throws IOException {
        return execute(service
                .tasklists()
                .get(id));
    }

    public com.google.api.services.tasks.model.Tasks getAllGtasksFromListId(String listId, boolean includeDeleted, boolean includeHidden, long lastSyncDate) throws IOException {
        return execute(service
                .tasks()
                .list(listId)
                .setShowDeleted(includeDeleted)
                .setShowHidden(includeHidden)
                .setUpdatedMin(GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339()));
    }

    public Task createGtask(String listId, Task task, String parent, String priorSiblingId) throws IOException {
        return execute(service
                .tasks()
                .insert(listId, task)
                .setParent(parent)
                .setPrevious(priorSiblingId));
    }

    public void updateGtask(String listId, Task task) throws IOException {
        execute(service
                .tasks()
                .update(listId, task.getId(), task));
    }

    public Task moveGtask(String listId, String taskId, String parentId, String previousId) throws IOException {
        return execute(service
                .tasks()
                .move(listId, taskId)
                .setParent(parentId)
                .setPrevious(previousId));
    }

    public void deleteGtask(String listId, String taskId) throws IOException {
        execute(service
                .tasks()
                .delete(listId, taskId));
    }

    private synchronized <T> T execute(TasksRequest<T> request) throws IOException {
        String caller = getCaller();
        Timber.d("%s request: %s", caller, request);
        T response;
        try {
            response = request.execute();
        } catch (IOException e) {
            handleException(e);
            response = request.execute();
        }
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
                Timber.e(e, e.getMessage());
            }
        }
        return "";
    }
}
