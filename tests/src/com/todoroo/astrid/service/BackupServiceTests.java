package com.todoroo.astrid.service;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.timsu.astrid.utilities.Preferences;
import com.todoroo.andlib.test.TodorooTestCase;
import com.todoroo.astrid.backup.BackupService;
import com.todoroo.astrid.backup.BackupService.BackupDirectorySetting;

public class BackupServiceTests extends TodorooTestCase {

    File temporaryDirectory = null;

    BackupDirectorySetting setting = new BackupDirectorySetting() {
        public File getBackupDirectory() {
            return temporaryDirectory;
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        temporaryDirectory = File.createTempFile("backup",
                Long.toString(System.nanoTime()));
        if(!(temporaryDirectory.delete()))
            throw new IOException("Could not delete temp file: " + temporaryDirectory.getAbsolutePath());
        if(!(temporaryDirectory.mkdir()))
            throw new IOException("Could not create temp directory: " + temporaryDirectory.getAbsolutePath());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if(temporaryDirectory != null) {
            for(File file : temporaryDirectory.listFiles())
                file.delete();
            temporaryDirectory.delete();
        }
    }

    /** Test backup works */
    public void testBackup() {
        assertEquals(0, temporaryDirectory.list().length);

        boolean backupSetting = Preferences.isBackupEnabled(getContext());
        try {
            Preferences.setBackupEnabled(getContext(), true);
            Preferences.setBackupSummary(getContext(), "");

            // create a backup
            BackupService service = new BackupService();
            service.setBackupDirectorySetting(setting);
            service.testBackup(getContext());

            // assert file created
            File[] files = temporaryDirectory.listFiles();
            assertEquals(1, files.length);
            assertTrue(files[0].getName().matches(BackupService.BACKUP_FILE_NAME_REGEX));

            // assert summary updated
            assertTrue(Preferences.getBackupSummary(getContext()).length() > 0);
            assertFalse(Preferences.getBackupSummary(getContext()).toLowerCase().contains("error"));
        } finally {
            Preferences.setBackupEnabled(getContext(), backupSetting);
        }
    }

    /** Test no backup */
    public void testNoBackup() {
        assertEquals(0, temporaryDirectory.list().length);

        boolean backupSetting = Preferences.isBackupEnabled(getContext());
        try {
            Preferences.setBackupEnabled(getContext(), false);
            Preferences.setBackupSummary(getContext(), "");

            // create a backup
            BackupService service = new BackupService();
            service.setBackupDirectorySetting(new BackupDirectorySetting() {
                public File getBackupDirectory() {
                    fail("Why was this method called?");
                    return null;
                }
            });
            service.testBackup(getContext());

            // assert no file created
            File[] files = temporaryDirectory.listFiles();
            assertEquals(0, files.length);

            // assert summary not updated
            assertEquals(0, Preferences.getBackupSummary(getContext()).length());
        } finally {
            Preferences.setBackupEnabled(getContext(), backupSetting);
        }
    }

    public void testDeletion() throws IOException {
        // create a bunch of backups
        assertEquals(0, temporaryDirectory.list().length);

        boolean backupSetting = Preferences.isBackupEnabled(getContext());
        try {
            Preferences.setBackupEnabled(getContext(), true);
            Preferences.setBackupSummary(getContext(), "");

            // create some user files
            File myFile = new File(temporaryDirectory, "beans");
            myFile.createNewFile();

            // create some backup files
            for(int i = 0; i < 10; i++) {
                DateFormat df = new SimpleDateFormat("MMdd-HHmm");
                String name = String.format("auto.%02d%s.xml", i, df.format(new Date()));
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
            service.testBackup(getContext());

            // assert the oldest file was deleted
            assertTrue(temporaryDirectory.listFiles().length < 11);
            assertFalse(files[4].exists());

            // assert user file still exists
            service.testBackup(getContext());
            assertTrue(myFile.exists());

        } finally {
            Preferences.setBackupEnabled(getContext(), backupSetting);
        }
    }

}
