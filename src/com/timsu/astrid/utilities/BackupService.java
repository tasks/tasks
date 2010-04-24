package com.timsu.astrid.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.io.File;
import java.io.FilenameFilter;

public class BackupService extends Service {
    /* Inspired heavily by SynchronizationService
    
     */

    private static final long BACKUP_OFFSET = 5*60*1000L;
    private static final String BACKUP_ACTION = "backup";
    private static final String BACKUP_FILE_NAME_REGEX = "auto\\.\\d{6}\\-\\d{4}\\.xml";
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

    private void startBackup(Context ctx) {
        if (ctx == null || ctx.getResources() == null) {
            return;
        }
        if (!Preferences.isBackupEnabled(ctx)) {
            return;
        }
        deleteOldBackups();
        TasksXmlExporter exporter = new TasksXmlExporter(true);
        exporter.setContext(ctx);
        exporter.exportTasks();
    }

    public static void scheduleService(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(ctx, 0,
                createAlarmIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pendingIntent);
        if (!Preferences.isBackupEnabled(ctx)) {
            return;
        }
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + BACKUP_OFFSET,
                AlarmManager.INTERVAL_DAY, pendingIntent);
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
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.matches(BACKUP_FILE_NAME_REGEX)) {
                    String dateString = s.substring(12, 18);
                    return DateUtilities.wasCreatedBefore(dateString, DAYS_TO_KEEP_BACKUP);
                }
                return false;
            }
        };
        File astridDir = TasksXmlExporter.getExportDirectory();
        String[] files = astridDir.list(filter);
        for (String file : files) {
            new File(astridDir, file).delete();
        }
    }
}
