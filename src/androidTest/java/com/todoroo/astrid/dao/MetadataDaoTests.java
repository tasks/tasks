/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import java.util.List;

import javax.inject.Inject;

public class MetadataDaoTests extends DatabaseTestCase {

    @Inject MetadataDao metadataDao;
    @Inject TaskDao taskDao;

    private Metadata metadata;

    public static Property<?>[] KEYS = new Property<?>[] { Metadata.ID, Metadata.KEY };

    @Override
    public void setUp() {
        super.setUp();
        metadata = new Metadata();
    }

    /**
     * Test basic creation, fetch, and save
     */
    public void testCrud() throws Exception {
        assertTrue(metadataDao.toList(Query.select(Metadata.ID)).isEmpty());

        // create "happy"
        Metadata metadata = new Metadata();
        metadata.setTask(1L);
        metadata.setKey("happy");
        assertTrue(metadataDao.persist(metadata));
        assertEquals(1, metadataDao.toList(Query.select(Metadata.ID)).size());
        long happyId = metadata.getId();
        assertNotSame(Metadata.NO_ID, happyId);
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getKey());

        // create "sad"
        metadata = new Metadata();
        metadata.setTask(1L);
        metadata.setKey("sad");
        assertTrue(metadataDao.persist(metadata));
        assertEquals(2, metadataDao.toList(Query.select(Metadata.ID)).size());

        // rename sad to melancholy
        long sadId = metadata.getId();
        assertNotSame(Metadata.NO_ID, sadId);
        metadata.setKey("melancholy");
        assertTrue(metadataDao.persist(metadata));
        assertEquals(2, metadataDao.toList(Query.select(Metadata.ID)).size());

        // check state
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getKey());
        metadata = metadataDao.fetch(sadId, KEYS);
        assertEquals("melancholy", metadata.getKey());

        // delete sad
        assertTrue(metadataDao.delete(sadId));
        List<Metadata> metadataList = metadataDao.toList(Query.select(KEYS));
        assertEquals(1, metadataList.size());
        assertEquals("happy", metadataList.get(0).getKey());
    }

    /**
     * Test metadata bound to task
     */
    public void disabled_testMetadataConditions() {
        // create "happy"
        Metadata metadata = new Metadata();
        metadata.setKey("with1");
        metadata.setTask(1L);
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setKey("with2");
        metadata.setTask(2L);
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setKey("with1");
        metadata.setTask(1L);
        assertTrue(metadataDao.persist(metadata));

        List<Metadata> metadataList = metadataDao.toList(Query.select(KEYS).where(MetadataCriteria.byTask(1)));
        assertEquals(2, metadataList.size());
        assertEquals("with1", metadataList.get(0).getKey());
        assertEquals("with1", metadataList.get(1).getKey());

        assertTrue(metadataDao.toList(Query.select(KEYS).where(MetadataCriteria.byTask(3))).isEmpty());

        assertEquals(2, metadataDao.deleteWhere(MetadataCriteria.byTask(1)));
        assertEquals(1, metadataDao.toList(Query.select(KEYS)).size());
    }

    public void testDontSaveMetadataWithoutTaskId() {
        try {
            metadataDao.persist(metadata);
            fail("expected exception");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("metadata needs to be attached to a task"));
        }
    }

    public void testSaveMetadata() {
        metadata.setTask(1L);
        metadataDao.persist(metadata);

        assertNotNull(metadataDao.fetch(metadata.getId()));
    }

    public void testDontDeleteValidMetadata() {
        final Task task = new Task();
        taskDao.save(task);
        metadata.setTask(task.getId());
        metadataDao.persist(metadata);

        metadataDao.removeDanglingMetadata();

        assertNotNull(metadataDao.fetch(metadata.getId()));
    }

    public void testDeleteDangling() {
        metadata.setTask(1L);
        metadataDao.persist(metadata);

        metadataDao.removeDanglingMetadata();

        assertNull(metadataDao.fetch(1));
    }
}
