/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

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
        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(Metadata.ID));
        assertEquals(0, cursor.getCount());
        cursor.close();

        // create "happy"
        Metadata metadata = new Metadata();
        metadata.setTask(1L);
        metadata.setKey("happy");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(
                Query.select(Metadata.ID));
        assertEquals(1, cursor.getCount());
        cursor.close();
        long happyId = metadata.getId();
        assertNotSame(Metadata.NO_ID, happyId);
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getKey());

        // create "sad"
        metadata = new Metadata();
        metadata.setTask(1L);
        metadata.setKey("sad");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(Query.select(Metadata.ID));
        assertEquals(2, cursor.getCount());
        cursor.close();

        // rename sad to melancholy
        long sadId = metadata.getId();
        assertNotSame(Metadata.NO_ID, sadId);
        metadata.setKey("melancholy");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(
                Query.select(Metadata.ID));
        assertEquals(2, cursor.getCount());
        cursor.close();

        // check state
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getKey());
        metadata = metadataDao.fetch(sadId, KEYS);
        assertEquals("melancholy", metadata.getKey());

        // delete sad
        assertTrue(metadataDao.delete(sadId));
        cursor = metadataDao.query(
                Query.select(KEYS));
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        metadata = new Metadata(cursor);
        assertEquals("happy", metadata.getKey());
        cursor.close();
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


        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(KEYS).where(MetadataCriteria.byTask(1)));
        assertEquals(2, cursor.getCount());
        cursor.moveToFirst();
        metadata = new Metadata(cursor);
        assertEquals("with1", metadata.getKey());
        cursor.moveToNext();
        metadata = new Metadata(cursor);
        assertEquals("with1", metadata.getKey());
        cursor.close();

        cursor = metadataDao.query(
                Query.select(KEYS).where(MetadataCriteria.byTask(3)));
        assertEquals(0, cursor.getCount());
        cursor.close();

        int deleted = metadataDao.deleteWhere(MetadataCriteria.byTask(1));
        assertEquals(2, deleted);
        cursor = metadataDao.query(
                Query.select(KEYS));
        assertEquals(1, cursor.getCount());
        cursor.close();
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
