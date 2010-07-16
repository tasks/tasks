package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class MetadataDaoTests extends DatabaseTestCase {

    @Autowired
    MetadataDao metadataDao;

    @Autowired
    TaskDao taskDao;

    public static Property<?>[] KEYS = new Property<?>[] { Metadata.ID,
            Metadata.KEY };

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
        metadata.setValue(Metadata.KEY, "happy");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(
                Query.select(Metadata.ID));
        assertEquals(1, cursor.getCount());
        cursor.close();
        long happyId = metadata.getId();
        assertNotSame(Metadata.NO_ID, happyId);
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getValue(Metadata.KEY));

        // create "sad"
        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "sad");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(Query.select(Metadata.ID));
        assertEquals(2, cursor.getCount());
        cursor.close();

        // rename sad to melancholy
        long sadId = metadata.getId();
        assertNotSame(Metadata.NO_ID, sadId);
        metadata.setValue(Metadata.KEY, "melancholy");
        assertTrue(metadataDao.persist(metadata));
        cursor = metadataDao.query(
                Query.select(Metadata.ID));
        assertEquals(2, cursor.getCount());
        cursor.close();

        // check state
        metadata = metadataDao.fetch(happyId, KEYS);
        assertEquals("happy", metadata.getValue(Metadata.KEY));
        metadata = metadataDao.fetch(sadId, KEYS);
        assertEquals("melancholy", metadata.getValue(Metadata.KEY));

        // delete sad
        assertTrue(metadataDao.delete(sadId));
        cursor = metadataDao.query(
                Query.select(KEYS));
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        metadata.readFromCursor(cursor);
        assertEquals("happy", metadata.getValue(Metadata.KEY));
        cursor.close();
    }

    /**
     * Test metadata bound to task
     */
    public void testMetadataConditions() throws Exception {
        // create "happy"
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with1");
        metadata.setValue(Metadata.TASK, 1L);
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with2");
        metadata.setValue(Metadata.TASK, 2L);
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with1");
        metadata.setValue(Metadata.TASK, 1L);
        assertTrue(metadataDao.persist(metadata));


        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(KEYS).where(MetadataCriteria.byTask(1)));
        assertEquals(2, cursor.getCount());
        cursor.moveToFirst();
        metadata.readFromCursor(cursor);
        assertEquals("with1", metadata.getValue(Metadata.KEY));
        cursor.moveToNext();
        metadata.readFromCursor(cursor);
        assertEquals("with1", metadata.getValue(Metadata.KEY));
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

    /**
     * Test metadata deletion
     */
    public void testFetchDangling() throws Exception {
        // fetch with nothing in db
        TodorooCursor<Metadata> cursor = metadataDao.fetchDangling(KEYS);
        assertEquals(0, cursor.getCount());
        cursor.close();

        Task task1 = new Task();
        taskDao.persist(task1);
        Task task2 = new Task();
        taskDao.persist(task2);
        Task task3 = new Task();
        taskDao.persist(task3);

        // fetch with only tasks
        cursor = metadataDao.fetchDangling(KEYS);
        assertEquals(0, cursor.getCount());
        cursor.close();

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with1");
        metadata.setValue(Metadata.TASK, task1.getId());
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with2");
        metadata.setValue(Metadata.TASK, task2.getId());
        assertTrue(metadataDao.persist(metadata));

        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with3");
        metadata.setValue(Metadata.TASK, task3.getId());
        assertTrue(metadataDao.persist(metadata));

        // fetch with tasks and corresponding metadata
        cursor = metadataDao.fetchDangling(KEYS);
        assertEquals(0, cursor.getCount());
        cursor.close();

        long task2Id = task2.getId();
        taskDao.delete(task2.getId());

        // note: we should not have any dangling, since deleting a task
        // will automatically delete metadata
        cursor = metadataDao.fetchDangling(KEYS);
        assertEquals(0, cursor.getCount());
        cursor.close();

        metadata = new Metadata();
        metadata.setValue(Metadata.KEY, "with2");
        metadata.setValue(Metadata.TASK, task2Id);
        assertTrue(metadataDao.persist(metadata));

        // but if we simulate something bad happening by creating
        // it manually.. well, what can i say, it should be broken
        cursor = metadataDao.fetchDangling(KEYS);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        metadata.readFromCursor(cursor);
        assertEquals("with2", metadata.getValue(Metadata.KEY));
        cursor.close();
    }

}