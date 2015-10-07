package com.todoroo.astrid.model;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property;

import org.tasks.injection.InjectingTestCase;

import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

import org.tasks.Snippet;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static org.tasks.Freeze.freezeClock;
import static org.tasks.RemoteModelHelpers.asQueryProperties;
import static org.tasks.RemoteModelHelpers.compareRemoteModel;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class TaskTest extends InjectingTestCase {

    @Inject TaskService taskService;
    @Inject Preferences preferences;

    public void testNewTaskHasNoCreationDate() {
        assertFalse(new Task().containsValue(Task.CREATION_DATE));
    }

    public void testSavedTaskHasCreationDate() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = new Task();
            taskService.save(task);
            assertEquals(currentTimeMillis(), (long) task.getCreationDate());
        }});
    }

    public void testReadTaskFromDb() {
        Task task = new Task();
        taskService.save(task);
        Property[] properties = asQueryProperties(Task.TABLE, task.getDatabaseValues());
        final Task fromDb = taskService.fetchById(task.getId(), properties);
        compareRemoteModel(task, fromDb);
    }

    public void testDefaults() {
        preferences.setDefaults();
        ContentValues defaults = new Task().getDefaultValues();
        assertTrue(defaults.containsKey(Task.TITLE.name));
        assertTrue(defaults.containsKey(Task.DUE_DATE.name));
        assertTrue(defaults.containsKey(Task.HIDE_UNTIL.name));
        assertTrue(defaults.containsKey(Task.COMPLETION_DATE.name));
        assertTrue(defaults.containsKey(Task.IMPORTANCE.name));
    }
}
