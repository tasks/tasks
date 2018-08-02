/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.jobs;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import android.support.test.runner.AndroidJUnit4;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;
import org.tasks.backup.TasksJsonExporter;
import org.tasks.injection.InjectingTestCase;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;

@RunWith(AndroidJUnit4.class)
public class BackupServiceTests extends InjectingTestCase {

  private static final long BACKUP_WAIT_TIME = 500L;
  @Inject TasksJsonExporter jsonExporter;
  @Inject TaskDao taskDao;
  @Inject Preferences preferences;
  private File temporaryDirectory = null;

  @Override
  public void setUp() {
    super.setUp();

    try {
      temporaryDirectory = File.createTempFile("backup", Long.toString(System.nanoTime()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (!(temporaryDirectory.delete())) {
      throw new RuntimeException(
          "Could not delete temp file: " + temporaryDirectory.getAbsolutePath());
    }
    if (!(temporaryDirectory.mkdir())) {
      throw new RuntimeException(
          "Could not create temp directory: " + temporaryDirectory.getAbsolutePath());
    }

    preferences.setString(R.string.p_backup_dir, temporaryDirectory.getAbsolutePath());

    // make a temporary task
    Task task = new Task();
    task.setTitle("helicopter");
    taskDao.createNew(task);
  }

  @Override
  protected void inject(TestComponent component) {
    component.inject(this);
  }

  @After
  public void tearDown() {
    if (temporaryDirectory != null) {
      for (File file : temporaryDirectory.listFiles()) {
        file.delete();
      }
      temporaryDirectory.delete();
    }
  }

  @Test
  public void testBackup() {
    assertEquals(0, temporaryDirectory.list().length);

    // create a backup
    BackupWork service = new BackupWork(getTargetContext(), jsonExporter, preferences);
    service.startBackup(getTargetContext());

    AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

    // assert file created
    File[] files = temporaryDirectory.listFiles();
    assertEquals(1, files.length);
    assertTrue(files[0].getName().matches(BackupWork.BACKUP_FILE_NAME_REGEX));
  }
}
