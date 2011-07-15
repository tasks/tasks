package com.todoroo.astrid.gtasks.api;

import java.io.IOException;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.v1.Tasks;
import com.google.api.services.tasks.v1.Tasks.TasksOperations.Insert;
import com.google.api.services.tasks.v1.Tasks.TasksOperations.List;
import com.google.api.services.tasks.v1.Tasks.TasksOperations.Move;
import com.google.api.services.tasks.v1.model.Task;
import com.google.api.services.tasks.v1.model.TaskList;
import com.google.api.services.tasks.v1.model.TaskLists;

/**
 * Wrapper around the official Google Tasks API to simplify common operations. In the case
 * of an exception, each request is tried twice in case of a timeout.
 * @author Sam Bosley
 *
 */
@SuppressWarnings("nls")
public class GtasksService {
    private Tasks service;

    private static final String API_KEY = "AIzaSyCIYZTBo6haRHxmiplZsfYdagFEpaiFnAk"; // non-production API key

    public static final String AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/tasks";

    public GtasksService(String authToken) {
        try {
            authenticate(authToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void authenticate(String authToken) throws IOException {

        GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(authToken);

        service = new Tasks(AndroidHttp.newCompatibleTransport(), accessProtectedResource, new JacksonFactory());
        service.accessKey = API_KEY;
        service.setApplicationName("Astrid");
    }

    /**
     * A simple service query that will throw an exception if anything goes wrong.
     * Useful for checking if token needs revalidating or if there are network problems--
     * no exception means all is well
     * @throws IOException
     */
    public void ping() throws IOException {
        service.tasklists.get("@default").execute();
    }

    public TaskLists allGtaskLists() throws IOException {
        TaskLists toReturn;
        try {
            toReturn = service.tasklists.list().execute();
        } catch (IOException e) {
            toReturn = service.tasklists.list().execute();
        }
        return toReturn;
    }

    public TaskList getGtaskList(String id) throws IOException {
        TaskList toReturn;
        try {
            toReturn = service.tasklists.get(id).execute();
        } catch (IOException e) {
            toReturn = service.tasklists.get(id).execute();
        }
        return toReturn;
    }

    public TaskList createGtaskList(String title) throws IOException {
        TaskList newList = new TaskList();
        newList.title = title;
        TaskList toReturn;
        try {
            toReturn = service.tasklists.insert(newList).execute();
        } catch (IOException e) {
            toReturn = service.tasklists.insert(newList).execute();
        }
        return toReturn;
    }

    public TaskList updateGtaskList(TaskList list) throws IOException {
        TaskList toReturn;
        try {
            toReturn = service.tasklists.update(list.id, list).execute();
        } catch (IOException e) {
            toReturn = service.tasklists.update(list.id, list).execute();
        }
        return toReturn;
    }

    public void deleteGtaskList(String listId) throws IOException {
        try {
            service.tasklists.delete(listId).execute();
        } catch (IOException e) {
            service.tasks.clear(listId).execute();
        }
    }

    public com.google.api.services.tasks.v1.model.Tasks getAllGtasksFromTaskList(TaskList list, boolean includeDeleted) throws IOException {
        com.google.api.services.tasks.v1.model.Tasks toReturn;
        try {
            toReturn = getAllGtasksFromListId(list.id, includeDeleted);
        } catch (IOException e) {
            toReturn = getAllGtasksFromListId(list.id, includeDeleted);
        }
        return toReturn;
    }

    public com.google.api.services.tasks.v1.model.Tasks getAllGtasksFromListId(String listId, boolean includeDeleted) throws IOException {
        com.google.api.services.tasks.v1.model.Tasks toReturn;
        List request = service.tasks.list(listId);
        request.showDeleted = includeDeleted;
        try {
            toReturn = request.execute();
        } catch (IOException e) {
            toReturn = request.execute();
        }
        return toReturn;
    }

    public Task getGtask(String listId, String taskId) throws IOException {
        Task toReturn;
        try {
            toReturn = service.tasks.get(listId, taskId).execute();
        } catch (IOException e) {
            toReturn = service.tasks.get(listId, taskId).execute();
        }
        return toReturn;
    }

    public Task createGtask(String listId, String title, String notes, String due) throws IOException {
        Task newGtask = new Task();
        newGtask.title = title;
        newGtask.notes = notes;
        newGtask.due = due;

        return createGtask(listId, newGtask);
    }

    public Task createGtask(String listId, Task task) throws IOException {
        return createGtask(listId, task, null, null);
    }

    public Task createGtask(String listId, Task task, String parent, String priorSiblingId) throws IOException {
        Insert insertOp = service.tasks.insert(listId, task);
        insertOp.parent = parent;
        insertOp.previous = priorSiblingId;

        Task toReturn;
        try {
            toReturn = insertOp.execute();
        } catch (IOException e) {
            toReturn = insertOp.execute();
        }
        return toReturn;
    }

    public Task updateGtask(String listId, Task task) throws IOException {
        Task toReturn;
        try {
            toReturn = service.tasks.update(listId, task.id, task).execute();
        } catch (IOException e) {
            toReturn = service.tasks.update(listId, task.id, task).execute();
        }
        return toReturn;
    }

    public Task moveGtask(String listId, String taskId, String parentId, String previousId) throws IOException {
        Move move = service.tasks.move(listId, taskId);
        move.parent = parentId;
        move.previous = previousId;

        Task toReturn;
        try {
            toReturn = move.execute();
        } catch (IOException e) {
            toReturn = move.execute();
        }
        return toReturn;
    }

    public void deleteGtask(String listId, String taskId) throws IOException {
        try {
            service.tasks.delete(listId, taskId).execute();
        } catch (IOException e) {
            service.tasks.delete(listId, taskId).execute();
        }
    }

    public void clearCompletedTasks(String listId) throws IOException {
        try {
            service.tasks.clear(listId).execute();
        } catch (IOException e) {
            service.tasks.clear(listId).execute();
        }
    }
}
