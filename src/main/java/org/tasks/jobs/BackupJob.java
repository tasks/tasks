package org.tasks.jobs;

import android.content.Context;

import com.todoroo.astrid.backup.TasksXmlExporter;

import org.tasks.injection.ForApplication;
import org.tasks.injection.IntentServiceComponent;
import org.tasks.preferences.Preferences;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import javax.inject.Inject;

import timber.log.Timber;

public class BackupJob extends MidnightJob {

    public static final String TAG = "job_backup";

    public static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.xml"; //$NON-NLS-1$
    private static final int DAYS_TO_KEEP_BACKUP = 7;

    @Inject @ForApplication Context context;
    @Inject JobManager jobManager;
    @Inject TasksXmlExporter tasksXmlExporter;
    @Inject Preferences preferences;

    public BackupJob() {
        super(BackupJob.class.getSimpleName());
    }

    BackupJob(Context context, JobManager jobManager, TasksXmlExporter tasksXmlExporter, Preferences preferences) {
        this();

        this.context = context;
        this.jobManager = jobManager;
        this.tasksXmlExporter = tasksXmlExporter;
        this.preferences = preferences;
    }

    @Override
    protected void run() {
        startBackup(context);
    }

    @Override
    protected void scheduleNext() {
        jobManager.scheduleMidnightBackup();
    }

    void startBackup(Context context) {
        try {
            deleteOldBackups();
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }

        try {
            tasksXmlExporter.exportTasks(context, TasksXmlExporter.ExportType.EXPORT_TYPE_SERVICE, null);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }

    private void deleteOldBackups() {
        FileFilter backupFileFilter = file -> {
            if (file.getName().matches(BACKUP_FILE_NAME_REGEX)) {
                return true;
            }
            return false;
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

        Arrays.sort(files, (file1, file2) -> -Long.valueOf(file1.lastModified()).compareTo(file2.lastModified()));
        for(int i = DAYS_TO_KEEP_BACKUP; i < files.length; i++) {
            if(!files[i].delete()) {
                Timber.i("Unable to delete: %s", files[i]);
            }
        }
    }

    @Override
    protected void inject(IntentServiceComponent component) {
        component.inject(this);
    }
}
