package org.tasks.scheduling;

import android.content.Context;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import javax.inject.Inject;

import timber.log.Timber;

public class BackupIntentService extends MidnightIntentService {

    public static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.xml"; //$NON-NLS-1$
    private static final int DAYS_TO_KEEP_BACKUP = 7;

    @Inject TasksXmlExporter xmlExporter;
    @Inject Preferences preferences;

    public BackupIntentService() {
        super(BackupIntentService.class.getSimpleName());
    }

    @Override
    void run() {
        startBackup(this);
    }

    @Override
    String getLastRunPreference() {
        return TasksXmlExporter.PREF_BACKUP_LAST_DATE;
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
            deleteOldBackups();
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }

        try {
            xmlExporter.exportTasks(context, TasksXmlExporter.ExportType.EXPORT_TYPE_SERVICE, null);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
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
        File astridDir = preferences.getBackupDirectory();
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
                Timber.i("Unable to delete: %s", files[i]);
            }
        }
    }
}
