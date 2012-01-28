package com.todoroo.astrid.producteev;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.producteev.api.ApiUtilities;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class ProducteevSyncTest extends DatabaseTestCase {

    private static final String TEST_USER = "sync_tester@astrid.com";
    private static final String TEST_PASSWORD = "wonky1234";

    private static final String TEST_USER_2 = "sync_tester2@astrid.com";

    private static ProducteevSyncProvider syncProvider;
    private static ProducteevInvoker invoker;

    private static final long TIME_BETWEEN_SYNCS = 3000l;

    private boolean initialized = false;
    private int dashboardId;

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired TagService tagService;
    private ProducteevDataService producteevDataService;

    /*
     * Basic creation tests
     */
    public void testTaskCreatedLocally() {
        String title = "Astrid task 1";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        assertTaskExistsRemotely(localTask, title);
    }

    public void testTaskCreatedRemotely() {
        JSONObject remoteTask = createNewRemoteTask("Producteev task 1");

        whenInvokeSync();

        assertTaskExistsLocally(remoteTask);
    }

    /*
     * Title editing tests
     */
    public void testNameChangedLocally() {
        String title = "Astrid task 2";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new title on local task
        String newTitle = "Astrid task 2 edited";
        localTask.setValue(Task.TITLE, newTitle);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch remote task and assert that both local and remote titles match expected
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newTitle, localTask.getValue(Task.TITLE));
        assertEquals(newTitle, getRemoteTitle(remoteTask));
    }

    public void testNameChangedRemotely() throws Exception {
        String title = "Astrid task 3";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new title on remote task
        String newRemoteTitle = "Task 3 edited on producteev";
        invoker.tasksSetTitle(getRemoteId(remoteTask), newRemoteTitle);

        whenInvokeSync();

        //Refetch local/remote tasks, assert that both titles match expected
        remoteTask = refetchRemoteTask(remoteTask);
        localTask = refetchLocalTask(localTask);
        assertEquals(newRemoteTitle, getRemoteTitle(remoteTask));
        assertEquals(newRemoteTitle, localTask.getValue(Task.TITLE));
    }

    /*
     * Tests for note creation
     */
    public void testNoteChangedLocally() {
        String title = "Astrid task 4";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new note on local task
        String newNote1 = "This is a new note";
        localTask.setValue(Task.NOTES, newNote1);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch and assert that the note has been pushed to producteev
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote1, getFirstNote(remoteTask));
        assertEquals(newNote1, localTask.getValue(Task.NOTES));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);
        //Create a new note locally
        String newNote2 = "This is a second note";
        localTask.setValue(Task.NOTES, newNote2);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch and assert that new note has been pushed and old note is still there
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote2, getFirstNote(remoteTask));
        assertEquals(newNote1, getSecondNote(remoteTask));
        assertEquals(newNote2, localTask.getValue(Task.NOTES));
    }

    public void testNoteChangedRemotely() throws Exception {
        String title = "Astrid task 5";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Create a new remote note
        String newNote1 = "Note added in producteev";
        invoker.tasksNoteCreate(getRemoteId(remoteTask), "Note added in producteev");

        whenInvokeSync();

        //Refetch both and assert new note exists, local note does not (?)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote1, getFirstNote(remoteTask));
        assertEquals("", localTask.getValue(Task.NOTES));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Create a second note remotely
        String newNote2 = "Second producteev note";
        invoker.tasksNoteCreate(getRemoteId(remoteTask), newNote2);

        whenInvokeSync();

        //Refetch both and assert two remote notes exist, local note does not (?)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newNote2, getFirstNote(remoteTask));
        assertEquals(newNote1,getSecondNote(remoteTask));
        assertEquals("", localTask.getValue(Task.NOTES));
    }

    /*
     * Helper methods for notes
     */
    private String getFirstNote(JSONObject remoteTask) {
        return getNoteAtIndex(remoteTask, 0);
    }

    private String getSecondNote(JSONObject remoteTask) {
        return getNoteAtIndex(remoteTask, 1);
    }

    private String getNoteAtIndex(JSONObject remoteTask, int index) {
        String result = null;
        try {
            result = getRemoteNotesArray(remoteTask).getJSONObject(index).getJSONObject("note").getString("message");
        } catch (Exception e) {
            fail("Failed to extract note from notes array");
        }
        return result;
    }

    /*
     * Due date editing tests
     */
    public void testDateChangedLocally() {
        Task localTask = createLocalTaskForDateTests(" locally");
        String title = localTask.getValue(Task.TITLE);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        //Assert the originally set due date matches between local/remote tasks
        assertEquals(localTask.getValue(Task.DUE_DATE).longValue(), getRemoteDueDate(remoteTask));
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on local task
        long newDueDate = new Date(116, 1, 8).getTime();
        localTask.setValue(Task.DUE_DATE, newDueDate);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch remote task and assert that both tasks match expected due date
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newDueDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newDueDate, getRemoteDueDate(remoteTask));
    }

    public void testDateChangedRemotely() throws Exception {
        Task localTask = createLocalTaskForDateTests(" remotely");
        String title = localTask.getValue(Task.TITLE);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        //Assert the originally set due date matches between local/remote tasks
        assertEquals(localTask.getValue(Task.DUE_DATE).longValue(), getRemoteDueDate(remoteTask));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on remote task
        long newDueDate = new Date(116, 1, 8).getTime();
        invoker.tasksSetDeadline(getRemoteId(remoteTask), ApiUtilities.unixTimeToProducteev(newDueDate), 0);

        whenInvokeSync();

        //Refetch remote/local tasks and assert that due dates match expected due date
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);

        assertEquals(newDueDate, getRemoteDueDate(remoteTask));
        assertEquals(newDueDate, localTask.getValue(Task.DUE_DATE).longValue());
    }

    public void testDateChangedBoth_ChooseLocal() throws Exception{
        Task localTask = createLocalTaskForDateTests(" in both");
        String title = localTask.getValue(Task.TITLE);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(localTask.getValue(Task.DUE_DATE).longValue(), getRemoteDueDate(remoteTask));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new due date on remote task first
        long newLocalDate = new Date(128, 5, 11).getTime();
        long newRemoteDate = new Date(121, 5, 25).getTime();

        invoker.tasksSetDeadline(getRemoteId(remoteTask), ApiUtilities.unixTimeToProducteev(newRemoteDate), 0);

        //Sleep between updates to tasks to establish which should be dominant in sync
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new local date second
        localTask.setValue(Task.DUE_DATE, newLocalDate);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch both and assert that due dates match the one we set to local (more recent)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newLocalDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newLocalDate, getRemoteDueDate(remoteTask));
    }

    public void DISABLED_testDateChangedBoth_ChooseRemote() throws Exception { //This test currently fails--known bug/expected behavior?
        Task localTask = createLocalTaskForDateTests(" in both");
        String title = localTask.getValue(Task.TITLE);


        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(localTask.getValue(Task.DUE_DATE).longValue(), getRemoteDueDate(remoteTask));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new local date first
        long newLocalDate = new Date(128, 5, 11).getTime();
        long newRemoteDate = new Date(121, 5, 25).getTime();

        localTask.setValue(Task.DUE_DATE, newLocalDate);
        taskService.save(localTask);

        //Sleep between updates to tasks to establish which should be dominant in sync
        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new remote date second
        invoker.tasksSetDeadline(getRemoteId(remoteTask), ApiUtilities.unixDateToProducteev(newRemoteDate), 0);

        whenInvokeSync();

        //Refetch both and assert that dates match the one we set to remote (more recent)
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newRemoteDate, localTask.getValue(Task.DUE_DATE).longValue());
        assertEquals(newRemoteDate, getRemoteDueDate(remoteTask));
    }

    /*
     * Helper method for due date methods
     */
    private Task createLocalTaskForDateTests(String addToTitle) {
        Task localTask = createNewLocalTask("Due date will change" + addToTitle);
        long dueDate = new Date(115, 2, 14).getTime();
        localTask.setValue(Task.DUE_DATE, dueDate);
        taskService.save(localTask);

        return localTask;
    }

    /*
     * Priority editing tests
     */
    public void testPriorityChangedLocally() {
        String title = "Priority will change locally";
        Task localTask = createNewLocalTask(title);
        localTask.setValue(Task.IMPORTANCE, Task.IMPORTANCE_MOST);
        taskService.save(localTask);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(expectedNumStars(Task.IMPORTANCE_MOST), getStars(remoteTask));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new local importance
        int newImportance = Task.IMPORTANCE_LEAST;
        localTask.setValue(Task.IMPORTANCE, newImportance);
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch and assert both match expected priority/# of stars
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(newImportance, localTask.getValue(Task.IMPORTANCE).intValue());
        assertEquals(expectedNumStars(newImportance), getStars(remoteTask));
    }

    public void testPriorityChangedRemotely() throws Exception {
        String title = "Priority will change remotely";
        Task localTask = createNewLocalTask(title);
        localTask.setValue(Task.IMPORTANCE, Task.IMPORTANCE_MUST_DO);
        taskService.save(localTask);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);
        assertEquals(expectedNumStars(Task.IMPORTANCE_MUST_DO), getStars(remoteTask));

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new remote # of stars
        int numStars = 0;
        invoker.tasksSetStar(getRemoteId(remoteTask), numStars);

        whenInvokeSync();

        //Refetch and assert both match expected priority/# of stars
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(numStars, getStars(remoteTask));
        assertEquals(expectedPriority(numStars), localTask.getValue(Task.IMPORTANCE).intValue());
    }

    /*
     * Helper methods for priority tests
     */
    private int getStars(JSONObject remoteTask) {
        int numStars = -1;
        try {
            numStars = remoteTask.getInt("star");
        } catch (Exception e){
            fail("Failed to extract stars count from remote object");
        }
        return numStars;
    }

    private int expectedNumStars(int importance) {
        return 5 - importance;
    }

    private int expectedPriority(int numStars) {
        return 5 - numStars;
    }

    /*
     * Tests for label creation
     */
    public void testLabelCreatedLocally() throws Exception {
        String title = "This task will get a tag";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set a new local tag on the task
        Metadata newTagData = new Metadata();
        newTagData.setValue(Metadata.TASK, localTask.getId());
        newTagData.setValue(Metadata.KEY, TagService.KEY);
        String newTag = "This is a new tag";
        newTagData.setValue(TagService.TAG, newTag);
        metadataService.save(newTagData);
        assertLocalTaskHasTag(localTask, newTag);
        localTask.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        taskService.save(localTask);

        whenInvokeSync();

        //Refetch remote; assert the label exists remotely both in general and on the remote task
        remoteTask = refetchRemoteTask(remoteTask);
        assertLocalTaskHasTag(localTask, newTag);
        assertRemoteTaskHasLabel(remoteTask, newTag);
        assertRemoteLabelListHasLabel(newTag);
    }

    public void testLabelCreatedRemotely() throws Exception {
        String title = "This task will get a tag";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();

        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Remotely create a new label and set it to the remote task
        String newTag = "Remote label";
        JSONObject newLabel = invoker.labelsCreate(dashboardId, newTag).getJSONObject("label");
        long labelId = newLabel.getLong("id_label");

        invoker.tasksChangeLabel(getRemoteId(remoteTask), labelId);

        whenInvokeSync();

        //Refetch and assert that the remote task still has the label and that the local task does too
        localTask = refetchLocalTask(localTask);
        remoteTask = refetchRemoteTask(remoteTask);

        assertRemoteLabelListHasLabel(newTag);
        assertRemoteTaskHasLabel(remoteTask, newTag);

        invoker.labelsDelete(labelId); //Cleanup the test account by deleting the new remote label
        assertLocalTaskHasTag(localTask, newTag);
    }

    /*
     * Helper methods for tag/label tests
     */
    private void assertLocalTaskHasTag(Task localTask, String tag) {
        TodorooCursor<Metadata> tags = tagService.getTags(localTask.getId());
        boolean foundTag = false;
        try {
            assertTrue(tags.getCount() > 0);
            int length = tags.getCount();
            Metadata m = new Metadata();
            for (int i = 0; i < length; i++) {
                tags.moveToNext();
                m.readFromCursor(tags);
                if(tag.equals(m.getValue(TagService.TAG))) {
                    foundTag = true;
                    break;
                }
            }
            if (!foundTag) throw new Exception();
        } catch (Exception e) {
            fail("Local task does not have tag " + tag);
        } finally {
            tags.close();
        }
    }

    private void assertRemoteTaskHasLabel(JSONObject remoteTask, String label) {
        boolean foundLabel = false;
        try {
            JSONArray labels = invoker.tasksLabels(getRemoteId(remoteTask));
            foundLabel = searchForLabel(labels, label);
        } catch (Exception e){
            fail("Couldn't extract labels from task");
        }
        if (!foundLabel) fail("Remote task did not have label " + label);
    }

    private void assertRemoteLabelListHasLabel(String label) {
        boolean foundLabel = false;
        try {
            JSONArray labels = invoker.labelsShowList(dashboardId, null);
            System.err.println(labels);
            foundLabel = searchForLabel(labels, label);
        } catch (Exception e) {
            fail("Couldn't parse label list");
        }
        if (!foundLabel) fail("Remote list did not have label " + label);
    }

    private boolean searchForLabel(JSONArray labels, String toFind) throws Exception {
        for (int i = 0; i < labels.length(); i++) {
            String currLabel = labels.getJSONObject(i).getJSONObject("label").getString("title");
            if (currLabel.equals(toFind)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Assignment editing tests
     */
    public void testAssignmentChangedLocally() throws Exception {
        String title = "This will get assigned to sync_tester2 locally";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();
        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set the colleague id locally to be sync_tester2
        Metadata producteevData = producteevDataService.getTaskMetadata(localTask.getId());
        producteevData.setValue(Metadata.TASK, localTask.getId());
        producteevData.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
        long colleagueId = getColleagueId();
        producteevData.setValue(ProducteevTask.RESPONSIBLE_ID, colleagueId);
        metadataService.save(producteevData);
        localTask.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        taskService.save(localTask);
        assertEquals(colleagueId, producteevData.getValue(ProducteevTask.RESPONSIBLE_ID).longValue());

        whenInvokeSync();

        //Get local metadata for the task and assert that the responsible id matches the expected colleague id
        Metadata localMetadata = producteevDataService.getTaskMetadata(localTask.getId());
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(colleagueId, localMetadata.getValue(ProducteevTask.RESPONSIBLE_ID).longValue());
        assertEquals(colleagueId, getRemoteResponsible(remoteTask));
        //Assert that the email of that person is in fact sync_tester2
        assertEquals(TEST_USER_2, getColleagueEmail(colleagueId));
    }

    public void testAssignmentChangedRemotely() throws Exception {
        String title = "This will get assigned to sync_tester2 remotely";
        Task localTask = createNewLocalTask(title);

        whenInvokeSync();
        JSONObject remoteTask = assertTaskExistsRemotely(localTask, title);

        AndroidUtilities.sleepDeep(TIME_BETWEEN_SYNCS);

        //Set new responsible id (the id for sync_tester)
        long colleagueId = getColleagueId();
        invoker.tasksSetResponsible(getRemoteId(remoteTask), getColleagueId());

        whenInvokeSync();


        //Get local metadata for the task and assert that the responsible id matches the expected colleague id
        Metadata localMetadata = producteevDataService.getTaskMetadata(localTask.getId());
        remoteTask = refetchRemoteTask(remoteTask);
        assertEquals(colleagueId, localMetadata.getValue(ProducteevTask.RESPONSIBLE_ID).longValue());
        assertEquals(colleagueId, getRemoteResponsible(remoteTask));
        //Assert that the email of that person is in fact sync_tester2
        assertEquals(TEST_USER_2, getColleagueEmail(colleagueId));
    }

    /*
     * Helper methods for assignment tests
     */
    private long getColleagueId() {
        long id = -1;
        try {
            JSONArray colleagues = invoker.usersColleagues().getJSONArray("colleagues");
            JSONObject colleagueObj = colleagues.getJSONObject(0).getJSONObject("user");
            id = colleagueObj.getLong("id_user");
        } catch (Exception e) {
            fail("Unable to extract colleague id");
        }
        return id;
    }

    private long getRemoteResponsible(JSONObject remoteTask) {
        long responsibleId = -1;
        try {
            responsibleId = remoteTask.getLong("id_responsible");
        } catch (Exception e) {
            fail("Unable to extract responsible id field");
        }
        return responsibleId;
    }

    private String getColleagueEmail(long colleagueId) {
        String email = null;
        try {
            JSONObject user = invoker.usersView(colleagueId).getJSONObject("user");
            email = user.getString("email");
        } catch (Exception e) {
            fail("Failed to extract colleague email");
        }
        return email;
    }

    /*
     * Helper method that looks for a local task's remote counterpart, asserts the titles
     * are both what is expected, and returns the remote JSON object
     */
    private JSONObject assertTaskExistsRemotely(Task task, String expectedTitle) {
        long remoteId = remoteIdForTask(task);
        JSONObject remoteTask = remoteTaskWithId(remoteId);
        assertNotNull(remoteTask);
        assertEquals(expectedTitle, task.getValue(Task.TITLE));
        assertEquals(expectedTitle, getRemoteTitle(remoteTask));
        return remoteTask;
    }

    /*
     * Extracts the remote id from a remote task JSON object
     */
    private long getRemoteId(JSONObject remoteTask) {
        long remoteId = 0;
        try {
            remoteId = remoteTask.getLong("id_task");
        } catch (Exception e) {
            fail("Remote task object did not contain id_task field");
        }
        return remoteId;
    }

    /*
     * Extracts the remote due date from a remote task JSON object
     */
    private long getRemoteDueDate(JSONObject remoteTask) {
        long remoteDueDate = 0;
        try {
            remoteDueDate = ApiUtilities.producteevToUnixTime(remoteTask.getString("deadline"), 0);
        } catch (Exception e) {
            fail("Couldn't fetch time from remote task");
        }
        return remoteDueDate;
    }

    /*
     * Extracts the remote title from a remote task JSON object
     */
    private String getRemoteTitle(JSONObject remoteTask) {
        String result = null;
        try {
            result = ApiUtilities.decode(remoteTask.getString("title"));
        } catch (Exception e) {
            fail("Remote task object did not contain title field");
        }
        return result;
    }

    /*
     * Extract the remote notes array from a remote task JSON object
     */
    private JSONArray getRemoteNotesArray(JSONObject remoteTask) {
        JSONArray result = null;
        try {
            result = remoteTask.getJSONArray("notes");
        } catch (Exception e) {
            fail("Remote task object did not contain notes field");
        }
        return result;
    }

    /*
     * Looks up and returns a remote task JSON object with the given remote id
     */
    private JSONObject remoteTaskWithId(long remoteId) {
        JSONObject remoteTask = null;
        try {
            remoteTask = invoker.tasksView(remoteId).getJSONObject("task");
            assertNotNull(remoteTask);
        } catch (Exception e) {
            fail("Failed to find remote task " + remoteId);
        }

        return remoteTask;
    }

    /*
     * Look up a remote tasks's corresponding local task and return the local task object
     */
    private Task assertTaskExistsLocally(JSONObject remoteTask) {
        long localId = localIdForTask(remoteTask);

        //Fetch the local task from the database
        Task localTask = taskService.fetchById(localId, Task.PROPERTIES);

        assertNotNull(localTask);
        assertEquals(getRemoteTitle(remoteTask), localTask.getValue(Task.TITLE));
        return localTask;
    }

    /*
     * Maps a local task to the corresponding remote task's id
     */
    private long remoteIdForTask(Task t) {
        return producteevDataService.getTaskMetadata(t.getId()).getValue(ProducteevTask.ID);
    }

    /*
     * Maps a remote task to the corresponding local task's id
     */
    private long localIdForTask(JSONObject remoteTask) {
        Long remoteId = 0l;
        try {
            remoteId = remoteTask.getLong("id_task");
        } catch (Exception e) {
            fail("Couldn't extract id from remote task object");
        }
        TodorooCursor<Metadata> cursor = metadataService.query(Query.select(Metadata.TASK).
                where(Criterion.and(Metadata.KEY.eq(ProducteevTask.METADATA_KEY), ProducteevTask.ID.eq(remoteId))));
        try {
            assertEquals(1, cursor.getCount());

            cursor.moveToFirst();
            return cursor.get(Metadata.TASK);
        } finally {
            cursor.close();
        }
    }

    /*
     * Start synchronizing with Producteev
     */
    private void whenInvokeSync() {
        syncProvider.synchronize(getContext());
    }

    /*
     * Create and save a new local task with the given title
     */
    private Task createNewLocalTask(String title) {
        Task task = new Task();
        task.setValue(Task.TITLE, title);
        taskService.save(task);
        return task;
    }

    /*
     * Create a new remote task with the given title
     */
    private JSONObject createNewRemoteTask(String title) {
        JSONObject toReturn = null;
        try {
            toReturn = invoker.tasksCreate(title, null,
                    null, null, null, null, null).getJSONObject("task");
            System.err.println(toReturn);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create remote task");
        }
        return toReturn;
    }

    /*
     * Refetch and return local task object from the database
     */
    private Task refetchLocalTask(Task localTask) {
        return taskService.fetchById(localTask.getId(), Task.PROPERTIES);
    }

    /*
     * Refetch and return a remote task with the same id
     */
    private JSONObject refetchRemoteTask(JSONObject remoteTask) {
        return remoteTaskWithId(getRemoteId(remoteTask));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_SYNC, null);
        if (!initialized) {
            initializeTestService();
        }
        clearAllRemoteTasks();
    }

    private void initializeTestService() throws Exception {
        //Set the username and password for the service
        Preferences.setString(R.string.producteev_PPr_email, TEST_USER);
        Preferences.setString(R.string.producteev_PPr_password, TEST_PASSWORD);

        invoker = ProducteevSyncProvider.getInvoker();
        producteevDataService = ProducteevDataService.getInstance();

        //clear dashboard cache since database was empty
        producteevDataService.updateDashboards(new JSONArray());

        invoker.authenticate(TEST_USER, TEST_PASSWORD);

        //Get the id for the default dashboard (needed for some tests)
        dashboardId = invoker.dashboardsShowList(null).getJSONObject(0).getJSONObject("dashboard").getInt("id_dashboard");

        syncProvider = new ProducteevSyncProvider();

        initialized = true;
    }

    /*
     * Deletes all the remote tasks on the default dashboard to ensure
     * clean tests.
     */
    private void clearAllRemoteTasks() {
        try {
            JSONArray remoteTasks = invoker.tasksShowList(null, null);
            for (int i = 0; i < remoteTasks.length(); i++) {
                JSONObject task = remoteTasks.getJSONObject(i).getJSONObject("task");
                invoker.tasksDelete(getRemoteId(task));
            }
        } catch (Exception e) {
            fail("Failed to clear remote tasks before tests");
        }

    }
}
