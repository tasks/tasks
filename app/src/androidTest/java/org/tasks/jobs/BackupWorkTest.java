package org.tasks.jobs;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.tasks.date.DateTimeUtils.newDate;

import android.support.test.runner.AndroidJUnit4;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.time.DateTime;

@RunWith(AndroidJUnit4.class)
public class BackupWorkTest {
  private static File newFile(DateTime lastModified) {
    File result = mock(File.class);
    stub(result.lastModified()).toReturn(lastModified.getMillis());
    return result;
  }

  @Test
  public void filterExcludesXmlFiles() {
    assertFalse(BackupWork.FILE_FILTER.accept(new File("/a/b/c/d/auto.180329-0001.xml")));
  }

  @Test
  public void filterIncludesJsonFiles() {
    assertTrue(BackupWork.FILE_FILTER.accept(new File("/a/b/c/d/auto.180329-0001.json")));
  }

  @Test
  public void getDeleteKeepAllFiles() {
    File file1 = newFile(newDate(2018, 3, 27));
    File file2 = newFile(newDate(2018, 3, 28));
    File file3 = newFile(newDate(2018, 3, 29));

    assertEquals(emptyList(), BackupWork.getDeleteList(new File[] {file2, file1, file3}, 7));
  }

  @Test
  public void getDeleteFromNullFileList() {
    assertEquals(emptyList(), BackupWork.getDeleteList(null, 2));
  }

  @Test
  public void sortFiles() {
    File file1 = newFile(newDate(2018, 3, 27));
    File file2 = newFile(newDate(2018, 3, 28));
    File file3 = newFile(newDate(2018, 3, 29));

    assertEquals(
        singletonList(file1), BackupWork.getDeleteList(new File[] {file2, file1, file3}, 2));
  }
}
