package org.tasks.data;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.tasks.makers.TagDataMaker.NAME;
import static org.tasks.makers.TagDataMaker.newTagData;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class TagDataDaoTest extends InjectingTestCase {

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

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
