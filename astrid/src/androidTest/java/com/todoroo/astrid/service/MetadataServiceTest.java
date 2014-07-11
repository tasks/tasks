package com.todoroo.astrid.service;

import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.InjectingTestCase;

import javax.inject.Inject;

public class MetadataServiceTest extends InjectingTestCase {

    @Inject MetadataService metadataService;
    @Inject MetadataDao metadataDao;
    @Inject TaskDao taskDao;

    Metadata metadata;

    @Override
    public void setUp() {
        super.setUp();
        metadata = new Metadata();
    }

    public void testDontSaveMetadataWithoutTaskId() {
        try {
            metadataService.save(metadata);
            fail("expected exception");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("metadata needs to be attached to a task"));
        }
    }

    public void testSaveMetadata() {
        metadata.setTask(1L);
        metadataService.save(metadata);

        assertNotNull(metadataDao.fetch(metadata.getId()));
    }

    public void testDontDeleteValidMetadata() {
        final Task task = new Task();
        taskDao.save(task);
        metadata.setTask(task.getId());
        metadataService.save(metadata);

        metadataService.removeDanglingMetadata();

        assertNotNull(metadataDao.fetch(metadata.getId()));
    }

    public void testDeleteDangling() {
        metadata.setTask(1L);
        metadataService.save(metadata);

        metadataService.removeDanglingMetadata();

        assertNull(metadataDao.fetch(1));
    }
}
