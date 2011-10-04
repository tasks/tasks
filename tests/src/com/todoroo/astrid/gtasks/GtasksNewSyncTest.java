package com.todoroo.astrid.gtasks;

import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.services.tasks.model.Tasks;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksNewSyncTest extends DatabaseTestCase {

    private static GtasksService gtasksService;
    private GtasksSyncProvider syncProvider;
    private static boolean initialized = false;
    private boolean bypassTests = false;

    private static String DEFAULT_LIST = "@default";
    private static final String TEST_ACCOUNT = "sync_tester2@astrid.com";
    private static final long TIME_BETWEEN_SYNCS = 3000l;

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    /*
     * Basic creation tests
     */
    public void testTaskCreatedLocally() {
        if(bypassTests) return;
        String title = "Astrid task 1";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        assertTaskExistsRemotely(localTask, title);
    }

    public void testTaskCreatedRemotely() throws Exception {
        if(bypassTests) return;
        String title = "Gtasks task 1";
        com.google.api.services.tasks.model.Task remoteTask = new com.google.api.services.tasks.model.Task();
        remoteTask.setTitle(title);
        remoteTask = gtasksService.createGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        assertTaskExistsLocally(remoteTask, title);
    }

    /*
     * Title editing tests
     */
    public void testTitleChangedLocally() throws Exception {
        if(bypassTests) return;
        String title = "Astrid task 2";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new title on local task
        String newTitle = "Astrid task 2 edited";
        localTask.setValue(Task.TITLE, newTitle);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch remote task and assert that both local and remote titles match expected
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newTitle, localTask.getValue(Task.TITLE));
        assertEquals(newTitle, remoteTask.getTitle());
    }

    public void testTitleChangedRemotely() throws Exception {
        if(bypassTests) return;
        String title = "Astrid task 3";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new title on remote task
        String newRemoteTitle = "Task 3 edited on gtasks";
        remoteTask.setTitle(newRemoteTitle);
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        //Refetch local/remote tasks, assert that both titles match expected
        remoteTask = refetchRemoteTask(remoteTask);
        localTask = refetchLocalTask(localTask);
        assertEquals(newRemoteTitle, remoteTask.getTitle());
        assertEquals(newRemoteTitle, localTask.getValue(Task.TITLE));
    }

    public void testDateChangedLocally() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForDateTests(" locally");
        String title = localTask.getValue(Task.TITLE);
        long startDate = localTask.getValue(Task.DUE_DATE);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        localTask = refetchLocalTask(localTask);
        assertTrue(String.format("Expected %s, was %s", new Date(startDate), new Date(localTask.getValue(Task.DUE_DATE))),
                Math.abs(startDate - localTask.getValue(Task.DUE_DATE)) < 5000);
        assertEquals(startDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on local task
        long newDueDate = new Date(116, 1, 8).getTime();
        localTask.setValue(Task.DUE_DATE, newDueDate);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch remote task and assert that both tasks match expected due date
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newDueDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newDueDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
    }

    public void testDateChangedRemotely() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForDateTests(" remotely");
        String title = localTask.getValue(Task.TITLE);
        long startDate = localTask.getValue(Task.DUE_DATE);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        localTask = refetchLocalTask(localTask);
        assertTrue(String.format("Expected %s, was %s", new Date(startDate), new Date(localTask.getValue(Task.DUE_DATE))),
                Math.abs(startDate - localTask.getValue(Task.DUE_DATE)) < 5000);
        assertEquals(startDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on remote task
        long newDueDate = new Date(116, 1, 8).getTime();
        remoteTask.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(newDueDate));
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        //Refetch remote task and assert that both tasks match expected due date
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newDueDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newDueDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));

    }

    public void testDateChangedBoth_ChooseLocal() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForDateTests(" remotely");
        String title = localTask.getValue(Task.TITLE);
        long startDate = localTask.getValue(Task.DUE_DATE);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        localTask = refetchLocalTask(localTask);
        assertTrue(String.format("Expected %s, was %s", new Date(startDate), new Date(localTask.getValue(Task.DUE_DATE))),
                Math.abs(startDate - localTask.getValue(Task.DUE_DATE)) < 5000);
        assertEquals(startDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on remote task first
        long newLocalDate = new Date(128, 5, 11).getTime();
        long newRemoteDate = new Date(121, 5, 25).getTime();

        remoteTask.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(newRemoteDate));
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        localTask.setValue(Task.DUE_DATE, newLocalDate);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch both and assert that due dates match the one we set to local (more recent)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newLocalDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newLocalDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
    }

    public void DISABLED_testDateChangedBoth_ChooseRemote() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForDateTests(" remotely");
        String title = localTask.getValue(Task.TITLE);
        long startDate = localTask.getValue(Task.DUE_DATE);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        localTask = refetchLocalTask(localTask);
        assertTrue(String.format("Expected %s, was %s", new Date(startDate), new Date(localTask.getValue(Task.DUE_DATE))),
                Math.abs(startDate - localTask.getValue(Task.DUE_DATE)) < 5000);
        assertEquals(startDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);


        //Set new due date on local task first
        long newLocalDate = new Date(128, 5, 11).getTime();
        long newRemoteDate = new Date(121, 5, 25).getTime();

        localTask.setValue(Task.DUE_DATE, newLocalDate);
        taskService.save(localTask);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        remoteTask.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(newRemoteDate));
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        //Refetch both and assert that due dates match the one we set to local (more recent)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newLocalDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newLocalDate, GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0));
    }

    /*
     * Helper method for due date tests
     */
    private Task createLocalTaskForDateTests(String addToTitle) {
        Task localTask = createNewLocalTask("Due date will change" + addToTitle);
        long dueDate = new Date(115, 2, 14).getTime();
        localTask.setValue(Task.DUE_DATE, dueDate);
        taskService.save(localTask);

        return localTask;
    }

    public void testNoteEditedLocally() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForNoteTests(" locally");
        String title = localTask.getValue(Task.TITLE);
        String originalNote = localTask.getValue(Task.NOTES);

        whenInvokeSync();

        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(originalNote, localTask.getValue(Task.NOTES));
        assertEquals(originalNote, remoteTask.getNotes());

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        String newNote = "New local note";
        localTask.setValue(Task.NOTES, newNote);
        taskService.save(localTask);

        whenInvokeSync();

        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote, localTask.getValue(Task.NOTES));
        assertEquals(newNote, remoteTask.getNotes());
    }

    public void testNoteEditedRemotely() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForNoteTests(" remotely");
        String title = localTask.getValue(Task.TITLE);
        String originalNote = localTask.getValue(Task.NOTES);

        whenInvokeSync();

        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(originalNote, localTask.getValue(Task.NOTES));
        assertEquals(originalNote, remoteTask.getNotes());

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        String newNote = "New remote note";
        remoteTask.setNotes(newNote);
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote, localTask.getValue(Task.NOTES));
        assertEquals(newNote, remoteTask.getNotes());
    }

    public void DISABLED_testNoteEditedBoth() throws Exception {
        if(bypassTests) return;
        Task localTask = createLocalTaskForNoteTests(" remotely");
        String title = localTask.getValue(Task.TITLE);
        String originalNote = localTask.getValue(Task.NOTES);

        whenInvokeSync();

        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(originalNote, localTask.getValue(Task.NOTES));
        assertEquals(originalNote, remoteTask.getNotes());

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        String newLocalNote = "New local note";
        String newRemoteNote = "New remote note";

        localTask.setValue(Task.NOTES, newLocalNote);
        taskService.save(localTask);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        remoteTask.setNotes(newRemoteNote);
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        System.err.println("Local note: " + localTask.getValue(Task.NOTES));
        System.err.println("Remote note: " + remoteTask.getNotes());
    }

    private Task createLocalTaskForNoteTests(String addToTitle) {
        Task localTask = createNewLocalTask("Note will change" + addToTitle);
        String note = "Original note";
        localTask.setValue(Task.NOTES, note);
        taskService.save(localTask);

        return localTask;
    }

    /*
     * Completion tests
     */

    public void testTaskCompletedLocally() throws Exception {
        if(bypassTests) return;
        String title = "Will complete locally";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        long completion = DateUtilities.now();
        localTask.setValue(Task.COMPLETION_DATE, completion);
        taskService.save(localTask);

        whenInvokeSync();

        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertTrue(String.format("Expected %s, was %s", new Date(completion), new Date(localTask.getValue(Task.COMPLETION_DATE))),
                Math.abs(completion - localTask.getValue(Task.COMPLETION_DATE)) < 5000);
        assertEquals("completed", remoteTask.getStatus());
    }

    public void testTaskCompletedRemotely() throws Exception {
        if(bypassTests) return;
        String title = "Will complete remotely";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();
        com.google.api.services.tasks.model.Task remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        long completion = DateUtilities.now();
        remoteTask.setStatus("completed");
        remoteTask.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(completion));
        gtasksService.updateGtask(DEFAULT_LIST, remoteTask);

        whenInvokeSync();

        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertTrue(String.format("Expected %s, was %s", new Date(completion), new Date(localTask.getValue(Task.COMPLETION_DATE))),
                Math.abs(completion - localTask.getValue(Task.COMPLETION_DATE)) < 5000);
        assertEquals("completed", remoteTask.getStatus());
    }

    private com.google.api.services.tasks.model.Task assertTaskExistsRemotely(Task localTask, String title) {
        //Get the corresponding remote id for a local task
        Metadata metadata = gtasksMetadataService.getTaskMetadata(localTask.getId());
        String taskId = metadata.getValue(GtasksMetadata.ID);
        String listId = metadata.getValue(GtasksMetadata.LIST_ID);

        //Fetch the remote task belonging to that id
        com.google.api.services.tasks.model.Task remoteTask = null;
        try {
            remoteTask = gtasksService.getGtask(listId, taskId);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to find remote task " + taskId);
        }

        //Do a basic title match
        assertNotNull(remoteTask);
        assertEquals(title, localTask.getValue(Task.TITLE));
        assertEquals(title, remoteTask.getTitle());
        return remoteTask;
    }

    private Task assertTaskExistsLocally(com.google.api.services.tasks.model.Task remoteTask, String title) {
        long localId = localIdForTask(remoteTask);

        //Fetch the local task from the database
        Task localTask = taskService.fetchById(localId, Task.PROPERTIES);

        assertNotNull(localTask);
        assertEquals(title, remoteTask.getTitle());
        assertEquals(title, localTask.getValue(Task.TITLE));
        return localTask;
    }

    private Task refetchLocalTask(Task localTask) {
        return taskService.fetchById(localTask.getValue(Task.ID), Task.PROPERTIES);
    }

    private com.google.api.services.tasks.model.Task refetchRemoteTask(com.google.api.services.tasks.model.Task remoteTask) throws Exception {
        return gtasksService.getGtask(GtasksApiUtilities.extractListIdFromSelfLink(remoteTask), remoteTask.getId());
    }

    private long localIdForTask(com.google.api.services.tasks.model.Task remoteTask) {
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.TASK).
                where(Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY), GtasksMetadata.ID.eq(remoteTask.getId()))));
        try {
            assertEquals(1, cursor.getCount());

            cursor.moveToFirst();
            return cursor.get(Metadata.TASK);
        } finally {
            cursor.close();
        }
    }


  //Create a new Astrid task and save it to the database
    private Task createNewLocalTask(String title) {
        Task task = new Task();
        task.setValue(Task.TITLE, title);
        taskService.save(task);
        return task;
    }

    //Perform a synchronization
    private void whenInvokeSync() {
        syncProvider.synchronize(getContext());
        gtasksService = syncProvider.getGtasksService(); //This is to prevent token mismatch; the sync provider invalidates the old one
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!initialized) {
            initializeTestService();
        }

        setupTestList();

        syncProvider = new GtasksSyncProvider();
    }

    private void initializeTestService() throws Exception {
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
        AccountManagerFuture<Bundle> accountManagerFuture = manager.manager.getAuthToken(toUse, "oauth2:https://www.googleapis.com/auth/tasks", true, null, null);

        Bundle authTokenBundle = accountManagerFuture.getResult();
        if (authTokenBundle.containsKey(AccountManager.KEY_INTENT)) {
            Intent i = (Intent) authTokenBundle.get(AccountManager.KEY_INTENT);
            ContextManager.getContext().startActivity(i);
            return;
        }
        String authToken = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
        authToken = GtasksTokenValidator.validateAuthToken(authToken);
        gtasksPreferenceService.setToken(authToken);

        gtasksService = new GtasksService(authToken);

        initialized = true;
    }

    private void setupTestList() throws Exception {
        Tasks defaultListTasks = gtasksService.getAllGtasksFromListId(DEFAULT_LIST, false, false);
        List<com.google.api.services.tasks.model.Task> items = defaultListTasks.getItems();
        if (items != null) {
            for (com.google.api.services.tasks.model.Task t : items) {
                gtasksService.deleteGtask(DEFAULT_LIST, t.getId());
            }
        }
    }

}
