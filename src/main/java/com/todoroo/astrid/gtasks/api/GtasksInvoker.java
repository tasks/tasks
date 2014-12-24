package com.todoroo.astrid.gtasks.api;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksRequest;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;

import java.io.IOException;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case
 * of an exception, each request is tried twice in case of a timeout.
 *
 * @author Sam Bosley
 */
public class GtasksInvoker {

    private static final Logger log = LoggerFactory.getLogger(GtasksInvoker.class);

    private final Context context;
    private final GtasksTokenValidator gtasksTokenValidator;
    private Tasks service;
    private GoogleCredential credential = new GoogleCredential();
    private String token;

    private final String key;

    public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; //"oauth2:https://www.googleapis.com/auth/tasks";

    public GtasksInvoker(Context context, GtasksTokenValidator gtasksTokenValidator, String authToken) {
        this.context = context;
        this.gtasksTokenValidator = gtasksTokenValidator;
        this.token = authToken;

        key = context.getString(R.string.gapi_key);
        credential.setAccessToken(authToken);
        HttpRequestInitializer httpRequestInitializer = new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                credential.initialize(request);
                request.setReadTimeout(0); // infinite
            }
        };
        service = new Tasks.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), httpRequestInitializer)
                .setApplicationName("Tasks")
                .build();
    }

    //If we get a 401 or 403, try revalidating the auth token before bailing
    private synchronized void handleException(IOException e) throws IOException {
        if (e instanceof HttpResponseException) {
            HttpResponseException h = (HttpResponseException) e;
            int statusCode = h.getStatusCode();
            if (statusCode == 401 || statusCode == 403) {
                token = gtasksTokenValidator.validateAuthToken(context, token);
                if (token != null) {
                    credential.setAccessToken(token);
                }
            } else if (statusCode == 400 || statusCode == 500) {
                throw h;
            } else if (statusCode == 404) {
                throw new HttpNotFoundException(h);
            } else {
                log.error(statusCode + ": " + h.getStatusMessage(), e);
            }
            // 503 errors are generally either 1) quota limit reached or 2) problems on Google's end
        } else {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * A simple service query that will throw an exception if anything goes wrong.
     * Useful for checking if token needs revalidating or if there are network problems--
     * no exception means all is well
     *
     * @throws IOException
     */
    public void ping() throws IOException {
        service.tasklists()
                .get("@default")
                .setKey(key)
                .execute();
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

    private <T> T execute(TasksRequest<T> request) throws IOException {
        String caller = getCaller();
        log.debug("{} request: {}", caller, request);
        T response;
        try {
            response = request
                    .setKey(key)
                    .execute();
        } catch (IOException e) {
            handleException(e);
            response = request
                    .setKey(key)
                    .execute();
        }
        log.debug("{} response: {}", caller, prettyPrint(response));
        return response;
    }

    private <T> Object prettyPrint(T object) throws IOException {
        if (log.isDebugEnabled() && object instanceof GenericJson) {
            return ((GenericJson) object).toPrettyString();
        }
        return object;
    }

    private String getCaller() {
        if (log.isDebugEnabled()) {
            try {
                return Thread.currentThread().getStackTrace()[4].getMethodName();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return "";
    }
}
