package com.todoroo.astrid.sync.repeats;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmDataService;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncProvider;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.service.MetadataService;

public class RepeatTestsActFmSync extends AbstractSyncRepeatTests<Task> {

    @Autowired MetadataService metadataService;
    @Autowired ActFmDataService actFmDataService;
    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;
    protected static ActFmInvoker invoker = null;

    private static final String TEST_ACCOUNT = "sync_tester2@astrid.com";
    private static final String TEST_PASSWORD = "wonkwonkjj";
    private static boolean initialized = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
        RepeatTaskCompleteListener.setSkipActFmCheck(false);

        if (!initialized) {
            initializeTestService();
        }

        clearTasks();
    }

    private void initializeTestService() throws Exception {
        invoker = new ActFmInvoker();
        authenticate(TEST_ACCOUNT, null, ActFmInvoker.PROVIDER_PASSWORD, TEST_PASSWORD);
        initialized = true;
    }

    private void clearTasks() throws Exception {
        JSONObject result = invoker.invoke("task_list", "active", 1);

        JSONArray taskList = result.getJSONArray("list");
        for(int i = 0; i < taskList.length(); i++) {
            Task remote = new Task();
            ActFmSyncService.JsonHelper.taskFromJson(taskList.getJSONObject(i), remote, new ArrayList<Metadata>());

            remote.setValue(Task.DELETION_DATE, DateUtilities.now());
            actFmSyncService.pushTaskOnSave(remote, remote.getSetValues());
        }
    }

    private void authenticate(String email, String name, String provider, String secret) {
        try {
            JSONObject result = invoker.authenticate(email, name, provider, secret);
            String token = invoker.getToken();
            postAuthenticate(result, token);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Error authenticating");
        }
    }

    @SuppressWarnings("nls")
    private void postAuthenticate(JSONObject result, String token) {
        actFmPreferenceService.setToken(token);

        Preferences.setLong(ActFmPreferenceService.PREF_USER_ID,
                result.optLong("id"));
        Preferences.setString(ActFmPreferenceService.PREF_NAME, result.optString("name"));
        Preferences.setString(ActFmPreferenceService.PREF_EMAIL, result.optString("email"));
        Preferences.setString(ActFmPreferenceService.PREF_PICTURE, result.optString("picture"));
    }


    @Override
    protected void waitAndSync() {
        AndroidUtilities.sleepDeep(3000L);
        new ActFmSyncProvider().synchronize(null);
        AndroidUtilities.sleepDeep(3000L);
    }

    /**
     * @param t
     * @param expectedDueDate
     */
    @Override
    protected Task assertTaskExistsRemotely(Task t, long expectedDueDate) {
        Task remote = new Task();
        try {
            ActFmSyncService.JsonHelper.taskFromJson(invoker.invoke("task_show", "id", t.getValue(Task.REMOTE_ID)), remote,
                    new ArrayList<Metadata>());
            assertTimesMatch(expectedDueDate, remote.getValue(Task.DUE_DATE).longValue());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in ActFm invoker");
        }
        return remote;
    }

    @Override
    protected void testRepeating(boolean completeBefore, boolean fromCompletion, RRule rrule, Frequency frequency, String title) {
        Task t = new Task();
        t.setValue(Task.TITLE, title);
        long dueDate = DateUtilities.now() + DateUtilities.ONE_DAY * 3;
        dueDate = (dueDate / 1000L) * 1000L; // Strip milliseconds
        if (fromCompletion)
            t.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, true);

        t.setValue(Task.DUE_DATE, dueDate);

        if (rrule == null) {
            rrule = new RRule();
            rrule.setFreq(frequency);
            int interval = 5;
            rrule.setInterval(interval);
        }
        t.setValue(Task.RECURRENCE, rrule.toIcal());
        taskDao.save(t);

        waitAndSync();
        t = taskDao.fetch(t.getId(), Task.PROPERTIES); // Refetch
        Task remoteModel = assertTaskExistsRemotely(t, dueDate);

        long completionDate = setCompletionDate(completeBefore, t, remoteModel, dueDate);

        waitAndSync();

        TodorooCursor<Task> cursor = taskDao.query(Query.select(Task.PROPERTIES).where(TaskCriteria.notDeleted()));
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            t.readFromCursor(cursor);

            long fromDate = (fromCompletion? completionDate : dueDate);
            long expectedTime = computeNextDueDateFromDate(fromDate, rrule, fromCompletion);
            long newDueDate = t.getValue(Task.DUE_DATE);

            assertTaskExistsRemotely(t, expectedTime);
            assertTrue(t.hasDueTime());
            assertEquals(title, t.getValue(Task.TITLE));
            assertTimesMatch(expectedTime, newDueDate);
            assertFalse(t.isCompleted());

        } finally {
            cursor.close();
        }
    }
}
