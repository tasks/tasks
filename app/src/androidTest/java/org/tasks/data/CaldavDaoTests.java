package org.tasks.data;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.CaldavTaskMaker.CALENDAR;
import static org.tasks.makers.CaldavTaskMaker.REMOTE_ID;
import static org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT;
import static org.tasks.makers.CaldavTaskMaker.newCaldavTask;
import static org.tasks.makers.TagDataMaker.newTagData;
import static org.tasks.makers.TagMaker.TAGDATA;
import static org.tasks.makers.TagMaker.TASK;
import static org.tasks.makers.TagMaker.newTag;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import com.google.common.primitives.Longs;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.makers.CaldavTaskMaker;

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
  @SdkSuppress(minSdkVersion = 21)
  public void findChildrenInList() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L)));
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(CaldavTaskMaker.PARENT, 1L),
                with(REMOTE_PARENT, "a"))));

    assertEquals(singletonList(2L), caldavDao.findChildrenInList(Longs.asList(1, 2)));
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void findRecursiveChildrenInList() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L)));
    taskDao.createNew(newTask(with(ID, 3L)));
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(CaldavTaskMaker.PARENT, 1L),
                with(REMOTE_ID, "b"),
                with(REMOTE_PARENT, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 3L),
                with(CALENDAR, "1"),
                with(CaldavTaskMaker.PARENT, 2L),
                with(REMOTE_PARENT, "b"))));

    assertEquals(asList(2L, 3L), caldavDao.findChildrenInList(Longs.asList(1, 2, 3)));
  }

  @Test
  @SdkSuppress(minSdkVersion = 21)
  public void findRecursiveChildrenInListAfterSkippingParent() {
    taskDao.createNew(newTask(with(ID, 1L)));
    taskDao.createNew(newTask(with(ID, 2L)));
    taskDao.createNew(newTask(with(ID, 3L)));
    caldavDao.insert(
        asList(
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 1L), with(CALENDAR, "1"), with(REMOTE_ID, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 2L),
                with(CALENDAR, "1"),
                with(CaldavTaskMaker.PARENT, 1L),
                with(REMOTE_ID, "b"),
                with(REMOTE_PARENT, "a")),
            newCaldavTask(
                with(CaldavTaskMaker.TASK, 3L),
                with(CALENDAR, "1"),
                with(CaldavTaskMaker.PARENT, 2L),
                with(REMOTE_PARENT, "b"))));

    assertEquals(singletonList(3L), caldavDao.findChildrenInList(Longs.asList(1, 3)));
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
