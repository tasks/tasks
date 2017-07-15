package com.todoroo.astrid.model;

import android.content.ContentValues;
import android.support.test.runner.AndroidJUnit4;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Snippet;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.tasks.Freeze.freezeClock;
import static org.tasks.RemoteModelHelpers.asQueryProperties;
import static org.tasks.RemoteModelHelpers.compareRemoteModel;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class TaskTest extends InjectingTestCase {

    @Inject TaskDao taskDao;
    @Inject Preferences preferences;

    @Test
    public void testNewTaskHasNoCreationDate() {
        assertFalse(new Task().containsValue(Task.CREATION_DATE));
    }

    @Test
    public void testSavedTaskHasCreationDate() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = new Task();
            taskDao.save(task);
            assertEquals(currentTimeMillis(), (long) task.getCreationDate());
        }});
    }

    @Test
    public void testReadTaskFromDb() {
        Task task = new Task();
        taskDao.save(task);
        Property[] properties = asQueryProperties(Task.TABLE, task.getDatabaseValues());
        final Task fromDb = taskDao.fetch(task.getId(), properties);
        compareRemoteModel(task, fromDb);
    }

    @Test
    public void testDefaults() {
        preferences.setDefaults();
        ContentValues defaults = new Task().getDefaultValues();
        assertTrue(defaults.containsKey(Task.TITLE.name));
        assertTrue(defaults.containsKey(Task.DUE_DATE.name));
        assertTrue(defaults.containsKey(Task.HIDE_UNTIL.name));
        assertTrue(defaults.containsKey(Task.COMPLETION_DATE.name));
        assertTrue(defaults.containsKey(Task.IMPORTANCE.name));
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }
}
