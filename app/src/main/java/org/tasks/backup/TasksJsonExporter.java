package org.tasks.backup;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.data.AlarmDao;
import org.tasks.data.CaldavDao;
import org.tasks.data.FilterDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.UserActivityDao;
import org.tasks.drive.DriveInvoker;
import org.tasks.files.FileHelper;
import org.tasks.jobs.WorkManager;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class TasksJsonExporter {

  static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String MIME = "application/json";
  private static final String EXTENSION = ".json";

  // --- public interface
  private final TagDataDao tagDataDao;

  // --- implementation
  private final AlarmDao alarmDao;
  private final LocationDao locationDao;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final FilterDao filterDao;
  private final GoogleTaskListDao googleTaskListDao;
  private final TaskAttachmentDao taskAttachmentDao;
  private final CaldavDao caldavDao;
  private final WorkManager workManager;
  private final TaskDao taskDao;
  private final UserActivityDao userActivityDao;
  private final Preferences preferences;
  private Context context;
  private int exportCount = 0;
  private ProgressDialog progressDialog;
  private Handler handler;
  private String latestSetVersionName;

  @Inject
  public TasksJsonExporter(
      TagDataDao tagDataDao,
      TaskDao taskDao,
      UserActivityDao userActivityDao,
      Preferences preferences,
      AlarmDao alarmDao,
      LocationDao locationDao,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao,
      FilterDao filterDao,
      GoogleTaskListDao googleTaskListDao,
      TaskAttachmentDao taskAttachmentDao,
      CaldavDao caldavDao,
      WorkManager workManager,
      DriveInvoker driveInvoker) {
    this.tagDataDao = tagDataDao;
    this.taskDao = taskDao;
    this.userActivityDao = userActivityDao;
    this.preferences = preferences;
    this.alarmDao = alarmDao;
    this.locationDao = locationDao;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
    this.filterDao = filterDao;
    this.googleTaskListDao = googleTaskListDao;
    this.taskAttachmentDao = taskAttachmentDao;
    this.caldavDao = caldavDao;
    this.workManager = workManager;
  }

  private static String getDateForExport() {
    return newDateTime().toString("yyMMdd-HHmm");
  }

  private void post(Runnable runnable) {
    if (handler != null) {
      handler.post(runnable);
    }
  }

  private void setProgress(final int taskNumber, final int total) {
    post(
        () -> {
          progressDialog.setMax(total);
          progressDialog.setProgress(taskNumber);
        });
  }

  public void exportTasks(
      final Context context,
      final ExportType exportType,
      @Nullable final ProgressDialog progressDialog) {
    this.context = context;
    this.exportCount = 0;
    this.latestSetVersionName = null;
    this.progressDialog = progressDialog;

    if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
      handler = new Handler();
      new Thread(() -> runBackup(exportType)).start();
    } else {
      runBackup(exportType);
    }
  }

  private void runBackup(ExportType exportType) {
    try {
      String filename = getFileName(exportType);
      List<Task> tasks = taskDao.getAll();

      if (tasks.size() > 0) {
        String basename = Files.getNameWithoutExtension(filename);
        Uri uri =
            FileHelper.newFile(
                context, preferences.getBackupDirectory(), MIME, basename, EXTENSION);
        OutputStream os = context.getContentResolver().openOutputStream(uri);
        doTasksExport(os, tasks);
        os.close();
        workManager.scheduleDriveUpload(uri, exportType == ExportType.EXPORT_TYPE_SERVICE);
      }

      if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
        onFinishExport(filename);
      }
    } catch (IOException e) {
      Timber.e(e);
    } finally {
      post(
          () -> {
            if (progressDialog != null
                && progressDialog.isShowing()
                && context instanceof Activity) {
              DialogUtilities.dismissDialog((Activity) context, progressDialog);
            }
          });
    }
  }

  private void doTasksExport(OutputStream os, List<Task> tasks) throws IOException {

    List<BackupContainer.TaskBackup> taskBackups = new ArrayList<>();

    for (Task task : tasks) {
      setProgress(taskBackups.size(), tasks.size());
      long taskId = task.getId();
      taskBackups.add(
          new BackupContainer.TaskBackup(
              task,
              alarmDao.getAlarms(taskId),
              locationDao.getGeofencesForTask(taskId),
              tagDao.getTagsForTask(taskId),
              googleTaskDao.getAllByTaskId(taskId),
              userActivityDao.getCommentsForTask(task.getUuid()),
              taskAttachmentDao.getAttachments(task.getUuid()),
              caldavDao.getTasks(taskId)));
    }

    Map<String, Object> data = new HashMap<>();
    data.put("version", BuildConfig.VERSION_CODE);
    data.put("timestamp", System.currentTimeMillis());
    data.put(
        "data",
        new BackupContainer(
            taskBackups,
            locationDao.getPlaces(),
            tagDataDao.getAll(),
            filterDao.getAll(),
            googleTaskListDao.getAccounts(),
            googleTaskListDao.getAllLists(),
            caldavDao.getAccounts(),
            caldavDao.getCalendars(),
            preferences.getPrefs(Integer.class),
            preferences.getPrefs(Long.class),
            preferences.getPrefs(String.class),
            preferences.getPrefs(Boolean.class)));

    OutputStreamWriter out = new OutputStreamWriter(os, UTF_8);
    Gson gson = BuildConfig.DEBUG ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
    out.write(gson.toJson(data));
    out.close();
    exportCount = taskBackups.size();
  }

  private void onFinishExport(final String outputFile) {
    post(
        () -> {
          if (exportCount == 0) {
            Toast.makeText(
                    context, context.getString(R.string.export_toast_no_tasks), Toast.LENGTH_LONG)
                .show();
          } else {
            CharSequence text =
                String.format(
                    context.getString(R.string.export_toast),
                    context
                        .getResources()
                        .getQuantityString(R.plurals.Ntasks, exportCount, exportCount),
                    outputFile);
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
          }
        });
  }

  private String getFileName(ExportType type) {
    switch (type) {
      case EXPORT_TYPE_SERVICE:
        return String.format(BackupConstants.BACKUP_FILE_NAME, getDateForExport());
      case EXPORT_TYPE_MANUAL:
        return String.format(BackupConstants.EXPORT_FILE_NAME, getDateForExport());
      case EXPORT_TYPE_ON_UPGRADE:
        return String.format(BackupConstants.UPGRADE_FILE_NAME, latestSetVersionName);
      default:
        throw new UnsupportedOperationException("Unhandled export type");
    }
  }

  public enum ExportType {
    EXPORT_TYPE_SERVICE,
    EXPORT_TYPE_MANUAL,
    EXPORT_TYPE_ON_UPGRADE
  }
}
