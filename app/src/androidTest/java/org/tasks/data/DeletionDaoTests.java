package org.tasks.data;

import static com.google.common.collect.Lists.newArrayList;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.tasks.makers.TaskMaker.CREATION_TIME;
import static org.tasks.makers.TaskMaker.newTask;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class DeletionDaoTests extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject DeletionDao deletionDao;

  @Test
  public void deleting1000DoesntCrash() {
    deletionDao.delete(
        new ArrayList<>(ContiguousSet.create(Range.closed(1L, 1000L), DiscreteDomain.longs())));
  }

  @Test
  public void marking998ForDeletionDoesntCrash() {
    deletionDao.markDeleted(
        new ArrayList<>(ContiguousSet.create(Range.closed(1L, 1000L), DiscreteDomain.longs())));
  }

  @Test
  public void markDeletedUpdatesModificationTime() {
    Task task = newTask(with(CREATION_TIME, new DateTime().minusMinutes(1)));
    taskDao.createNew(task);
    deletionDao.markDeleted(singletonList(task.getId()));
    task = taskDao.fetch(task.getId());
    assertTrue(task.getModificationDate() > task.getCreationDate());
    assertTrue(task.getModificationDate() < currentTimeMillis());
  }

  @Test
  public void markDeletedUpdatesDeletionTime() {
    Task task = newTask(with(CREATION_TIME, new DateTime().minusMinutes(1)));
    taskDao.createNew(task);
    deletionDao.markDeleted(singletonList(task.getId()));
    task = taskDao.fetch(task.getId());
    assertTrue(task.getDeletionDate() > task.getCreationDate());
    assertTrue(task.getDeletionDate() < currentTimeMillis());
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
