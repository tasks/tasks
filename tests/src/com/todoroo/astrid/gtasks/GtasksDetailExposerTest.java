package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Preferences;
import com.todoroo.gtasks.GoogleTaskListInfo;

public class GtasksDetailExposerTest extends DatabaseTestCase {

    @Autowired private GtasksListService gtasksListService;
    private GtasksTestPreferenceService preferences = new GtasksTestPreferenceService();

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
        detail = new GtasksDetailExposer().getTaskDetails(task.getId(), false);
    }

    private void givenLoggedInStatus(boolean status) {
        preferences.setLoggedIn(status);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if(!Preferences.isSet(GtasksPreferenceService.PREF_DEFAULT_LIST))
            Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, "list");

    }

}
