/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.InjectingService;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import javax.inject.Inject;

/**
 * Inspired heavily by SynchronizationService
 */
public class BackupService extends InjectingService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    // --- constants for backup

    /**
     * when after phone starts to start first back up
     */
    private static final long BACKUP_OFFSET = 5 * 60 * 1000L;

    /**
     * how often to back up
     */
    private static final long BACKUP_INTERVAL = AlarmManager.INTERVAL_DAY;
    public static final String BACKUP_ACTION = "backup"; //$NON-NLS-1$
    public static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.xml"; //$NON-NLS-1$
    private static final int DAYS_TO_KEEP_BACKUP = 7;

    @Inject TasksXmlExporter xmlExporter;
    @Inject Preferences preferences;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        try {
            ContextManager.setContext(this);
            startBackup(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Test hook for backup
     */
    void testBackup(TasksXmlExporter xmlExporter, Preferences preferences, Context context) {
        this.xmlExporter = xmlExporter;
        this.preferences = preferences;
        startBackup(context);
    }

    private void startBackup(Context context) {
        if (context == null || context.getResources() == null) {
            return;
        }
        try {
            if (!preferences.getBoolean(R.string.backup_BPr_auto_key, true)) {
                return;
            }

            try {
                deleteOldBackups();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            xmlExporter.exportTasks(context, TasksXmlExporter.ExportType.EXPORT_TYPE_SERVICE,
                    backupDirectorySetting.getBackupDirectory());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            preferences.setString(BackupPreferences.PREF_BACKUP_LAST_ERROR, e.toString());
        }
    }

    public static void scheduleService(Preferences preferences, Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
        if (!preferences.getBoolean(R.string.backup_BPr_auto_key, true)) {
            return;
        }
        am.setInexactRepeating(AlarmManager.RTC, DateUtilities.now() + BACKUP_OFFSET,
                BACKUP_INTERVAL, pendingIntent);
    }

    private static Intent createAlarmIntent(Context context) {
        Intent intent = new Intent(context, BackupService.class);
        intent.setAction(BACKUP_ACTION);
        return intent;
    }

    private void deleteOldBackups() {
        FileFilter backupFileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.getName().matches(BACKUP_FILE_NAME_REGEX)) {
                    return true;
                }
                return false;
            }
        };
        File astridDir = backupDirectorySetting.getBackupDirectory();
        if(astridDir == null) {
            return;
        }

        // grab all backup files, sort by modified date, delete old ones
        File[] files = astridDir.listFiles(backupFileFilter);
        if(files == null) {
            return;
        }

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return -Long.valueOf(file1.lastModified()).compareTo(file2.lastModified());
            }
        });
        for(int i = DAYS_TO_KEEP_BACKUP; i < files.length; i++) {
            if(!files[i].delete()) {
                log.info("Unable to delete: {}", files[i]);
            }
        }
    }

    /**
     * Interface for setting where backups go
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public interface BackupDirectorySetting {
        public File getBackupDirectory();
    }

    private BackupDirectorySetting backupDirectorySetting = new BackupDirectorySetting() {
        @Override
        public File getBackupDirectory() {
            return BackupConstants.defaultExportDirectory();
        }
    };

    void setBackupDirectorySetting(
            BackupDirectorySetting backupDirectorySetting) {
        this.backupDirectorySetting = backupDirectorySetting;
    }
}
