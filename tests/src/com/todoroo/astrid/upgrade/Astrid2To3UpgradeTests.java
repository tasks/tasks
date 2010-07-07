package com.todoroo.astrid.upgrade;

import java.util.Date;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.sql.Query;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.alarms.Alarm;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.legacy.data.alerts.AlertController;
import com.todoroo.astrid.legacy.data.enums.Importance;
import com.todoroo.astrid.legacy.data.enums.RepeatInterval;
import com.todoroo.astrid.legacy.data.tag.TagController;
import com.todoroo.astrid.legacy.data.tag.TagIdentifier;
import com.todoroo.astrid.legacy.data.task.TaskController;
import com.todoroo.astrid.legacy.data.task.TaskIdentifier;
import com.todoroo.astrid.legacy.data.task.TaskModelForEdit;
import com.todoroo.astrid.legacy.data.task.AbstractTaskModel.RepeatInfo;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.Astrid2To3UpgradeHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid2To3UpgradeTests extends DatabaseTestCase {

    @Autowired
    TaskDao taskDao;

    public void upgrade2To3() {
        new Astrid2To3UpgradeHelper().upgrade2To3();
    }

    public static void assertDatesEqual(Date old, int newDate) {
        if(old == null)
            assertEquals(0, newDate);
        else
            assertEquals(old.getTime() / 1000L, newDate);
    }

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
    public void testTaskTableUpgrade() {
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
        taskController.saveTask(guti, false);
        Date createdDate = new Date();

        // assert created
        assertEquals(2, taskController.getAllTaskIdentifiers().size());

        // upgrade
        upgrade2To3();

        // verify that data exists in our new table
        database.openForReading();
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(2, tasks.getCount());

        tasks.moveToFirst();
        Task task = new Task(tasks);
        assertEquals(griffey.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(griffey.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertEquals((Integer)Task.IMPORTANCE_NONE, task.getValue(Task.IMPORTANCE));
        assertEquals(griffey.getEstimatedSeconds(), task.getValue(Task.ESTIMATED_SECONDS));
        assertEquals(griffey.getNotes(), task.getValue(Task.NOTES));
        assertEquals((Integer)0, task.getValue(Task.LAST_NOTIFIED));
        assertEquals((Integer)0, task.getValue(Task.HIDDEN_UNTIL));

        tasks.moveToNext();
        task = new Task(tasks);
        assertEquals(guti.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(guti.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertDatesEqual(guti.getPreferredDueDate(), task.getValue(Task.PREFERRED_DUE_DATE));
        assertDatesEqual(guti.getHiddenUntil(), task.getValue(Task.HIDDEN_UNTIL));
        assertEquals((Integer)Task.IMPORTANCE_DO_OR_DIE, task.getValue(Task.IMPORTANCE));
        assertEquals(guti.getRepeat().getValue(), task.getRepeatInfo().getValue());
        assertEquals(guti.getRepeat().getInterval().ordinal(), task.getRepeatInfo().getInterval().ordinal());
        assertEquals(guti.getElapsedSeconds(), task.getValue(Task.ELAPSED_SECONDS));
        assertDatesEqual(createdDate, task.getValue(Task.CREATION_DATE));
        assertDatesEqual(createdDate, task.getValue(Task.MODIFICATION_DATE));
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
        TagService tagService = new TagService(getContext());
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
        TagService tagService = new TagService(getContext());
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

        // upgradeia32-sun-java6-bin
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

}


