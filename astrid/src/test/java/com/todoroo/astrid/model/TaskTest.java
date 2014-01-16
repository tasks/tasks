package com.todoroo.astrid.model;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.test.TodorooRobolectricTestCase;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.AstridPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.tasks.Snippet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tasks.Freeze.freezeClock;
import static org.tasks.RemoteModelHelpers.asQueryProperties;
import static org.tasks.RemoteModelHelpers.compareRemoteModel;
import static org.tasks.date.DateTimeUtils.currentTimeMillis;

@RunWith(RobolectricTestRunner.class)
public class TaskTest extends TodorooRobolectricTestCase {

    private TaskService taskService;

    @Override
    public void before() throws Exception {
        super.before();

        taskService = new TaskService();
    }

    @Test
    public void newTaskHasNoCreationDate() {
        assertFalse(new Task().containsValue(Task.CREATION_DATE));
    }

    @Test
    public void savedTaskHasCreationDate() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = new Task();
            taskService.save(task);
            assertEquals(currentTimeMillis(), (long) task.getValue(Task.CREATION_DATE));
        }});
    }

    @Test
    public void readTaskFromDb() {
        Task task = new Task();
        taskService.save(task);
        Property[] properties = asQueryProperties(Task.TABLE, task.getDatabaseValues());
        final Task fromDb = taskService.fetchById(task.getId(), properties);
        compareRemoteModel(task, fromDb);
    }

    @Test
    public void testDefaults() {
        AstridPreferences.setPreferenceDefaults();
        ContentValues defaults = new Task().getDefaultValues();
        assertTrue(defaults.containsKey(Task.TITLE.name));
        assertTrue(defaults.containsKey(Task.DUE_DATE.name));
        assertTrue(defaults.containsKey(Task.HIDE_UNTIL.name));
        assertTrue(defaults.containsKey(Task.COMPLETION_DATE.name));
        assertTrue(defaults.containsKey(Task.IMPORTANCE.name));
    }
}
