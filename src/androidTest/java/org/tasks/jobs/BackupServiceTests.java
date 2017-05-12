/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.tasks.jobs;

import android.support.test.runner.AndroidJUnit4;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.backup.TasksXmlExporter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;
import org.tasks.injection.TestComponent;
import org.tasks.preferences.Preferences;
import org.tasks.scheduling.AlarmManager;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

@RunWith(AndroidJUnit4.class)
public class BackupServiceTests extends DatabaseTestCase {

    private static final long BACKUP_WAIT_TIME = 500L;

    File temporaryDirectory = null;

    @Inject TasksXmlExporter xmlExporter;
    @Inject TaskDao taskDao;
    @Inject Preferences preferences;

    @Override
    public void setUp() {
        super.setUp();

        try {
            temporaryDirectory = File.createTempFile("backup", Long.toString(System.nanoTime()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!(temporaryDirectory.delete()))
            throw new RuntimeException("Could not delete temp file: " + temporaryDirectory.getAbsolutePath());
        if (!(temporaryDirectory.mkdir()))
            throw new RuntimeException("Could not create temp directory: " + temporaryDirectory.getAbsolutePath());

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

    @Override
    public void tearDown() {
        super.tearDown();

        if (temporaryDirectory != null) {
            for (File file : temporaryDirectory.listFiles())
                file.delete();
            temporaryDirectory.delete();
        }
    }

    @Ignore
    @Test
    public void testBackup() {
        assertEquals(0, temporaryDirectory.list().length);

        preferences.setLong(TasksXmlExporter.PREF_BACKUP_LAST_DATE, 0);

        // create a backup
        BackupJob service = new BackupJob(getTargetContext(), new JobManager(getTargetContext(), mock(AlarmManager.class)), xmlExporter, preferences);
        service.startBackup(getTargetContext());

        AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

        // assert file created
        File[] files = temporaryDirectory.listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().matches(BackupJob.BACKUP_FILE_NAME_REGEX));

        // assert summary updated
        assertTrue(preferences.getLong(TasksXmlExporter.PREF_BACKUP_LAST_DATE, 0) > 0);
    }

    @Test
    public void testDeletion() throws IOException {
        // create a bunch of backups
        assertEquals(0, temporaryDirectory.list().length);

        // create some user files
        File myFile = new File(temporaryDirectory, "beans");
        myFile.createNewFile();

        // create some backup files
        for (int i = 0; i < 10; i++) {
            String name = String.format("auto.%02d%s.xml", i, newDateTime().toString("MMdd-HHmm"));
            File tempFile = new File(temporaryDirectory, name);
            tempFile.createNewFile();
        }

        // make one really old
        File[] files = temporaryDirectory.listFiles();
        files[4].setLastModified(currentTimeMillis() - 20000);

        // assert files created
        assertEquals(11, files.length);

        // backup
        BackupJob service = new BackupJob(getTargetContext(), new JobManager(getTargetContext(), mock(AlarmManager.class)), xmlExporter, preferences);
        service.startBackup(getTargetContext());

        AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

        // assert the oldest file was deleted
        assertTrue(temporaryDirectory.listFiles().length < 11);
        assertFalse(files[4].exists());

        // assert user file still exists
        service.startBackup(getTargetContext());
        assertTrue(myFile.exists());
    }
}
