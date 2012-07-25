/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

@SuppressWarnings("nls")
public class GtasksMetadataServiceTest extends DatabaseTestCase {

    private final GtasksTestPreferenceService preferences = new GtasksTestPreferenceService();
    @Autowired private GtasksMetadataService gtasksMetadataService;

    private Task task;
    private Metadata metadata;
    private TodorooCursor<Task> cursor;

    public void testMetadataFound() {
        givenTask(taskWithMetadata(null));

        whenSearchForMetadata();

        thenExpectMetadataFound();
    }

    public void testMetadataDoesntExist() {
        givenTask(taskWithoutMetadata());

        whenSearchForMetadata();

        thenExpectNoMetadataFound();
    }

    public void testLocallyCreatedHasItem() {
        taskWithMetadata("ok");
        givenTask(taskWithoutMetadata());

        whenReadLocalCreated();

        thenExpectCursorEquals(task);
    }

    public void testLocallyCreatedWhenEmptyMetadata() {
        givenTask(taskWithMetadata(null));

        whenReadLocalCreated();

        thenExpectCursorEquals(task);
    }

    public void testLocallyCreatedIsEmpty() {
        givenTask(taskWithMetadata("ok"));

        whenReadLocalCreated();

        thenExpectCursorIsEmpty();
    }

    public void testLocallyUpdatedHasItem() {
        givenSyncDate(DateUtilities.now() - 5000L);
        givenTask(taskWithMetadata("ok"));

        whenReadLocalUpdated();

        thenExpectCursorEquals(task);
    }

    public void testLocallyUpdatedIsEmptyWhenUpToDate() {
        givenTask(taskWithMetadata("ok"));
        givenSyncDate(DateUtilities.now());

        whenReadLocalUpdated();

        thenExpectCursorIsEmpty();
    }

    public void testLocallyUpdatedIsEmptyWhenNoUpdatedTasks() {
        givenTask(taskWithMetadata(null));

        whenReadLocalUpdated();

        thenExpectCursorIsEmpty();
    }

    // --- helpers

    private void givenSyncDate(long date) {
        preferences.setSyncDate(date);
    }

    private void whenReadLocalUpdated() {
        cursor = gtasksMetadataService.getLocallyUpdated(Task.ID);
    }

    private void thenExpectCursorIsEmpty() {
        assertEquals("cursor is empty", 0, cursor.getCount());
    }

    private void thenExpectCursorEquals(Task expectedTask) {
        assertEquals("cursor has one item", 1, cursor.getCount());
        cursor.moveToFirst();
        Task receivedTask = new Task(cursor);
        assertEquals("task equals expected", expectedTask.getId(), receivedTask.getId());
    }

    private void whenReadLocalCreated() {
        cursor = gtasksMetadataService.getLocallyCreated(Task.ID);
    }

    private void thenExpectNoMetadataFound() {
        assertNull(metadata);
    }

    private void thenExpectMetadataFound() {
        assertNotNull(metadata);
    }

    private void whenSearchForMetadata() {
        metadata = gtasksMetadataService.getTaskMetadata(task.getId());
    }

    private Task taskWithMetadata(String id) {
        Task task = new Task();
        task.setValue(Task.TITLE, "cats");
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        if(id != null)
            metadata.setValue(GtasksMetadata.ID, id);
        metadata.setValue(Metadata.TASK, task.getId());
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

    private void givenTask(Task taskToTest) {
        task = taskToTest;
    }

    private Task taskWithoutMetadata() {
        Task task = new Task();
        task.setValue(Task.TITLE, "dogs");
        PluginServices.getTaskService().save(task);
        return task;
    }

    @Override
    protected void addInjectables() {
        super.addInjectables();
        testInjector.addInjectable("gtasksPreferenceService", preferences);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        if(!Preferences.isSet(GtasksPreferenceService.PREF_DEFAULT_LIST))
            Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, "list");
    }

}
