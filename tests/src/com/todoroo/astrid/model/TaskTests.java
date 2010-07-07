package com.todoroo.astrid.model;

import java.util.ArrayList;
import java.util.TreeSet;

import android.content.ContentValues;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.test.DatabaseTestCase;
import com.todoroo.astrid.utility.Preferences;

public class TaskTests extends DatabaseTestCase {

    @Autowired
    TaskService taskService;

    /** Sanity-check the constants */
    public void testSanity() {
        assertTrue(Task.IMPORTANCE_DO_OR_DIE < Task.IMPORTANCE_MUST_DO);
        assertTrue(Task.IMPORTANCE_MUST_DO < Task.IMPORTANCE_SHOULD_DO);
        assertTrue(Task.IMPORTANCE_SHOULD_DO < Task.IMPORTANCE_NONE);

        ArrayList<Integer> reminderFlags = new ArrayList<Integer>();
        reminderFlags.add(Task.NOTIFY_AFTER_DEADLINE);
        reminderFlags.add(Task.NOTIFY_AT_DEADLINE);
        reminderFlags.add(Task.NOTIFY_NONSTOP);

        // assert no duplicates
        assertEquals(new TreeSet<Integer>(reminderFlags).size(),
                reminderFlags.size());
    }

    /** Check defaults */
    public void checkDefaults() {
        Preferences.setPreferenceDefaults();
        ContentValues defaults = new Task().getDefaultValues();
        assertTrue(defaults.containsKey(Task.TITLE.name));
        assertTrue(defaults.containsKey(Task.DUE_DATE.name));
        assertTrue(defaults.containsKey(Task.HIDE_UNTIL.name));
        assertTrue(defaults.containsKey(Task.COMPLETION_DATE.name));
        assertTrue(defaults.containsKey(Task.IMPORTANCE.name));
    }

    /** Check task gets a creation date at some point */
    public void checkCreationDate() {
        Task task = new Task();
        taskService.save(task, false);
        assertTrue(task.getValue(Task.CREATION_DATE) > 0);
    }

    /**
     * Check various getters
     */
    public void checkGetters() {
        Task task = new Task();
        assertFalse(task.isCompleted());
        task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        assertTrue(task.isCompleted());

        task = new Task();
        assertFalse(task.isHidden());
        task.setValue(Task.HIDE_UNTIL, DateUtilities.now() + 1000);
        assertTrue(task.isHidden());

        task = new Task();
        assertFalse(task.hasDueDate());
        task.setValue(Task.DUE_DATE, DateUtilities.now() + 1000);
        assertTrue(task.hasDueDate());
    }

}
