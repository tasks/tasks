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

import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;

@SuppressWarnings("nls")
public class GtasksMetadataServiceTest extends DatabaseTestCase {

    @Module
    public class GtasksMetadataServiceTestModule {
        private final GtasksTestPreferenceService service;

        public GtasksMetadataServiceTestModule(Context context) {
            service = new GtasksTestPreferenceService(context, new Preferences(context, null), null);
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

    @Subcomponent(modules = GtasksMetadataServiceTest.GtasksMetadataServiceTestModule.class)
    public interface GtasksMetadataServiceTestComponent {
        void inject(GtasksMetadataServiceTest gtasksMetadataServiceTest);
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

    @Override
    protected void inject(TestComponent component) {
        component
                .plus(new GtasksMetadataServiceTestModule(getContext()))
                .inject(this);
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
}
