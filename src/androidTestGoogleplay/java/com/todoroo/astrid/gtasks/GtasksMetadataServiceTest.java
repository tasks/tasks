/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;

import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.injection.TestModule;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

@SuppressWarnings("nls")
public class GtasksMetadataServiceTest extends DatabaseTestCase {

    @Module(addsTo = TestModule.class, injects = {GtasksMetadataServiceTest.class})
    static class GtasksMetadataServiceTestModule {
        private final GtasksTestPreferenceService service;

        public GtasksMetadataServiceTestModule(Context context) {
            service = new GtasksTestPreferenceService(context, new Preferences(context, null, null), null);
        }

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
    @Inject MetadataDao metadataDao;
    @Inject TaskService taskService;
    @Inject GtasksMetadata gtasksMetadata;

    private Task task;
    private Metadata metadata;

    @Override
    public void setUp() {
        super.setUp();

        if (preferences.getDefaultList() == null) {
            preferences.setDefaultList("list");
        }
    }

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

    // --- helpers

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
        Metadata metadata = gtasksMetadata.createEmptyMetadata(task.getId());
        if (id != null)
            metadata.setValue(GtasksMetadata.ID, id);
        metadata.setTask(task.getId());
        metadataDao.persist(metadata);
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
    protected Object getModule() {
        return new GtasksMetadataServiceTestModule(getContext());
    }
}
