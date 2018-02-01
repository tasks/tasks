package com.todoroo.astrid.model;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.Snippet;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.tasks.Freeze.freezeClock;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class TaskTest extends InjectingTestCase {

    @Inject TaskDao taskDao;

    @Test
    public void testSavedTaskHasCreationDate() {
        freezeClock().thawAfter(new Snippet() {{
            Task task = new Task();
            taskDao.createNew(task);
            assertEquals(currentTimeMillis(), (long) task.getCreationDate());
        }});
    }

    @Test
    public void testReadTaskFromDb() {
        Task task = new Task();
        taskDao.createNew(task);
        final Task fromDb = taskDao.fetch(task.getId());
        assertEquals(task, fromDb);
    }

    @Override
    protected void inject(TestComponent component) {
        component.inject(this);
    }
}
