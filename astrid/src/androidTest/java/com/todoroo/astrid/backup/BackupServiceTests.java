/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.backup.BackupService.BackupDirectorySetting;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.inject.Inject;

import static org.tasks.date.DateTimeUtils.newDate;

public class BackupServiceTests extends DatabaseTestCase {

    private static final long BACKUP_WAIT_TIME = 500L;

    File temporaryDirectory = null;

    @Inject TasksXmlExporter xmlExporter;
    @Inject TaskDao taskDao;
    @Inject Preferences preferences;

    BackupDirectorySetting setting = new BackupDirectorySetting() {
        public File getBackupDirectory() {
            return temporaryDirectory;
        }
    };

    @Override
    protected void setUp() {
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

        // make a temporary task
        Task task = new Task();
        task.setTitle("helicopter");
        taskDao.createNew(task);
    }

    @Override
    protected void tearDown() {
        super.tearDown();

        if (temporaryDirectory != null) {
            for (File file : temporaryDirectory.listFiles())
                file.delete();
            temporaryDirectory.delete();
        }
    }

    private boolean getBackupSetting() {
        return preferences.getBoolean(R.string.backup_BPr_auto_key, true);
    }

    private void setBackupSetting(boolean setting) {
        preferences.setBoolean(R.string.backup_BPr_auto_key, setting);
    }

    public void disabled_testBackup() {
        assertEquals(0, temporaryDirectory.list().length);

        boolean backupSetting = getBackupSetting();
        try {
            setBackupSetting(true);
            preferences.setLong(BackupPreferences.PREF_BACKUP_LAST_DATE, 0);

            // create a backup
            BackupService service = new BackupService();
            service.setBackupDirectorySetting(setting);
            service.testBackup(xmlExporter, preferences, getContext());

            AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

            // assert file created
            File[] files = temporaryDirectory.listFiles();
            assertEquals(1, files.length);
            assertTrue(files[0].getName().matches(BackupService.BACKUP_FILE_NAME_REGEX));

            // assert summary updated
            assertTrue(preferences.getLong(BackupPreferences.PREF_BACKUP_LAST_DATE, 0) > 0);
            assertNull(preferences.getStringValue(BackupPreferences.PREF_BACKUP_LAST_ERROR));
        } finally {
            setBackupSetting(backupSetting);
        }
    }

    public void disabled_testNoBackup() {
        assertEquals(0, temporaryDirectory.list().length);
        System.err.println("test no backup");
        boolean backupSetting = getBackupSetting();
        try {
            setBackupSetting(false);
            preferences.setLong(BackupPreferences.PREF_BACKUP_LAST_DATE, 0);

            // create a backup
            BackupService service = new BackupService();
            service.setBackupDirectorySetting(new BackupDirectorySetting() {
                public File getBackupDirectory() {
                    fail("Why was this method called?");
                    return null;
                }
            });
            service.testBackup(xmlExporter, preferences, getContext());

            AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

            // assert no file created
            File[] files = temporaryDirectory.listFiles();
            assertEquals(0, files.length);

            // assert summary not updated
            assertEquals(0, preferences.getLong(BackupPreferences.PREF_BACKUP_LAST_DATE, 0));
        } finally {
            setBackupSetting(backupSetting);
        }
    }

    public void testDeletion() throws IOException {
        // create a bunch of backups
        assertEquals(0, temporaryDirectory.list().length);

        boolean backupSetting = getBackupSetting();
        try {
            setBackupSetting(true);

            // create some user files
            File myFile = new File(temporaryDirectory, "beans");
            myFile.createNewFile();

            // create some backup files
            for (int i = 0; i < 10; i++) {
                DateFormat df = new SimpleDateFormat("MMdd-HHmm");
                String name = String.format("auto.%02d%s.xml", i, df.format(newDate()));
                File tempFile = new File(temporaryDirectory, name);
                tempFile.createNewFile();
            }

            // make one really old
            File[] files = temporaryDirectory.listFiles();
            files[4].setLastModified(System.currentTimeMillis() - 20000);

            // assert files created
            assertEquals(11, files.length);

            // backup
            BackupService service = new BackupService();
            service.setBackupDirectorySetting(setting);
            service.testBackup(xmlExporter, preferences, getContext());

            AndroidUtilities.sleepDeep(BACKUP_WAIT_TIME);

            // assert the oldest file was deleted
            assertTrue(temporaryDirectory.listFiles().length < 11);
            assertFalse(files[4].exists());

            // assert user file still exists
            service.testBackup(xmlExporter, preferences, getContext());
            assertTrue(myFile.exists());

        } finally {
            setBackupSetting(backupSetting);
        }
    }

}
