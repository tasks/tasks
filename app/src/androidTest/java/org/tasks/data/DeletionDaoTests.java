package org.tasks.data;

import static com.google.common.collect.Lists.newArrayList;

import android.support.test.runner.AndroidJUnit4;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;

@RunWith(AndroidJUnit4.class)
public class DeletionDaoTests extends InjectingTestCase {

  @Inject DeletionDao deletionDao;

  @Test
  public void deleting1000DoesntCrash() {
    deletionDao.delete(
        newArrayList(ContiguousSet.create(Range.closed(1L, 1000L), DiscreteDomain.longs())));
  }

  @Test
  public void marking998ForDeletionDoesntCrash() {
    deletionDao.markDeleted(
        newArrayList(ContiguousSet.create(Range.closed(1L, 1000L), DiscreteDomain.longs())));
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }
}
