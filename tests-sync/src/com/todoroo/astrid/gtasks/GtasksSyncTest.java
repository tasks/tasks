/*package com.todoroo.astrid.gtasks;
import java.util.ArrayList;
import java.util.List;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;


public class GtasksSyncTest extends DatabaseTestCase {

    private static final String TEST_USERNAME = "astridtasktest@gmail.com";
    private static final String TEST_PASSWORD = "tasktest0000";

    private static boolean initialized = false;
    private static GoogleTaskService testService;
    private static GoogleTaskListInfo taskList;

    @Autowired TaskService taskService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    // --- tests

    // !!! This test is disabled until it works :(

    public void DISABLED_testBasicTaskCreation() throws Exception {
        Task task = givenTask("wasabi");

        whenSynchronizing();

        thenAssertTaskExistsRemotely(task);
    }

    public void DISABLED_testTaskWithDueDate() throws Exception {
        Task task = givenTask("wasabi");
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                DateUtilities.now()));

        whenSynchronizing();

        GoogleTaskTask remote = thenAssertTaskExistsRemotely(task);
        assertTrue(remote.getTask_date() > DateUtilities.now() - DateUtilities.ONE_DAY);
        assertEquals(task.getValue(Task.DUE_DATE), refetchLocal(task).getValue(Task.DUE_DATE));
    }

    public void DISABLED_testTaskWithDueTime() throws Exception {
        Task task = givenTask("wasabi");
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                DateUtilities.now()));

        whenSynchronizing();

        GoogleTaskTask remote = thenAssertTaskExistsRemotely(task);
        assertTrue(remote.getTask_date() > DateUtilities.now() - DateUtilities.ONE_DAY);
        assertEquals(task.getValue(Task.DUE_DATE), refetchLocal(task).getValue(Task.DUE_DATE));
    }

    // --- helpers

    private Task refetchLocal(Task task) {
        return taskService.fetchById(task.getId(), Task.PROPERTIES);
    }

    private GoogleTaskTask thenAssertTaskExistsRemotely(Task task) throws Exception {
        List<GoogleTaskTask> tasks = testService.getTasks(taskList.getId());
        for(GoogleTaskTask remote : tasks) {
            if(remote.getName().equals(task.getValue(Task.TITLE)))
                return remote;
        }
        fail("Task didn't exist remotely: " + task);
        return null;
    }

    private void whenSynchronizing() {
        new GtasksSyncProvider().synchronize(getContext());
    }

    private Task givenTask(String title) {
        Task task = new Task();
        task.setValue(Task.TITLE, title + System.currentTimeMillis());
        taskService.save(task);
        return task;
    }

    // --- setup stuff

    // set up task list and clean it out
    protected void DISABLED_setUp() throws Exception {
        super.setUp();

        if(!initialized)
            initializeTestService();

        ArrayList<ListAction> actions = new ArrayList<ListAction>();
        ListActions l = new ListActions();
        for(GoogleTaskTask task : testService.getTasks(taskList.getId())) {
            actions.add(l.modifyTask(task.getId()).deleted(true).done());
        }

        testService.executeListActions(taskList.getId(), actions.toArray(new ListAction[actions.size()]));
    }

    public void initializeTestService() throws Exception {
        GoogleConnectionManager gcm = new GoogleConnectionManager(TEST_USERNAME, TEST_PASSWORD);
        testService = new GoogleTaskService(gcm);
        GoogleTaskView taskView = testService.getTaskView();
        GoogleTaskListInfo[] lists = taskView.getAllLists();
        outer: {
            for(GoogleTaskListInfo list : lists) {
                if("AstridUnitTests".equals(list.getName())) {
                    taskList = list;
                    break outer;
                }
            }
            fail("could not find the main list");
        }
        initialized = true;

        Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, taskList.getId());
        Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, TEST_USERNAME);
        Preferences.setString(GtasksPreferenceService.PREF_PASSWORD, TEST_PASSWORD);
        gtasksPreferenceService.setToken(gcm.getToken());
    }



}//*/
