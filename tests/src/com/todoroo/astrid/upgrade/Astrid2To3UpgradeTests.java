package com.todoroo.astrid.upgrade;

import java.util.Date;

import com.thoughtworks.sql.Query;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.legacy.data.enums.Importance;
import com.todoroo.astrid.legacy.data.enums.RepeatInterval;
import com.todoroo.astrid.legacy.data.task.TaskController;
import com.todoroo.astrid.legacy.data.task.TaskModelForEdit;
import com.todoroo.astrid.legacy.data.task.AbstractTaskModel.RepeatInfo;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.UpgradeService;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid2To3UpgradeTests extends DatabaseTestCase {

    @Autowired
    TaskDao taskDao;

    public void upgrade2To3() {
        new UpgradeService().performUpgrade(130, 150);
    }

    public static void assertDatesEqual(Date old, int newDate) {
        assertEquals(old.getTime() / 1000L, newDate);
    }

    public void testBasicUpgrades() {
        TaskController taskController = new TaskController(getContext());
        taskController.open();

        // create some ish
        TaskModelForEdit griffey = new TaskModelForEdit();
        griffey.setName("ken griffey jr");
        griffey.setDefiniteDueDate(new Date());
        griffey.setImportance(Importance.LEVEL_1);
        griffey.setEstimatedSeconds(3212);
        griffey.setNotes("debut game: 1989");
        taskController.saveTask(griffey, false);

        TaskModelForEdit guti = new com.todoroo.astrid.legacy.data.task.TaskModelForEdit();
        guti.setName("franklin gutierrez");
        guti.setPreferredDueDate(new Date(System.currentTimeMillis() + 5000000L));
        guti.setHiddenUntil(new Date());
        guti.setRepeat(new RepeatInfo(RepeatInterval.DAYS, 10));
        guti.setElapsedSeconds(500);
        taskController.saveTask(guti, false);

        // assert created
        assertEquals(2, taskController.getAllTaskIdentifiers());

        // upgrade
        taskController.close();
        upgrade2To3();

        // verify that it ain't no more in the legacy table
        taskController.open();
        assertEquals(0, taskController.getAllTaskIdentifiers());

        // verify that data exists in our new table
        TodorooCursor<Task> tasks = taskDao.query(database, Query.select(Task.PROPERTIES));
        tasks.moveToFirst();
        Task task = new Task(tasks, Task.PROPERTIES);
        assertEquals(griffey.getName(), task.getValue(Task.TITLE));
        assertDatesEqual(griffey.getDefiniteDueDate(), task.getValue(Task.DUE_DATE));
        assertEquals((Integer)Task.IMPORTANCE_SHOULD_DO, task.getValue(Task.IMPORTANCE));

    }



}
