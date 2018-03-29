package org.tasks.jobs;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

import android.content.Context;
import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.tasks.backup.TasksJsonExporter;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class BackupJob extends Job {

  public static final String TAG = "job_backup";
  static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.json";
  static final FileFilter FILE_FILTER = f -> f.getName().matches(BACKUP_FILE_NAME_REGEX);
  private static final Comparator<File> BY_LAST_MODIFIED = (f1, f2) ->
      Long.compare(f2.lastModified(), f1.lastModified());

  private static final int DAYS_TO_KEEP_BACKUP = 7;
  private final Context context;
  private final TasksJsonExporter tasksJsonExporter;
  private final Preferences preferences;

  BackupJob(Context context, TasksJsonExporter tasksJsonExporter, Preferences preferences) {
    this.context = context;
    this.tasksJsonExporter = tasksJsonExporter;
    this.preferences = preferences;
  }

  @NonNull
  @Override
  protected Result onRunJob(@NonNull Params params) {
    startBackup(context);
    return Result.SUCCESS;
  }

  void startBackup(Context context) {
    try {
      deleteOldBackups();
    } catch (Exception e) {
      Timber.e(e, e.getMessage());
    }

    try {
      tasksJsonExporter
          .exportTasks(context, TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE, null);
    } catch (Exception e) {
      Timber.e(e, e.getMessage());
    }
  }

  private void deleteOldBackups() {
    File astridDir = preferences.getBackupDirectory();
    if (astridDir == null) {
      return;
    }

    // grab all backup files, sort by modified date, delete old ones
    File[] fileArray = astridDir.listFiles(FILE_FILTER);
    for (File file : getDeleteList(fileArray, DAYS_TO_KEEP_BACKUP)) {
      if (!file.delete()) {
        Timber.e("Unable to delete: %s", file);
      }
    }
  }

  static List<File> getDeleteList(File[] fileArray, int keepNewest) {
    if (fileArray == null) {
      return emptyList();
    }

    List<File> files = Arrays.asList(fileArray);
    Collections.sort(files, BY_LAST_MODIFIED);
    return newArrayList(skip(files, keepNewest));
  }
}
