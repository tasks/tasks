package org.tasks.data;

import static com.google.common.collect.Sets.newHashSet;
import static com.natpryce.makeiteasy.MakeItEasy.with;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.TagDataMaker.NAME;
import static org.tasks.makers.TagDataMaker.UID;
import static org.tasks.makers.TagDataMaker.newTagData;
import static org.tasks.makers.TagMaker.TAGDATA;
import static org.tasks.makers.TagMaker.TAGUID;
import static org.tasks.makers.TagMaker.TASK;
import static org.tasks.makers.TaskMaker.ID;
import static org.tasks.makers.TaskMaker.newTask;

import androidx.core.util.Pair;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.Set;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.makers.TagMaker;

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

    tagDao.insert(TagMaker.newTag(with(TAGDATA, tagOne), with(TASK, taskOne)));
    tagDao.insert(TagMaker.newTag(with(TAGDATA, tagTwo), with(TASK, taskTwo)));

    assertEquals(singletonList(tagOne), tagDataDao.getTagDataForTask(taskOne.getId()));
  }

  @Test
  public void getEmptyTagSelections() {
    Pair<Set<String>, Set<String>> selections = tagDataDao.getTagSelections(ImmutableList.of(1L));
    assertTrue(selections.first.isEmpty());
    assertTrue(selections.second.isEmpty());
  }

  @Test
  public void getPartialTagSelections() {
    newTag(1, "tag1", "tag2");
    newTag(2, "tag2", "tag3");


    assertEquals(
        newHashSet("tag1", "tag3"), tagDataDao.getTagSelections(ImmutableList.of(1L, 2L)).first);
  }

  @Test
  public void getEmptyPartialSelections() {
    newTag(1, "tag1");
    newTag(2, "tag1");

    assertTrue(tagDataDao.getTagSelections(ImmutableList.of(1L, 2L)).first.isEmpty());
  }

  @Test
  public void getCommonTagSelections() {
    newTag(1, "tag1", "tag2");
    newTag(2, "tag2", "tag3");

    assertEquals(
        newHashSet("tag2"), tagDataDao.getTagSelections(ImmutableList.of(1L, 2L)).second);
  }

  @Test
  public void getEmptyCommonSelections() {
    newTag(1, "tag1");
    newTag(2, "tag2");

    assertTrue(tagDataDao.getTagSelections(ImmutableList.of(1L, 2L)).second.isEmpty());
  }

  private void newTag(long taskId, String... tags) {
    Task task = newTask(with(ID, taskId));
    taskDao.createNew(task);
    for (String tag : tags) {
      tagDao.insert(TagMaker.newTag(with(TASK, task), with(TAGUID, tag)));
    }
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
