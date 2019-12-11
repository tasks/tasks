package org.tasks.db;

import static org.junit.Assert.assertEquals;
import static org.tasks.db.QueryUtils.showCompleted;
import static org.tasks.db.QueryUtils.showHidden;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.astrid.data.Task;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QueryUtilsTest {
  @Test
  public void replaceHiddenLT() {
    assertEquals(
        "(1)",
        showHidden(Task.HIDE_UNTIL.lt(Functions.now()).toString()));
  }

  @Test
  public void replaceHiddenLTE() {
    assertEquals(
        "(1)",
        showHidden(Task.HIDE_UNTIL.lte(Functions.now()).toString()));
  }

  @Test
  public void replaceUncompletedEQ() {
    assertEquals(
        "(1)",
        showCompleted(Task.COMPLETION_DATE.eq(0).toString()));
  }

  @Test
  public void replaceUncompletedLTE() {
    assertEquals(
        "(1)",
        showCompleted(Task.COMPLETION_DATE.lte(0).toString()));
  }
}
