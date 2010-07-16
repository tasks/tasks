package com.todoroo.astrid.upgrade;

import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.TestDependencyInjector;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.alarms.Alarm;
import com.todoroo.astrid.alarms.AlarmDatabase;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.legacy.data.alerts.AlertController;
import com.todoroo.astrid.legacy.data.enums.Importance;
import com.todoroo.astrid.legacy.data.enums.RepeatInterval;
import com.todoroo.astrid.legacy.data.sync.SyncDataController;
import com.todoroo.astrid.legacy.data.sync.SyncMapping;
import com.todoroo.astrid.legacy.data.tag.TagController;
import com.todoroo.astrid.legacy.data.tag.TagIdentifier;
import com.todoroo.astrid.legacy.data.task.AbstractTaskModel.RepeatInfo;
import com.todoroo.astrid.legacy.data.task.TaskController;
import com.todoroo.astrid.legacy.data.task.TaskIdentifier;
import com.todoroo.astrid.legacy.data.task.TaskModelForEdit;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.data.MilkTask;
import com.todoroo.astrid.service.Astrid2To3UpgradeHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid2To3UpgradeTests extends DatabaseTestCase {

    // --- legacy table names

    private static final String SYNC_TEST = "synctest";
    private static final String ALERTS_TEST = "alertstest";
    private static final String TAG_TASK_TEST = "tagtasktest";
    private static final String TAGS_TEST = "tagstest";
    private static final String TASKS_TEST = "taskstest";

    // --- setup and teardown

    private AlarmDatabase alarmsDatabase;

    @Autowired
    TaskDao taskDao;

    @Autowired
    MetadataDao metadataDao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // initialize test dependency injector
        TestDependencyInjector injector = TestDependencyInjector.initialize("upgrade");
        injector.addInjectable("tasksTable", TASKS_TEST);
        injector.addInjectable("tagsTable", TAGS_TEST);
        injector.addInjectable("tagTaskTable", TAG_TASK_TEST);
        injector.addInjectable("alertsTable", ALERTS_TEST);
        injector.addInjectable("syncTable", SYNC_TEST);
        injector.addInjectable("database", database);

        deleteDatabase(TASKS_TEST);
        deleteDatabase(TAGS_TEST);
        deleteDatabase(TAG_TASK_TEST);
        deleteDatabase(ALERTS_TEST);
        deleteDatabase(SYNC_TEST);

        database.clear();

        alarmsDatabase = new AlarmDatabase();
        alarmsDatabase.clear();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        TestDependencyInjector.deinitialize("upgrade");
    }

    // --- helper methods

    public void upgrade2To3() {
        new Astrid2To3UpgradeHelper().upgrade2To3();
    }

    public static void assertDatesEqual(Date old, long newDate) {
        if(old == null)
            assertEquals(0, newDate);
        else
            assertEquals(old.getTime(), newDate);
    }

    // --- tests

    /**
     * Test upgrade doesn't crash and burn when there is nothing
     */
    public void testEmptyUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        assertEquals(0, taskController.getAllTaskIdentifiers().size());

        // upgrade
        taskController.close();
        upgrade2To3();

        database.openForReading();
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(0, tasks.getCount());
    }

    /**
     * Test various parameters of tasks table
     */
    public void testTaskTableUpgrade() throws Exception {
        TaskController taskController = new TaskController(getContext());
        taskController.open();

        // create some ish
        TaskModelForEdit griffey = new TaskModelForEdit();
        griffey.setName("ken griffey jr");
        griffey.setDefiniteDueDate(new Date(1234567L));
        griffey.setImportance(Importance.LEVEL_4);
        griffey.setEstimatedSeconds(3212);
        griffey.setNotes("debut game: 1989");
        taskController.saveTask(griffey, false);

        TaskModelForEdit guti = new com.todoroo.astrid.legacy.data.task.TaskModelForEdit();
        guti.setName("franklin gutierrez");
        guti.setPreferredDueDate(new Date(System.currentTimeMillis() + 5000000L));
        guti.setImportance(Importance.LEVEL_1);
        guti.setHiddenUntil(new Date());
        guti.setRepeat(new RepeatInfo(RepeatInterval.DAYS, 10));
        guti.setElapsedSeconds(500);
        guti.setNotificationIntervalSeconds(200);
        taskController.saveTask(guti, false);

        // assert created
        assertEquals(2, taskController.getAllTaskIdentifiers().size());

        // upgrade
        Date upgradeTime = new Date();
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(2, tasks.getCount());

        tasks.moveToFirst();
        Task task = new Task(tasks);
        assertEquals(griffey.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(griffey.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertEquals(Task.IMPORTANCE_NONE, (int)task.getValue(Task.IMPORTANCE));
        assertEquals(griffey.getEstimatedSeconds(), task.getValue(Task.ESTIMATED_SECONDS));
        assertEquals(griffey.getNotes(), task.getValue(Task.NOTES));
        assertEquals("", task.getValue(Task.RECURRENCE));
        assertEquals(0, (long)task.getValue(Task.REMINDER_LAST));
        assertEquals(0, (long)task.getValue(Task.HIDE_UNTIL));

        tasks.moveToNext();
        task = new Task(tasks);
        assertEquals(guti.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(guti.getPreferredDueDate(), task.getValue(Task.DUE_DATE));
        assertDatesEqual(guti.getHiddenUntil(), task.getValue(Task.HIDE_UNTIL));
        assertEquals((Integer)Task.IMPORTANCE_DO_OR_DIE, task.getValue(Task.IMPORTANCE));
        assertEquals(guti.getRepeat().getValue(), new RRule(task.getValue(Task.RECURRENCE)).getInterval());
        assertEquals(Frequency.DAILY,
                new RRule(task.getValue(Task.RECURRENCE)).getFreq());
        assertEquals(guti.getElapsedSeconds(), task.getValue(Task.ELAPSED_SECONDS));
        assertEquals(guti.getNotificationIntervalSeconds() * 1000L, (long)task.getValue(Task.REMINDER_PERIOD));
        assertTrue(task.getValue(Task.CREATION_DATE) > 0);
        assertTrue(task.getValue(Task.MODIFICATION_DATE) > 0);
        assertTrue(task.getValue(Task.CREATION_DATE) <= upgradeTime.getTime());
        assertTrue(task.getValue(Task.MODIFICATION_DATE) <= upgradeTime.getTime());
    }

    /**
     * Test basic upgrading of tags
     */
    public void testTagTableUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        TagController tagController = new TagController(getContext());
        tagController.open();

        // create some ish
        TagIdentifier tasty = tagController.createTag("tasty");
        TagIdentifier salty = tagController.createTag("salty");

        TaskModelForEdit peanut = new TaskModelForEdit();
        TaskModelForEdit icecream = new TaskModelForEdit();
        TaskModelForEdit pickle = new TaskModelForEdit();
        taskController.saveTask(peanut, false);
        taskController.saveTask(icecream, false);
        taskController.saveTask(pickle, false);
        tagController.addTag(peanut.getTaskIdentifier(), tasty);
        tagController.addTag(peanut.getTaskIdentifier(), salty);
        tagController.addTag(icecream.getTaskIdentifier(), tasty);

        // assert created
        assertEquals(2, tagController.getAllTags().size());

        // upgrade
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();
        TagService tagService = TagService.getInstance();
        Tag[] tags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA);
        assertEquals(2, tags.length);
        assertEquals("salty", tags[0].tag);
        assertEquals("tasty", tags[1].tag);

        // verify that tags are applied correctly
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(3, tasks.getCount());
        tasks.moveToFirst();
        Task task = new Task(tasks);
        assertEquals("tasty, salty", tagService.getTagsAsString(task.getId()));
        tasks.moveToNext();
        task.readFromCursor(tasks);
        assertEquals("tasty", tagService.getTagsAsString(task.getId()));
        tasks.moveToNext();
        task.readFromCursor(tasks);
        assertEquals("", tagService.getTagsAsString(task.getId()));
    }

    /**
     * Test basic upgrading when tags point to deleted tasks
     */
    public void testDanglingTagsUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        TagController tagController = new TagController(getContext());
        tagController.open();

        // create some ish
        TagIdentifier tag1 = tagController.createTag("dangling");
        TagIdentifier tag2 = tagController.createTag("hanging");
        TagIdentifier tag3 = tagController.createTag("attached");

        TaskModelForEdit cliff = new TaskModelForEdit();
        cliff.setName("cliff");
        taskController.saveTask(cliff, false);
        TaskModelForEdit water = new TaskModelForEdit();
        water.setName("water");
        taskController.saveTask(water, false);

        // fake task identifiers
        tagController.addTag(new TaskIdentifier(10), tag1);
        tagController.addTag(new TaskIdentifier(15), tag2);
        tagController.addTag(cliff.getTaskIdentifier(), tag3);

        // assert created
        assertEquals(3, tagController.getAllTags().size());

        // upgrade
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();
        TagService tagService = TagService.getInstance();
        Tag[] tags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA);
        assertEquals(1, tags.length);
        assertEquals("attached", tags[0].tag);

        // verify that tags are applied correctly
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(2, tasks.getCount());
        tasks.moveToFirst();
        Task task = new Task(tasks);
        assertEquals("attached", tagService.getTagsAsString(task.getId()));
        tasks.moveToNext();
        task.readFromCursor(tasks);
        assertEquals("", tagService.getTagsAsString(task.getId()));
    }

    /**
     * Test basic upgrading of alerts table
     */
    public void testAlertTableUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        AlertController alertController = new AlertController(getContext());
        alertController.open();

        // create some ish
        TaskModelForEdit christmas = new TaskModelForEdit();
        taskController.saveTask(christmas, false);
        Date x1 = new Date(90,11,25);
        Date x2 = new Date(91,11,25);
        alertController.addAlert(christmas.getTaskIdentifier(), x1);
        alertController.addAlert(christmas.getTaskIdentifier(), x2);

        // assert created
        assertEquals(2, alertController.getTaskAlerts(christmas.getTaskIdentifier()).size());

        // upgrade
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();

        alarmsDatabase.openForReading();
        TodorooCursor<Alarm> cursor = alarmsDatabase.getDao().query(Query.select(Alarm.TIME));
        assertEquals(2, cursor.getCount());
        cursor.moveToFirst();
        Alarm alarm = new Alarm(cursor);
        assertDatesEqual(x1, alarm.getValue(Alarm.TIME));
        cursor.moveToNext();
        alarm.readFromCursor(cursor);
        assertDatesEqual(x2, alarm.getValue(Alarm.TIME));
    }

    /**
     * Test basic upgrading of the sync mapping table
     */
    public void testSyncTableUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        SyncDataController syncController = new SyncDataController(getContext());
        syncController.open();

        // create some ish
        TaskModelForEdit christmas = new TaskModelForEdit();
        christmas.setName("christmas");
        taskController.saveTask(christmas, false);
        String remoteId = "123|456|789000";
        SyncMapping mapping = new SyncMapping(christmas.getTaskIdentifier(), 1, remoteId);
        syncController.saveSyncMapping(mapping);

        christmas = new TaskModelForEdit();
        christmas.setName("july");
        taskController.saveTask(christmas, false);
        syncController.addToUpdatedList(christmas.getTaskIdentifier());

        // assert created
        assertEquals(1, syncController.getSyncMappings(1).size());

        // upgrade
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();

        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.withKey(MilkTask.METADATA_KEY)));
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        Metadata metadata = new Metadata(cursor);
        assertEquals(123, (long)metadata.getValue(MilkTask.TASK_ID));
        assertEquals(456, (long)metadata.getValue(MilkTask.TASK_SERIES_ID));
        assertEquals(789000, (long)metadata.getValue(MilkTask.LIST_ID));
        cursor.close();
    }


}


