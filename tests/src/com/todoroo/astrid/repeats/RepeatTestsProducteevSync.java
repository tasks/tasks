package com.todoroo.astrid.repeats;

import org.json.JSONArray;
import org.json.JSONObject;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.api.ApiUtilities;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevSyncProvider;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

public class RepeatTestsProducteevSync extends NewRepeatTests<JSONObject> {

    private static boolean initialized = false;

    protected static ProducteevInvoker invoker;

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired TagService tagService;
    protected ProducteevDataService producteevDataService;

    private static final String TEST_USER = "sync_tester2@astrid.com";
    private static final String TEST_PASSWORD = "wonkwonkjj";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_SYNC, null);
        if (!initialized) {
            initializeTestService();
        }
        producteevDataService = ProducteevDataService.getInstance();
        producteevDataService.updateDashboards(new JSONArray());
        clearAllRemoteTasks();
    }

    private void initializeTestService() throws Exception {
        //Set the username and password for the service
        Preferences.setString(R.string.producteev_PPr_email, TEST_USER);
        Preferences.setString(R.string.producteev_PPr_password, TEST_PASSWORD);

        invoker = ProducteevSyncProvider.getInvoker();

        invoker.authenticate(TEST_USER, TEST_PASSWORD);

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
                System.err.println(invoker.tasksDelete(getRemoteId(task)));
            }
        } catch (Exception e) {
            fail("Failed to clear remote tasks before tests");
        }
    }

    protected long getRemoteId(JSONObject remoteTask) {
        long remoteId = 0;
        try {
            remoteId = remoteTask.getLong("id_task");
        } catch (Exception e) {
            fail("Remote task object did not contain id_task field");
        }
        return remoteId;
    }

    @Override
    protected void waitAndSync() {
        AndroidUtilities.sleepDeep(3000L);
        new ProducteevSyncProvider().synchronize(null);
        AndroidUtilities.sleepDeep(3000L);
    }

    /**
     * @param t
     * @param expectedDueDate
     */
    @Override
    protected JSONObject assertTaskExistsRemotely(Task t, long expectedDueDate) {
        long remoteId = producteevDataService.getTaskMetadata(t.getId()).getValue(ProducteevTask.ID);
        JSONObject remoteTask = null;
        try {
            remoteTask = invoker.tasksView(remoteId).getJSONObject("task");
            assertNotNull(remoteTask);

            long remoteDueDate = ApiUtilities.producteevToUnixTime(remoteTask.getString("deadline"), 0);
            assertTimesMatch(expectedDueDate, remoteDueDate);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in producteev invoker");
        }
        return remoteTask;
    }

    @Override
    protected void assertTaskCompletedRemotely(Task t) {
        long remoteId = producteevDataService.getTaskMetadata(t.getId()).getValue(ProducteevTask.ID);
        JSONObject remoteTask = null;
        try {
            remoteTask = invoker.tasksView(remoteId).getJSONObject("task");
            assertNotNull(remoteTask);
            System.err.println(remoteTask);
            assertEquals(2, remoteTask.getInt("status"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in producteev invoker");
        }
    }

}
