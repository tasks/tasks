package com.todoroo.astrid.upgrade;

import java.util.Date;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.sql.Query;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.legacy.data.enums.Importance;
import com.todoroo.astrid.legacy.data.enums.RepeatInterval;
import com.todoroo.astrid.legacy.data.tag.TagController;
import com.todoroo.astrid.legacy.data.tag.TagIdentifier;
import com.todoroo.astrid.legacy.data.task.TaskController;
import com.todoroo.astrid.legacy.data.task.TaskModelForEdit;
import com.todoroo.astrid.legacy.data.task.AbstractTaskModel.RepeatInfo;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.tagsold.Tag;
import com.todoroo.astrid.tagsold.TagService;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid2To3UpgradeTests extends DatabaseTestCase {

    @Autowired
    TaskDao taskDao;

    public void upgrade2To3() {
        new UpgradeService().performUpgrade(130, 150);
    }

    public static void assertDatesEqual(Date old, int newDate) {
        if(old == null)
            assertEquals(0, newDate);
        assertEquals(old.getTime() / 1000L, newDate);
    }

    public void xtestEmptyUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();
        assertEquals(0, taskController.getAllTaskIdentifiers().size());

        // upgrade
        taskController.close();
        upgrade2To3();

        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(0, tasks.getCount());
    }

    public void testTaskTableUpgrade() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();

        // create some ish
        TaskModelForEdit griffey = new TaskModelForEdit();
        griffey.setName("ken griffey jr");
        griffey.setDefiniteDueDate(new Date(1234567L));
        griffey.setImportance(Importance.LEVEL_1);
        griffey.setEstimatedSeconds(3212);
        griffey.setNotes("debut game: 1989");
        taskController.saveTask(griffey, false);

        TaskModelForEdit guti = new com.todoroo.astrid.legacy.data.task.TaskModelForEdit();
        guti.setName("franklin gutierrez");
        guti.setPreferredDueDate(new Date(System.currentTimeMillis() + 5000000L));
        guti.setImportance(Importance.LEVEL_4);
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
        TodorooCursor<Task> tasks = taskDao.query(Query.select(Task.PROPERTIES));
        assertEquals(2, tasks.getCount());

        tasks.moveToFirst();
        Task task = new Task(tasks, Task.PROPERTIES);
        assertEquals(griffey.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(griffey.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertEquals((Integer)Task.IMPORTANCE_SHOULD_DO, task.getValue(Task.IMPORTANCE));
        assertEquals(griffey.getEstimatedSeconds(), task.getValue(Task.ESTIMATED_SECONDS));
        assertEquals(griffey.getNotes(), task.getValue(Task.NOTES));
        assertEquals((Integer)0, task.getValue(Task.LAST_NOTIFIED));
        assertEquals((Integer)0, task.getValue(Task.HIDDEN_UNTIL));

        tasks.moveToNext();
        task = new Task(tasks, Task.PROPERTIES);
        assertEquals(guti.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(guti.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertDatesEqual(guti.getPreferredDueDate(), task.getValue(Task.DUE_DATE));
        assertDatesEqual(guti.getHiddenUntil(), task.getValue(Task.HIDDEN_UNTIL));
        assertEquals((Integer)Task.IMPORTANCE_DO_OR_DIE, task.getValue(Task.IMPORTANCE));
        assertEquals(guti.getRepeat().getValue(), task.getRepeatInfo().getValue());
        assertEquals(guti.getRepeat().getInterval().ordinal(), task.getRepeatInfo().getInterval().ordinal());
        assertEquals(guti.getElapsedSeconds(), task.getValue(Task.ELAPSED_SECONDS));
        assertEquals(createdDate, task.getValue(Task.CREATION_DATE));
        assertEquals(createdDate, task.getValue(Task.MODIFICATION_DATE));
    }


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
        TagService tagService = new TagService();
        TodorooCursor<Tag> tags = tagService.getAllTags(Tag.NAME);
        assertEquals(2, tags.getCount());

    }


}


