package com.todoroo.astrid.gtasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksDetailExposerTest extends DatabaseTestCase {

    @Autowired private GtasksListService gtasksListService;
    private GtasksTestPreferenceService preferences = new GtasksTestPreferenceService();
    private DetailListener detailListener = new DetailListener();

    private Task task;
    private String detail;

    public void testExposeNotLoggedIn() {
        givenTwoListSetup();
        givenLoggedInStatus(false);
        givenTaskWithList("listone-id");

        whenRequestingDetails();

        thenExpectNoDetail();
    }

    public void testExposeListOne() {
        givenTwoListSetup();
        givenLoggedInStatus(true);
        givenTaskWithList("listone-id");

        whenRequestingDetails();

        thenExpectDetail("List One");
    }

    public void testExposeListTwo() {
        givenTwoListSetup();
        givenLoggedInStatus(true);
        givenTaskWithList("listtwo-id");

        whenRequestingDetails();

        thenExpectDetail("List Two");
    }

    public void testExposeListDoesntExist() {
        givenTwoListSetup();
        givenLoggedInStatus(true);
        givenTaskWithList("blah");

        whenRequestingDetails();

        thenExpectNoDetail();
    }

    public void testExposeListNotSet() {
        givenTwoListSetup();
        givenLoggedInStatus(true);
        givenTaskWithList(null);

        whenRequestingDetails();

        thenExpectNoDetail();
    }

    // --- helpers

    private void thenExpectNoDetail() {
        assertNull("no detail", detail);
    }

    private void thenExpectDetail(String expected) {
        assertNotNull("detail not null", detail);
        assertTrue("detail was '" + detail + "', '" + expected + "' expected",
                detail.contains(expected));
    }

    private void givenTwoListSetup() {
        GoogleTaskListInfo[] newLists = new GoogleTaskListInfo[2];
        GoogleTaskListInfo list = new GoogleTaskListInfo("listone-id", "List One");
        newLists[0] = list;
        list = new GoogleTaskListInfo("listtwo-id", "List Two");
        newLists[1] = list;
        gtasksListService.updateLists(newLists);
    }

    private Task givenTaskWithList(String list) {
        Task newTask = new Task();
        PluginServices.getTaskService().save(newTask);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(newTask.getId());
        if(list != null)
            metadata.setValue(GtasksMetadata.LIST_ID, list);
        PluginServices.getMetadataService().save(metadata);
        return task = newTask;
    }

    @Override
    protected void addInjectables() {
        super.addInjectables();
        testInjector.addInjectable("gtasksPreferenceService", preferences);
    }

    private void whenRequestingDetails() {
        Intent intent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
        intent.putExtra(AstridApiConstants.EXTRAS_EXTENDED, false);
        intent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
        detail = null;
        new GtasksDetailExposer().onReceive(getContext(), intent);
        AndroidUtilities.sleepDeep(500);
    }

    private void givenLoggedInStatus(boolean status) {
        preferences.setLoggedIn(status);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getContext().registerReceiver(detailListener, new IntentFilter(AstridApiConstants.BROADCAST_SEND_DETAILS));

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getContext().unregisterReceiver(detailListener);
    }

    private class DetailListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            detail = intent.getExtras().getString(AstridApiConstants.EXTRAS_RESPONSE);
        }
    }

}
