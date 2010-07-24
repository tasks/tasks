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

/**
 * Inspired heavily by SynchronizationService
 */
public class BackupService extends Service {

    /**
     * when after phone starts to start first back up
     */
    private static final long BACKUP_OFFSET = 5 * 60 * 1000L;

    /**
     * how often to back up
     */
    private static final long BACKUP_INTERVAL = AlarmManager.INTERVAL_DAY;
    public static final String BACKUP_ACTION = "backup";
    public static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.xml";
    private static final int DAYS_TO_KEEP_BACKUP = 7;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent.getAction().equals(BACKUP_ACTION)) {
            startBackup(this);
        }
    }

    /**
     * Test hook for backup
     * @param ctx
     */
    public void testBackup(Context ctx) {
        startBackup(ctx);
    }

    private void startBackup(Context ctx) {
        /*if (ctx == null || ctx.getResources() == null) {
            return;
        }
        try {
            if (!Preferences.isBackupEnabled(ctx)) {
                return;
            }

            try {
                deleteOldBackups();
            } catch (Exception e) {
                Log.e("error-deleting", "Error deleting old backups: " + e);
            }
            TasksXmlExporter exporter = new TasksXmlExporter(true);
            exporter.setContext(ctx);
            exporter.exportTasks(backupDirectorySetting.getBackupDirectory());
            Preferences.setBackupSummary(ctx,
                    ctx.getString(R.string.BPr_backup_desc_success,
                            BackupDateUtilities.getFormattedDate(ctx, new Date())));
        } catch (Exception e) {
            // unable to backup.
            if (e == null || e.getMessage() == null) {
                Preferences.setBackupSummary(ctx,
                        ctx.getString(R.string.BPr_backup_desc_failure_null));
            } else {
                Preferences.setBackupSummary(ctx,
                        ctx.getString(R.string.BPr_backup_desc_failure,
                                e.toString()));
            }
        }*/
    }

    public static void scheduleService(Context ctx) {
        /*AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(ctx, 0,
                createAlarmIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
        if (!Preferences.isBackupEnabled(ctx)) {
            return;
        }
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + BACKUP_OFFSET,
                BACKUP_INTERVAL, pendingIntent);*/
    }

    public static void unscheduleService(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(ctx, 0,
                createAlarmIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
    }

    private static Intent createAlarmIntent(Context ctx) {
        Intent intent = new Intent(ctx, BackupService.class);
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

        // grab all backup files, sort by modified date, delete old ones
        File[] files = astridDir.listFiles(backupFileFilter);
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return -Long.valueOf(file1.lastModified()).compareTo(Long.valueOf(file2.lastModified()));
            }
        });
        for(int i = DAYS_TO_KEEP_BACKUP; i < files.length; i++) {
            files[i].delete();
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
            return null; //TasksXmlExporter.getExportDirectory();
        }
    };

    public void setBackupDirectorySetting(
            BackupDirectorySetting backupDirectorySetting) {
        this.backupDirectorySetting = backupDirectorySetting;
    }
}
