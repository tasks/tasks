package org.tasks.data;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.TagDataMaker.newTagData;
import static org.tasks.makers.TagMaker.TAGDATA;
import static org.tasks.makers.TagMaker.TASK;
import static org.tasks.makers.TagMaker.newTag;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class CaldavDaoTests extends InjectingTestCase {
  @Inject TaskDao taskDao;
  @Inject TagDao tagDao;
  @Inject TagDataDao tagDataDao;
  @Inject CaldavDao caldavDao;

  @Test
  public void getCaldavTasksWithTags() {
    Task task = newTask(with(ID, 1L));
    taskDao.createNew(task);
    TagData one = newTagData();
    TagData two = newTagData();
    tagDataDao.createNew(one);
    tagDataDao.createNew(two);
    tagDao.insert(newTag(with(TASK, task), with(TAGDATA, one)));
    tagDao.insert(newTag(with(TASK, task), with(TAGDATA, two)));
    caldavDao.insert(new CaldavTask(task.getId(), "calendar"));

    assertEquals(singletonList(task.getId()), caldavDao.getTasksWithTags());
  }

  @Test
  public void ignoreNonCaldavTaskWithTags() {
    Task task = newTask(with(ID, 1L));
    taskDao.createNew(task);
    TagData tag = newTagData();
    tagDataDao.createNew(tag);
    tagDao.insert(newTag(with(TASK, task), with(TAGDATA, tag)));

    assertTrue(caldavDao.getTasksWithTags().isEmpty());
  }

  @Test
  public void ignoreCaldavTaskWithoutTags() {
    Task task = newTask(with(ID, 1L));
    taskDao.createNew(task);
    tagDataDao.createNew(newTagData());
    caldavDao.insert(new CaldavTask(task.getId(), "calendar"));

    assertTrue(caldavDao.getTasksWithTags().isEmpty());
  }

  @Test
  public void noResultsForEmptyAccounts() {
    CaldavAccount caldavAccount = new CaldavAccount();
    caldavAccount.setUuid(UUIDHelper.newUUID());
    caldavDao.insert(caldavAccount);

    assertTrue(caldavDao.getCaldavFilters(caldavAccount.getUuid(), now()).isEmpty());
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
