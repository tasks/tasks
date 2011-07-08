package com.todoroo.astrid.gtasks;

import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.services.tasks.v1.model.Task;
import com.google.api.services.tasks.v1.model.TaskList;
import com.google.api.services.tasks.v1.model.TaskLists;
import com.google.api.services.tasks.v1.model.Tasks;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.gtasks.api.MoveListRequest;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksApiTest extends DatabaseTestCase {

    private static final String DEFAULT_LIST = "@default";
    private static final String TEST_ACCOUNT = "sync_tester@astrid.com";
    private static GtasksService service;
    private static boolean initialized = false;

    public void testCreateTask() throws Exception {
        Task newTask = new Task();
        String title = newTask.title = "New task";

        service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));
    }

    public void testUpdateTaskProperties() throws Exception {
        Task newTask = new Task();
        String title = newTask.title = "This title will change";

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        String title2 = newTask.title = "Changed Title";
        service.updateGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title2));
        assertFalse(taskWithTitleExists(title));
    }

    public void testTaskDateFormatting() throws Exception {
        Task newTask = new Task();
        String title = newTask.title = "Due date will change";

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        long dueTime = new Date(114, 1, 13).getTime();
        String dueTimeString = GtasksApiUtilities.unixTimeToGtasksTime(dueTime);
        newTask.due = dueTimeString;
        newTask = service.updateGtask(DEFAULT_LIST, newTask);
        assertEquals(dueTimeString, GtasksApiUtilities.gtasksDueTimeStringToLocalTimeString(newTask.due));
        assertEquals(dueTime, GtasksApiUtilities.gtasksDueTimeToUnixTime(newTask.due, 0));

        long compTime = new Date(115, 2, 14).getTime();
        String compTimeString = GtasksApiUtilities.unixTimeToGtasksTime(compTime);
        newTask.completed = compTimeString;
        newTask.status = "completed";
        newTask = service.updateGtask(DEFAULT_LIST, newTask);
        assertEquals(compTimeString, GtasksApiUtilities.gtasksCompletedTimeStringToLocalTimeString(newTask.completed));
        assertEquals(compTime, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(newTask.completed, 0));
    }

    public void testTaskDeleted() throws Exception {
        Task newTask = new Task();
        String title = newTask.title = "This task will be deleted";

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        service.deleteGtask(DEFAULT_LIST, newTask.id);
        assertFalse(taskWithTitleExists(title));
    }

    public void testTaskMoved() throws Exception {
        Task newTask1 = new Task();
        String title1 = newTask1.title = "Task 1";
        Task newTask2 = new Task();
        String title2 = newTask2.title = "Task 2";

        newTask1 = service.createGtask(DEFAULT_LIST, newTask1);
        newTask2 = service.createGtask(DEFAULT_LIST, newTask2);

        assertTrue(taskWithTitleExists(title1));
        assertTrue(taskWithTitleExists(title2));

        System.err.println("Task 1 id: " + newTask1.id);
        System.err.println("Task 2 id: " + newTask2.id);

        service.moveGtask(DEFAULT_LIST, newTask1.id, newTask2.id, null);
        newTask1 = service.getGtask(DEFAULT_LIST, newTask1.id);
        newTask2 = service.getGtask(DEFAULT_LIST, newTask2.id);

        assertEquals(newTask1.parent, newTask2.id);

        service.moveGtask(DEFAULT_LIST, newTask1.id, null, newTask2.id);
        newTask1 = service.getGtask(DEFAULT_LIST, newTask1.id);
        newTask2 = service.getGtask(DEFAULT_LIST, newTask2.id);

        assertNull(newTask1.parent);
        assertTrue(newTask2.position.compareTo(newTask1.position) < 0);
    }

    public void testMoveBetweenLists() throws Exception {
        Task newTask = new Task();
        String title = newTask.title = "This task will move lists";

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        String listTitle = "New list";
        service.createGtaskList(listTitle);
        TaskList newList;

        assertNotNull(newList = listWithTitle(listTitle));

        MoveListRequest moveTask = new MoveListRequest(service, newTask.id, DEFAULT_LIST, newList.id, null);
        moveTask.executePush();

        assertFalse(taskWithTitleExists(title));
        assertTrue(listHasTaskWithTitle(newList.id, title));
    }

    private boolean listHasTaskWithTitle(String listId, String title) throws Exception {
        com.google.api.services.tasks.v1.model.Tasks newListTasks = service.getAllGtasksFromListId(listId, false);
        if (newListTasks.items != null) {
            for (Task t : newListTasks.items) {
                if (t.title.equals(title)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean taskWithTitleExists(String title) throws Exception {
        Tasks defaultList = service.getAllGtasksFromListId(DEFAULT_LIST, false);
        if (defaultList.items != null) {
            for (Task t : defaultList.items) {
                if (t.title.equals(title))
                    return true;
            }
        }
        return false;
    }

    public void testCreateList() throws Exception {
        String title1 = "My new list!";
        service.createGtaskList(title1);
        assertNotNull(listWithTitle(title1));

        String title2 = "Another new list!";
        service.createGtaskList("Another new list!");
        assertNotNull(listWithTitle(title2));
        assertNotNull(listWithTitle(title1));
    }

    public void testDeleteList() throws Exception {
        String title = "This list will be deleted";
        TaskList t = service.createGtaskList(title);
        assertNotNull(listWithTitle(title));

        service.deleteGtaskList(t.id);
        assertNull(listWithTitle(title));
    }

    public void testUpdateListProperties() throws Exception {
        String title1 = "This title will change";
        TaskList t = service.createGtaskList(title1);
        assertNotNull(listWithTitle(title1));

        String title2 = t.title = "New title";
        service.updateGtaskList(t);
        assertNotNull(listWithTitle(title2));
        assertNull(listWithTitle(title1));
    }

    private TaskList listWithTitle(String title) throws Exception {
        TaskLists allLists = service.allGtaskLists();
        for (TaskList t : allLists.items) {
            if (t.title.equals(title))
                return t;
        }
        return null;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!initialized) {
            GoogleAccountManager manager = new GoogleAccountManager(ContextManager.getContext());
            Account[] accounts = manager.getAccounts();
            Account toUse = null;
            for (Account a : accounts) {
                if (a.name.equals(TEST_ACCOUNT)) {
                    toUse = a;
                    break;
                }
            }

            if (toUse == null) {
                toUse = accounts[0];
            }

            Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, toUse.name);
            AccountManagerFuture<Bundle> accountManagerFuture = manager.manager.getAuthToken(toUse, GtasksService.AUTH_TOKEN_TYPE, true, null, null);

            Bundle authTokenBundle = accountManagerFuture.getResult();
            if (authTokenBundle.containsKey(AccountManager.KEY_INTENT)) {
                Intent i = (Intent) authTokenBundle.get(AccountManager.KEY_INTENT);
                ContextManager.getContext().startActivity(i);
                return;
            }

            String authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            authToken = GtasksTokenValidator.validateAuthToken(authToken);

            service = new GtasksService(authToken);

            initialized = true;
        }
        deleteAllLists();
        clearDefaultList();
    }

    private void deleteAllLists() {
        try {
            TaskLists allLists = service.allGtaskLists();
            for (TaskList t : allLists.items) {
                if (!t.title.equals("Default List"))
                    service.deleteGtaskList(t.id);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to clear lists");
        }
    }

    private void clearDefaultList() {
        try {
            Tasks tasks = service.getAllGtasksFromListId(DEFAULT_LIST, false);
            if (tasks.items != null) {
                for (Task t : tasks.items) {
                    service.deleteGtask(DEFAULT_LIST, t.id);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to clear default list");
        }
    }

}
