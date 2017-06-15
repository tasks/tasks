package org.tasks.gtasks;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.astrid.data.Task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DUE;
import static com.todoroo.astrid.data.Task.HIDE_UNTIL_DUE_TIME;
import static junit.framework.Assert.assertEquals;
import static org.tasks.gtasks.GoogleTaskSyncAdapter.mergeDates;
import static org.tasks.makers.TaskMaker.DUE_DATE;
import static org.tasks.makers.TaskMaker.DUE_TIME;
import static org.tasks.makers.TaskMaker.HIDE_TYPE;
import static org.tasks.makers.TaskMaker.newTask;

@RunWith(AndroidJUnit4.class)
public class GoogleTaskSyncAdapterTest {

    @Test
    public void testMergeDate() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 11)));
        Task local = newTask(with(DUE_DATE, new DateTime(2016, 3, 12)));

        mergeDates(remote, local);

        assertEquals(new DateTime(2016, 3, 11, 12, 0).getMillis(), remote.getDueDate().longValue());
    }

    @Test
    public void testMergeTime() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 11)));
        Task local = newTask(with(DUE_TIME, new DateTime(2016, 3, 11, 13, 30)));

        mergeDates(remote, local);

        assertEquals(
                new DateTime(2016, 3, 11, 13, 30, 1).getMillis(),
                remote.getDueDate().longValue());
    }

    @Test
    public void testDueDateAdjustHideBackwards() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 11)));
        Task local = newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

        mergeDates(remote, local);

        assertEquals(new DateTime(2016, 3, 11).getMillis(), remote.getHideUntil().longValue());
    }

    @Test
    public void testDueDateAdjustHideForwards() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 14)));
        Task local = newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

        mergeDates(remote, local);

        assertEquals(new DateTime(2016, 3, 14).getMillis(), remote.getHideUntil().longValue());
    }

    @Test
    public void testDueTimeAdjustHideBackwards() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 11)));
        Task local = newTask(with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

        mergeDates(remote, local);

        assertEquals(new DateTime(2016, 3, 11, 13, 30, 1).getMillis(), remote.getHideUntil().longValue());
    }

    @Test
    public void testDueTimeAdjustTimeForwards() {
        Task remote = newTask(with(DUE_DATE, new DateTime(2016, 3, 14)));
        Task local = newTask(with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

        mergeDates(remote, local);

        assertEquals(new DateTime(2016, 3, 14, 13, 30, 1).getMillis(), remote.getHideUntil().longValue());
    }

    @Test
    public void testDueDateClearHide() {
        Task remote = newTask();
        Task local = newTask(with(DUE_DATE, new DateTime(2016, 3, 12)), with(HIDE_TYPE, HIDE_UNTIL_DUE));

        mergeDates(remote, local);

        assertEquals(0, remote.getHideUntil().longValue());
    }

    @Test
    public void testDueTimeClearHide() {
        Task remote = newTask();
        Task local = newTask(with(DUE_TIME, new DateTime(2016, 3, 12, 13, 30)),
                with(HIDE_TYPE, HIDE_UNTIL_DUE_TIME));

        mergeDates(remote, local);

        assertEquals(0, remote.getHideUntil().longValue());
    }
}
