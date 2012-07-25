/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.MoveListRequest;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksApiTest extends DatabaseTestCase {

    private static final String DEFAULT_LIST = "@default";
    private static final String TEST_ACCOUNT = "sync_tester2@astrid.com";
    private static GtasksInvoker service;
    private static boolean initialized = false;
    private boolean bypassTests = false;

    public void testCreateTask() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "New task";
        newTask.setTitle(title);

        service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));
    }

    public void testUpdateTaskProperties() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "This title will change";
        newTask.setTitle(title);

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        String title2 = "Changed Title";
        newTask.setTitle(title2);
        service.updateGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title2));
        assertFalse(taskWithTitleExists(title));
    }

    public void testTaskDateFormatting2() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "Due date will change";
        newTask.setTitle(title);

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));
        newTask = service.getGtask(DEFAULT_LIST, newTask.getId());
        System.err.println("Newtask A: " + newTask.getDue());

        long now = DateUtilities.now();
        newTask.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(now));
        System.err.println("Newtask B: " + newTask.getDue());
        newTask = service.updateGtask(DEFAULT_LIST, newTask);
        System.err.println("Newtask C: " + newTask.getDue());

       long complete = now + DateUtilities.ONE_DAY;
       newTask.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(complete));
       System.err.println("Newtask D: " + newTask.getCompleted());
       newTask.setStatus("completed");
       newTask = service.updateGtask(DEFAULT_LIST, newTask);
       System.err.println("Newtask E: " + newTask.getCompleted());
    }

    public void testTaskDateFormatting() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "Due date will change";
        newTask.setTitle(title);

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        long dueTime = new Date(114, 1, 13).getTime();
        DateTime dueTimeString = GtasksApiUtilities.unixTimeToGtasksDueDate(dueTime);
        newTask.setDue(dueTimeString);
        newTask = service.updateGtask(DEFAULT_LIST, newTask);
        //assertEquals(dueTimeString, GtasksApiUtilities.gtasksDueTimeStringToLocalTimeString(newTask.due));
        assertEquals(dueTime, GtasksApiUtilities.gtasksDueTimeToUnixTime(newTask.getDue(), 0));

        long compTime = new Date(115, 2, 14).getTime();
        DateTime compTimeString = GtasksApiUtilities.unixTimeToGtasksCompletionTime(compTime);
        newTask.setCompleted(compTimeString);
        newTask.setStatus("completed");
        newTask = service.updateGtask(DEFAULT_LIST, newTask);
        //assertEquals(compTimeString, GtasksApiUtilities.gtasksCompletedTimeStringToLocalTimeString(newTask.completed));
        assertEquals(compTime, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(newTask.getCompleted(), 0));
    }

    public void testTaskDeleted() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "This task will be deleted";
        newTask.setTitle(title);

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        service.deleteGtask(DEFAULT_LIST, newTask.getId());
        assertFalse(taskWithTitleExists(title));
    }

    public void testTaskMoved() throws Exception {
        if(bypassTests) return;
        Task newTask1 = new Task();
        String title1 = "Task 1";
        newTask1.setTitle(title1);

        Task newTask2 = new Task();
        String title2 = "Task 2";
        newTask2.setTitle(title2);

        newTask1 = service.createGtask(DEFAULT_LIST, newTask1);
        newTask2 = service.createGtask(DEFAULT_LIST, newTask2);

        assertTrue(taskWithTitleExists(title1));
        assertTrue(taskWithTitleExists(title2));

        System.err.println("Task 1 id: " + newTask1.getId());
        System.err.println("Task 2 id: " + newTask2.getId());

        service.moveGtask(DEFAULT_LIST, newTask1.getId(), newTask2.getId(), null);
        newTask1 = service.getGtask(DEFAULT_LIST, newTask1.getId());
        newTask2 = service.getGtask(DEFAULT_LIST, newTask2.getId());

        assertEquals(newTask1.getParent(), newTask2.getId());

        service.moveGtask(DEFAULT_LIST, newTask1.getId(), null, newTask2.getId());
        newTask1 = service.getGtask(DEFAULT_LIST, newTask1.getId());
        newTask2 = service.getGtask(DEFAULT_LIST, newTask2.getId());

        assertNull(newTask1.getParent());
        assertTrue(newTask2.getPosition().compareTo(newTask1.getPosition()) < 0);
    }

    public void testMoveBetweenLists() throws Exception {
        if(bypassTests) return;
        Task newTask = new Task();
        String title = "This task will move lists";
        newTask.setTitle(title);

        newTask = service.createGtask(DEFAULT_LIST, newTask);
        assertTrue(taskWithTitleExists(title));

        String listTitle = "New list";
        service.createGtaskList(listTitle);
        TaskList newList;

        assertNotNull(newList = listWithTitle(listTitle));

        MoveListRequest moveTask = new MoveListRequest(service, newTask.getId(), DEFAULT_LIST, newList.getId(), null);
        moveTask.executePush();

        assertFalse(taskWithTitleExists(title));
        assertTrue(listHasTaskWithTitle(newList.getId(), title));
    }

    private boolean listHasTaskWithTitle(String listId, String title) throws Exception {
        com.google.api.services.tasks.model.Tasks newListTasks = service.getAllGtasksFromListId(listId, false, false, 0);
        List<Task> items = newListTasks.getItems();
        if (items != null) {
            for (Task t : items) {
                if (t.getTitle().equals(title)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean taskWithTitleExists(String title) throws Exception {
        Tasks defaultList = service.getAllGtasksFromListId(DEFAULT_LIST, false, false, 0);
        List<Task> items = defaultList.getItems();
        if (items != null) {
            for (Task t : items) {
                if (t.getTitle().equals(title))
                    return true;
            }
        }
        return false;
    }

    public void testCreateList() throws Exception {
        if(bypassTests) return;
        String title1 = "My new list!";
        service.createGtaskList(title1);
        assertNotNull(listWithTitle(title1));

        String title2 = "Another new list!";
        service.createGtaskList("Another new list!");
        assertNotNull(listWithTitle(title2));
        assertNotNull(listWithTitle(title1));
    }

    public void testDeleteList() throws Exception {
        if(bypassTests) return;
        String title = "This list will be deleted";
        TaskList t = service.createGtaskList(title);
        assertNotNull(listWithTitle(title));

        service.deleteGtaskList(t.getId());
        assertNull(listWithTitle(title));
    }

    public void testUpdateListProperties() throws Exception {
        if(bypassTests) return;
        String title1 = "This title will change";
        TaskList t = service.createGtaskList(title1);
        assertNotNull(listWithTitle(title1));

        String title2 = "New title";
        t.setTitle(title2);
        service.updateGtaskList(t);
        assertNotNull(listWithTitle(title2));
        assertNull(listWithTitle(title1));
    }

    private TaskList listWithTitle(String title) throws Exception {
        TaskLists allLists = service.allGtaskLists();
        List<TaskList> items = allLists.getItems();
        for (TaskList t : items) {
            if (t.getTitle().equals(title))
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
                if (accounts.length == 0) {
                    bypassTests = true;
                    return;
                }
                toUse = accounts[0];
            }

            Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, toUse.name);
            AccountManagerFuture<Bundle> accountManagerFuture = manager.manager.getAuthToken(toUse, GtasksInvoker.AUTH_TOKEN_TYPE, true, null, null);

            Bundle authTokenBundle = accountManagerFuture.getResult();
            if (authTokenBundle.containsKey(AccountManager.KEY_INTENT)) {
                Intent i = (Intent) authTokenBundle.get(AccountManager.KEY_INTENT);
                ContextManager.getContext().startActivity(i);
                return;
            }

            String authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            authToken = GtasksTokenValidator.validateAuthToken(getContext(), authToken);

            service = new GtasksInvoker(authToken);

            initialized = true;
        }
        deleteAllLists();
        clearDefaultList();
    }

    private void deleteAllLists() {
        try {
            TaskLists allLists = service.allGtaskLists();
            List<TaskList> items = allLists.getItems();
            for (TaskList t : items) {
                if (!t.getTitle().equals("Default List"))
                    service.deleteGtaskList(t.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to clear lists");
        }
    }

    private void clearDefaultList() {
        try {
            Tasks tasks = service.getAllGtasksFromListId(DEFAULT_LIST, false, false, 0);
            List<Task> items = tasks.getItems();
            if (items != null) {
                for (Task t : items) {
                    service.deleteGtask(DEFAULT_LIST, t.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to clear default list");
        }
    }

}
