package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import android.content.Context;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.Tasks.TasksOperations.Insert;
import com.google.api.services.tasks.Tasks.TasksOperations.List;
import com.google.api.services.tasks.Tasks.TasksOperations.Move;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case
 * of an exception, each request is tried twice in case of a timeout.
 * @author Sam Bosley
 *
 */
@SuppressWarnings("nls")
public class GtasksInvoker {
    private Tasks service;
    private GoogleAccessProtectedResource accessProtectedResource;
    private String token;
    private JsonFactory jsonFactory;

    @Autowired ExceptionService exceptionService;

    private static final String API_KEY = "AIzaSyCIYZTBo6haRHxmiplZsfYdagFEpaiFnAk"; // non-production API key

    public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; //"oauth2:https://www.googleapis.com/auth/tasks";

    public GtasksInvoker(String authToken) {
        DependencyInjectionService.getInstance().inject(this);
        authenticate(authToken);
    }

    public void authenticate(String authToken) {
        this.token = authToken;
        accessProtectedResource = new GoogleAccessProtectedResource(authToken);

        jsonFactory = new GsonFactory();
        service = new Tasks(AndroidHttp.newCompatibleTransport(), accessProtectedResource, jsonFactory);
        service.setKey(API_KEY);
        service.setApplicationName("Astrid");
    }

    //If we get a 401 or 403, try revalidating the auth token before bailing
    private synchronized void handleException(IOException e) throws IOException {
        if (e instanceof HttpResponseException) {
            HttpResponseException h = (HttpResponseException)e;
            int statusCode = h.getResponse().getStatusCode();
            if (statusCode == 401 || statusCode == 403) {
                System.err.println("Encountered " + statusCode + " error");
                token = GtasksTokenValidator.validateAuthToken(ContextManager.getContext(), token);
                if (token != null) {
                    accessProtectedResource.setAccessToken(token);
                }
            } else if (statusCode == 503) { // 503 errors are generally either 1) quota limit reached or 2) problems on Google's end
                System.err.println("Encountered 503 error");
                final Context context = ContextManager.getContext();
                String message = context.getString(R.string.gtasks_error_backend);
                exceptionService.reportError(message, h);
            } else if (statusCode == 400 || statusCode == 500) {
                System.err.println("Encountered " + statusCode + " error");
                System.err.println(h.getResponse().getStatusMessage());
                h.printStackTrace();
                throw h;
            }
        }
    }

    private static void log(String method, Object result) {
        System.err.println("QUERY: " + method + ", RESULT: " + result);
    }

    /**
     * A simple service query that will throw an exception if anything goes wrong.
     * Useful for checking if token needs revalidating or if there are network problems--
     * no exception means all is well
     * @throws IOException
     */
    public void ping() throws IOException {
        service.tasklists().get("@default").execute();
    }

    public TaskLists allGtaskLists() throws IOException {
        TaskLists toReturn = null;
        try {
            toReturn = service.tasklists().list().execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasklists().list().execute();
        } finally {
            log("All gtasks lists", toReturn);
        }
        return toReturn;
    }

    public TaskList getGtaskList(String id) throws IOException {
        TaskList toReturn = null;
        try {
            toReturn = service.tasklists().get(id).execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasklists().get(id).execute();
        } finally {
            log("Get gtask list, id: " + id, toReturn);
        }
        return toReturn;
    }

    public TaskList createGtaskList(String title) throws IOException {
        TaskList newList = new TaskList();
        newList.setTitle(title);
        TaskList toReturn = null;
        try {
            toReturn = service.tasklists().insert(newList).execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasklists().insert(newList).execute();
        } finally {
            log("Create gtask list, title: " + title, toReturn);
        }
        return toReturn;
    }

    public TaskList updateGtaskList(TaskList list) throws IOException {
        TaskList toReturn = null;
        try {
            toReturn = service.tasklists().update(list.getId(), list).execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasklists().update(list.getId(), list).execute();
        } finally {
            log("Update list, id: " + list.getId(), toReturn);
        }
        return toReturn;
    }

    public void deleteGtaskList(String listId) throws IOException {
        try {
            service.tasklists().delete(listId).execute();
        } catch (IOException e) {
            handleException(e);
            service.tasklists().delete(listId).execute();
        } finally {
            log("Delete list, id: " + listId, null);
        }
    }

    public com.google.api.services.tasks.model.Tasks getAllGtasksFromTaskList(TaskList list, boolean includeDeleted, boolean includeHidden, long lastSyncDate) throws IOException {
        return getAllGtasksFromListId(list.getId(), includeDeleted, includeHidden, lastSyncDate);
    }

    public com.google.api.services.tasks.model.Tasks getAllGtasksFromListId(String listId, boolean includeDeleted, boolean includeHidden, long lastSyncDate) throws IOException {
        com.google.api.services.tasks.model.Tasks toReturn = null;
        List request = service.tasks().list(listId);
        request.setShowDeleted(includeDeleted);
        request.setShowHidden(includeHidden);
        request.setUpdatedMin(GtasksApiUtilities.unixTimeToGtasksCompletionTime(lastSyncDate).toStringRfc3339());
        try {
            toReturn = request.execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = request.execute();
        } finally {
            log("Get all tasks, list: " + listId + ", include deleted: " + includeDeleted, toReturn);
        }
        return toReturn;
    }

    public Task getGtask(String listId, String taskId) throws IOException {
        Task toReturn = null;
        try {
            toReturn = service.tasks().get(listId, taskId).execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasks().get(listId, taskId).execute();
        } finally {
            log("Get gtask, id: " + taskId + ", list id: " + listId, toReturn);
        }
        return toReturn;
    }

    public Task createGtask(String listId, String title, String notes, DateTime due) throws IOException {
        Task newGtask = new Task();
        newGtask.setTitle(title);
        newGtask.setNotes(notes);
        newGtask.setDue(due);

        return createGtask(listId, newGtask);
    }

    public Task createGtask(String listId, Task task) throws IOException {
        return createGtask(listId, task, null, null);
    }

    public Task createGtask(String listId, Task task, String parent, String priorSiblingId) throws IOException {
        Insert insertOp = service.tasks().insert(listId, task);
        insertOp.setParent(parent);
        insertOp.setPrevious(priorSiblingId);

        Task toReturn = null;
        try {
            toReturn = insertOp.execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = insertOp.execute();
        } finally {
            log("Creating gtask, title: " + task.getTitle(), toReturn);
        }
        return toReturn;
    }

    public Task updateGtask(String listId, Task task) throws IOException {
        Task toReturn = null;
        try {
            toReturn = service.tasks().update(listId, task.getId(), task).execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = service.tasks().update(listId, task.getId(), task).execute();
        } finally {
            log("Update gtask, title: " + task.getTitle(), toReturn);
        }
        return toReturn;
    }

    public Task moveGtask(String listId, String taskId, String parentId, String previousId) throws IOException {
        Move move = service.tasks().move(listId, taskId);
        move.setParent(parentId);
        move.setPrevious(previousId);

        Task toReturn = null;
        try {
            toReturn = move.execute();
        } catch (IOException e) {
            handleException(e);
            toReturn = move.execute();
        } finally {
            log("Move task " + taskId + "to parent: " + parentId + ", prior sibling: " + previousId, toReturn);
        }
        return toReturn;
    }

    public void deleteGtask(String listId, String taskId) throws IOException {
        try {
            service.tasks().delete(listId, taskId).execute();
        } catch (IOException e) {
            handleException(e);
            service.tasks().delete(listId, taskId).execute();
        } finally {
            log("Delete task, id: " + taskId, null);
        }
    }

    public void clearCompletedTasks(String listId) throws IOException {
        try {
            service.tasks().clear(listId).execute();
        } catch (IOException e) {
            handleException(e);
            service.tasks().clear(listId).execute();
        } finally {
            log("Clear completed tasks, list id: " + listId, null);
        }
    }

    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }
}
