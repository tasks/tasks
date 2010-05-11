package com.todoroo.astrid.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import android.content.ContentValues;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.test.utility.DateUtilities;
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

        ArrayList<Integer> urgencies = new ArrayList<Integer>();
        urgencies.add(Task.URGENCY_NONE);
        urgencies.add(Task.URGENCY_SPECIFIC_DAY);
        urgencies.add(Task.URGENCY_SPECIFIC_DAY_TIME);
        urgencies.add(Task.URGENCY_THIS_MONTH);
        urgencies.add(Task.URGENCY_THIS_WEEK);
        urgencies.add(Task.URGENCY_TODAY);
        urgencies.add(Task.URGENCY_WITHIN_A_YEAR);
        urgencies.add(Task.URGENCY_WITHIN_SIX_MONTHS);
        urgencies.add(Task.URGENCY_WITHIN_THREE_MONTHS);

        // assert no duplicates
        assertEquals(new TreeSet<Integer>(urgencies).size(),
                urgencies.size());
    }

    /** Check defaults */
    public void checkDefaults() {
        Preferences.setPreferenceDefaults();
        ContentValues defaults = new Task().getDefaultValues();
        assertTrue(defaults.containsKey(Task.TITLE.name));
        assertTrue(defaults.containsKey(Task.DUE_DATE.name));
        assertTrue(defaults.containsKey(Task.HIDDEN_UNTIL.name));
        assertTrue(defaults.containsKey(Task.COMPLETION_DATE.name));
        assertTrue(defaults.containsKey(Task.URGENCY.name));
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
        task.setValue(Task.HIDDEN_UNTIL, DateUtilities.now() + 1000);
        assertTrue(task.isHidden());

        task = new Task();
        assertFalse(task.hasDueDate());
        task.setValue(Task.DUE_DATE, DateUtilities.now() + 1000);
        assertTrue(task.hasDueDate());

        int[] colors = Task.getImportanceColors(getContext());
        assertEquals(Math.abs(Task.IMPORTANCE_NONE - Task.IMPORTANCE_DO_OR_DIE + 1),
                colors.length);
        HashSet<Integer> set = new HashSet<Integer>();
        for(int i = 0; i < colors.length; i++) {
            assertFalse(set.contains(colors[i]));
            set.add(colors[i]);
        }
    }

    public void checkDueDateInitialization() {
        assertEquals(0, Task.initializeDueDate(Task.URGENCY_NONE));

        int date = Task.initializeDueDate(Task.URGENCY_THIS_MONTH);
        assertTrue(date > DateUtilities.now() + 27 * 24 * 3600);
        assertTrue(date < DateUtilities.now() + 32 * 24 * 3600);

        date = Task.initializeDueDate(Task.URGENCY_THIS_WEEK);
        assertTrue(date > DateUtilities.now() + 6 * 24 * 3600);
        assertTrue(date < DateUtilities.now() + 8 * 24 * 3600);

        date = Task.initializeDueDate(Task.URGENCY_TODAY);
        assertTrue(date > DateUtilities.now() - 60);
        assertTrue(date < DateUtilities.now() + 24 * 3600);

        date = Task.initializeDueDate(Task.URGENCY_WITHIN_THREE_MONTHS);
        assertTrue(date > DateUtilities.now() + 85 * 24 * 3600);
        assertTrue(date < DateUtilities.now() + 95 * 24 * 3600);

        date = Task.initializeDueDate(Task.URGENCY_WITHIN_SIX_MONTHS);
        assertTrue(date > DateUtilities.now() + 180 * 24 * 3600);
        assertTrue(date < DateUtilities.now() + 185 * 24 * 3600);

        date = Task.initializeDueDate(Task.URGENCY_WITHIN_A_YEAR);
        assertTrue(date > DateUtilities.now() + 364 * 24 * 3600);
        assertTrue(date < DateUtilities.now() + 367 * 24 * 3600);
    }
}
