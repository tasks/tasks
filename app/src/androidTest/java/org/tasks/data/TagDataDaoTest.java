package org.tasks.data;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.TagDataMaker.NAME;
import static org.tasks.makers.TagDataMaker.newTagData;
import static org.tasks.makers.TagMaker.TAGDATA;
import static org.tasks.makers.TagMaker.TASK;
import static org.tasks.makers.TagMaker.newTag;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class TagDataDaoTest extends InjectingTestCase {

  @Inject TaskDao taskDao;
  @Inject TagDao tagDao;
  @Inject TagDataDao tagDataDao;

  @Test
  public void tagDataOrderedByNameIgnoresNullNames() {
    tagDataDao.createNew(newTagData(with(NAME, (String) null)));

    assertTrue(tagDataDao.tagDataOrderedByName().isEmpty());
  }

  @Test
  public void tagDataOrderedByNameIgnoresEmptyNames() {
    tagDataDao.createNew(newTagData(with(NAME, "")));

    assertTrue(tagDataDao.tagDataOrderedByName().isEmpty());
  }

  @Test
  public void getTagWithCaseForMissingTag() {
    assertEquals("derp", tagDataDao.getTagWithCase("derp"));
  }

  @Test
  public void getTagWithCaseFixesCase() {
    tagDataDao.createNew(newTagData(with(NAME, "Derp")));

    assertEquals("Derp", tagDataDao.getTagWithCase("derp"));
  }

  @Test
  public void getTagsByName() {
    TagData tagData = newTagData(with(NAME, "Derp"));
    tagDataDao.createNew(tagData);

    assertEquals(singletonList(tagData), tagDataDao.getTags(singletonList("Derp")));
  }

  @Test
  public void getTagsByNameCaseSensitive() {
    tagDataDao.createNew(newTagData(with(NAME, "Derp")));

    assertTrue(tagDataDao.getTags(singletonList("derp")).isEmpty());
  }

  @Test
  public void getTagDataForTask() {
    Task taskOne = newTask();
    Task taskTwo = newTask();
    taskDao.createNew(taskOne);
    taskDao.createNew(taskTwo);

    TagData tagOne = newTagData(with(NAME, "one"));
    TagData tagTwo = newTagData(with(NAME, "two"));
    tagDataDao.createNew(tagOne);
    tagDataDao.createNew(tagTwo);

    tagDao.insert(newTag(with(TAGDATA, tagOne), with(TASK, taskOne)));
    tagDao.insert(newTag(with(TAGDATA, tagTwo), with(TASK, taskTwo)));

    assertEquals(singletonList(tagOne), tagDataDao.getTagDataForTask(taskOne.getId()));
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
