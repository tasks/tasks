/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.injection.TestModule;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

@SuppressWarnings("nls")
public class GtasksMetadataServiceTest extends DatabaseTestCase {

    @Module(addsTo = TestModule.class, injects = {GtasksMetadataServiceTest.class})
    static class GtasksMetadataServiceTestModule {
        private final GtasksTestPreferenceService service = new GtasksTestPreferenceService();

        @Provides
        public GtasksTestPreferenceService getGtasksTestPreferenceService() {
            return service;
        }

        @Provides
        public GtasksPreferenceService getGtasksPreferenceService() {
            return service;
        }
    }

    @Inject GtasksTestPreferenceService preferences;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject MetadataService metadataService;
    @Inject TaskService taskService;

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

    public void disabled_testLocallyCreatedHasItem() {
        taskWithMetadata("ok");
        givenTask(taskWithoutMetadata());

        whenReadLocalCreated();

        thenExpectCursorEquals(task);
    }

    public void disabled_testLocallyCreatedWhenEmptyMetadata() {
        givenTask(taskWithMetadata(null));

        whenReadLocalCreated();

        thenExpectCursorEquals(task);
    }

    public void disabled_testLocallyCreatedIsEmpty() {
        givenTask(taskWithMetadata("ok"));

        whenReadLocalCreated();

        thenExpectCursorIsEmpty();
    }

    public void disabled_testLocallyUpdatedHasItem() {
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
        task.setTitle("cats");
        taskService.save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        if (id != null)
            metadata.setValue(GtasksMetadata.ID, id);
        metadata.setTask(task.getId());
        metadataService.save(metadata);
        return task;
    }

    private void givenTask(Task taskToTest) {
        task = taskToTest;
    }

    private Task taskWithoutMetadata() {
        Task task = new Task();
        task.setTitle("dogs");
        taskService.save(task);
        return task;
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        if (!Preferences.isSet(GtasksPreferenceService.PREF_DEFAULT_LIST))
            Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, "list");
    }

    @Override
    protected Object getModule() {
        return new GtasksMetadataServiceTestModule();
    }
}
