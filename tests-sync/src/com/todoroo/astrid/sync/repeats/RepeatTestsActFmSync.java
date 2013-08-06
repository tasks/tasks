/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync.repeats;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import org.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmInvoker;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.actfm.sync.ActFmSyncV2Provider;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.sync.SyncResultCallbackAdapter;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class RepeatTestsActFmSync extends AbstractSyncRepeatTests<Task> {

    @Autowired
    MetadataService metadataService;
    @Autowired
    ActFmSyncService actFmSyncService;
    @Autowired
    ActFmPreferenceService actFmPreferenceService;
    protected static ActFmInvoker invoker = null;

    private static final String TEST_ACCOUNT = "sync_tester2@astrid.com";
    private static final String TEST_PASSWORD = "wonkwonkjj";
    private static boolean initialized = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);

        if (!initialized) {
            initializeTestService();
        }

        clearTasks();
    }

    private void initializeTestService() throws Exception {
        invoker = new ActFmInvoker();
        authenticate(TEST_ACCOUNT, null, null, ActFmInvoker.PROVIDER_PASSWORD, TEST_PASSWORD);
        initialized = true;
    }

    private void clearTasks() throws Exception {
    }

    private void authenticate(String email, String firstName, String lastName, String provider, String secret) {
        try {
            JSONObject result = invoker.authenticate(email, firstName, lastName, provider, secret);
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

        Preferences.setString(ActFmPreferenceService.PREF_USER_ID,
                Long.toString(result.optLong("id")));
        Preferences.setString(ActFmPreferenceService.PREF_NAME, result.optString("name"));
        Preferences.setString(ActFmPreferenceService.PREF_EMAIL, result.optString("email"));
        Preferences.setString(ActFmPreferenceService.PREF_PICTURE, result.optString("picture"));
    }


    @Override
    protected void waitAndSync() {
        AndroidUtilities.sleepDeep(3000L);
        final Semaphore sema = new Semaphore(0);
        new ActFmSyncV2Provider().synchronizeActiveTasks(true, new SyncResultCallbackAdapter() {
            @Override
            public void finished() {
                sema.release();
            }
        });
        try {
            sema.acquire();
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for sync to finish");
        }
        AndroidUtilities.sleepDeep(3000L);
    }

    /**
     * @param t
     * @param expectedDueDate
     */
    @Override
    protected Task assertTaskExistsRemotely(Task t, long expectedDueDate) {
        return null; //remote;
    }

    @Override
    protected void testRepeating(boolean completeBefore, boolean fromCompletion, RRule rrule, Frequency frequency, String title) {
        Task t = new Task();
        t.setValue(Task.TITLE, title);
        long dueDate = DateUtilities.now() + ((completeBefore ? -1 : 1) * DateUtilities.ONE_DAY * 3);
        dueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, (dueDate / 1000L) * 1000L); // Strip milliseconds

        t.setValue(Task.DUE_DATE, dueDate);

        if (rrule == null) {
            rrule = new RRule();
            rrule.setFreq(frequency);
            int interval = 5;
            rrule.setInterval(interval);
        }

        String result = rrule.toIcal();
        if (fromCompletion)
            result = result + ";FROM=COMPLETION";

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

            long fromDate = (fromCompletion ? completionDate : dueDate);
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
