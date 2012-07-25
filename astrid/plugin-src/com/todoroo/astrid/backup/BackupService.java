/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.backup;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;

/**
 * Inspired heavily by SynchronizationService
 */
public class BackupService extends Service {

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
            PluginServices.getExceptionService().reportError("backup-bg-sync", e); //$NON-NLS-1$
        }
    }

    /**
     * Test hook for backup
     * @param context
     */
    public void testBackup(Context context) {
        startBackup(context);
    }

    private void startBackup(Context context) {
        if (context == null || context.getResources() == null) {
            return;
        }
        try {
            if (!Preferences.getBoolean(R.string.backup_BPr_auto_key, true)) {
                return;
            }

            try {
                deleteOldBackups();
            } catch (Exception e) {
                Log.e("error-deleting", "Error deleting old backups", e); //$NON-NLS-1$ //$NON-NLS-2$
            }

            TasksXmlExporter.exportTasks(context, TasksXmlExporter.ExportType.EXPORT_TYPE_SERVICE, null,
                    backupDirectorySetting.getBackupDirectory(), null);

        } catch (Exception e) {
            Log.e("error-backup", "Error starting backups", e); //$NON-NLS-1$ //$NON-NLS-2$
            Preferences.setString(BackupPreferences.PREF_BACKUP_LAST_ERROR, e.toString());
        }
    }

    public static void scheduleService(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
        if (!Preferences.getBoolean(R.string.backup_BPr_auto_key, true)) {
            return;
        }
        am.setInexactRepeating(AlarmManager.RTC, DateUtilities.now() + BACKUP_OFFSET,
                BACKUP_INTERVAL, pendingIntent);
    }

    public static void unscheduleService(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                createAlarmIntent(context), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
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
        if(astridDir == null)
            return;

        // grab all backup files, sort by modified date, delete old ones
        File[] files = astridDir.listFiles(backupFileFilter);
        if(files == null)
            return;

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return -Long.valueOf(file1.lastModified()).compareTo(Long.valueOf(file2.lastModified()));
            }
        });
        for(int i = DAYS_TO_KEEP_BACKUP; i < files.length; i++) {
            if(!files[i].delete())
                Log.i("astrid-backups", "Unable to delete: " + files[i]); //$NON-NLS-1$ //$NON-NLS-2$
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
        public File getBackupDirectory() {
            return BackupConstants.defaultExportDirectory();
        }
    };

    public void setBackupDirectorySetting(
            BackupDirectorySetting backupDirectorySetting) {
        this.backupDirectorySetting = backupDirectorySetting;
    }
}
