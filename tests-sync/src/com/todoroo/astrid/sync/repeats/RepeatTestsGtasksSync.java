package com.todoroo.astrid.sync.repeats;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.services.tasks.model.Tasks;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.gtasks.sync.GtasksSyncProvider;
import com.todoroo.astrid.repeats.RepeatTaskCompleteListener;
import com.todoroo.astrid.service.MetadataService;

public class RepeatTestsGtasksSync extends AbstractSyncRepeatTests<com.google.api.services.tasks.model.Task> {

    @Autowired MetadataService metadataService;
    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    private static final String TEST_ACCOUNT = "sync_tester2@astrid.com";
    public static final String DEFAULT_LIST = "@default";

    private static boolean initialized = false;
    protected static GtasksInvoker gtasksService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Preferences.setStringFromInteger(R.string.p_default_urgency_key, 0);
        RepeatTaskCompleteListener.setSkipActFmCheck(true);

        if (!initialized) {
            initializeTestService();
        }

        setupTestList();
    }

    @Override
    protected void waitAndSync() {
        AndroidUtilities.sleepDeep(3000L);
        new GtasksSyncProvider().synchronize(null);
        AndroidUtilities.sleepDeep(3000L);
    }

    @Override
    protected com.google.api.services.tasks.model.Task assertTaskExistsRemotely(Task t, long expectedRemoteTime) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(t.getId());
        assertNotNull(metadata);
        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        String taskId = metadata.getValue(GtasksMetadata.ID);
        com.google.api.services.tasks.model.Task remote = null;
        try {
            remote = gtasksService.getGtask(listId, taskId);
        } catch (IOException e){
            e.printStackTrace();
            fail("Exception in gtasks service");
        }
        assertNotNull(remote);
        assertEquals(t.getValue(Task.TITLE), remote.getTitle());

        Date expected = new Date(expectedRemoteTime);
        expected.setHours(0);
        expected.setMinutes(0);
        expected.setSeconds(0);

        long gtasksTime = GtasksApiUtilities.gtasksDueTimeToUnixTime(remote.getDue(), 0);
        assertEquals(expected.getTime(), gtasksTime);
        return remote;
    }

    @Override
    protected void assertTaskCompletedRemotely(Task t) {
        Metadata metadata = gtasksMetadataService.getTaskMetadata(t.getId());
        assertNotNull(metadata);
        String listId = metadata.getValue(GtasksMetadata.LIST_ID);
        String taskId = metadata.getValue(GtasksMetadata.ID);
        com.google.api.services.tasks.model.Task remote = null;
        try {
            remote = gtasksService.getGtask(listId, taskId);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception in gtasks service");
        }

        assertNotNull(remote);
        assertEquals(t.getValue(Task.TITLE), remote.getTitle());

        assertEquals("completed", remote.getStatus());
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
        authToken = GtasksTokenValidator.validateAuthToken(getContext(), authToken);
        gtasksPreferenceService.setToken(authToken);

        gtasksService = new GtasksInvoker(authToken);

        initialized = true;
    }

    private void setupTestList() throws Exception {
        Tasks defaultListTasks = gtasksService.getAllGtasksFromListId(DEFAULT_LIST, false, false, 0);
        List<com.google.api.services.tasks.model.Task> items = defaultListTasks.getItems();
        if (items != null) {
            for (com.google.api.services.tasks.model.Task t : items) {
                gtasksService.deleteGtask(DEFAULT_LIST, t.getId());
            }
        }
    }
}
