package com.todoroo.astrid.gtasks;

import android.content.Context;
import android.content.Intent;

import com.google.api.services.tasks.model.TaskList;
import com.todoroo.andlib.test.TodorooRobolectricTestCaseWithInjector;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(RobolectricTestRunner.class)
public class GtasksDetailExposerTest extends TodorooRobolectricTestCaseWithInjector {

    private Context context;
    private RobolectricGtasksPreferenceService gtasksPreferenceService;

    @Override
    protected void addInjectables() {
        testInjector.addInjectable("gtasksPreferenceService", gtasksPreferenceService);
    }

    @Override
    public void setUp() throws Exception {
        gtasksPreferenceService = new RobolectricGtasksPreferenceService();
        context = mock(Context.class);
        new GtasksListService().addNewList(
                new TaskList().setId("list_id").setTitle("list_title"));

        super.setUp();
    }

    @Override
    public void after() throws Exception {
        verify(context).getApplicationContext();
        verifyNoMoreInteractions(context);

        super.after();
    }

    @Test
    public void dontBroadcastDetailsWhenNotLoggedIn() {
        gtasksPreferenceService.logout();

        getDetails(newTask("list_id"));
    }

    @Test
    public void dontBroadcastDetailsForInvalidTaskId() {
        getDetails(0);
    }

    @Test
    public void dontBroadcastDetailsWithNoList() {
        getDetails(newTask());
    }

    @Test
    public void dontBroadcastDetailsWithNullList() {
        getDetails(newTask(null));
    }

    @Test
    public void dontBroadcastDetailsWithDefaultList() {
        getDetails(newTask(GtasksPreferenceService.PREF_DEFAULT_LIST));
    }

    @Test
    public void dontBroadcastDetailsForNonexistentList() {
        getDetails(newTask("invalid_list_id"));
    }

    @Test
    public void broadcastDetails() {
        Task task = newTask("list_id");

        getDetails(task);

        verify(context).sendBroadcast(
                new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS)
                        .putExtra(AstridApiConstants.EXTRAS_ADDON, GtasksPreferenceService.IDENTIFIER)
                        .putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId())
                        .putExtra(AstridApiConstants.EXTRAS_RESPONSE, "<img src='gtasks_detail'/> list_title"),
                AstridApiConstants.PERMISSION_READ);
    }

    private void getDetails(Task task) {
        getDetails(task.getId());
    }

    private void getDetails(long taskId) {
        Intent intent = new Intent().putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        new GtasksDetailExposer().onReceive(context, intent);
    }

    private Task newTask() {
        Task task = new Task();
        new TaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        new MetadataService().save(metadata);
        return task;
    }

    private Task newTask(String list) {
        Task task = new Task();
        new TaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.LIST_ID, list);
        new MetadataService().save(metadata);
        return task;
    }
}
