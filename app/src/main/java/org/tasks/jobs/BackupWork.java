package org.tasks.jobs;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static java.util.Collections.emptyList;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.backup.TasksJsonExporter;
import org.tasks.injection.ForApplication;
import org.tasks.injection.JobComponent;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class BackupWork extends RepeatingWorker {

  static final String BACKUP_FILE_NAME_REGEX = "auto\\.[-\\d]+\\.json";
  static final FileFilter FILE_FILTER = f -> f.getName().matches(BACKUP_FILE_NAME_REGEX);
  private static final Comparator<File> BY_LAST_MODIFIED =
      (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified());

  private static final int DAYS_TO_KEEP_BACKUP = 7;
  @Inject @ForApplication Context context;
  @Inject TasksJsonExporter tasksJsonExporter;
  @Inject Preferences preferences;
  @Inject WorkManager workManager;

  public BackupWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @Override
  protected Result run() {
    startBackup(context);
    preferences.setLong(R.string.p_last_backup, now());
    return Result.SUCCESS;
  }

  @Override
  protected void scheduleNext() {
    workManager.scheduleBackup();
  }

  static List<File> getDeleteList(File[] fileArray, int keepNewest) {
    if (fileArray == null) {
      return emptyList();
    }

    List<File> files = Arrays.asList(fileArray);
    Collections.sort(files, BY_LAST_MODIFIED);
    return newArrayList(skip(files, keepNewest));
  }

  @Override
  protected void inject(JobComponent component) {
    component.inject(this);
  }

  void startBackup(Context context) {
    try {
      deleteOldBackups();
    } catch (Exception e) {
      Timber.e(e);
    }

    try {
      tasksJsonExporter.exportTasks(
          context, TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE, null);
    } catch (Exception e) {
      Timber.e(e);
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
}
