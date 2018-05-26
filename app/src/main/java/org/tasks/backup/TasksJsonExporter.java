package org.tasks.backup;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class TasksJsonExporter {

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
  private final TaskDao taskDao;
  private final UserActivityDao userActivityDao;
  private final Preferences preferences;
  private Context context;
  private int exportCount = 0;
  private ProgressDialog progressDialog;
  private Handler handler;
  private File backupDirectory;
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
      CaldavDao caldavDao) {
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
    this.backupDirectory = preferences.getBackupDirectory();
    this.latestSetVersionName = null;
    this.progressDialog = progressDialog;

    handler = exportType == ExportType.EXPORT_TYPE_MANUAL ? new Handler() : null;

    new Thread(
            () -> {
              try {
                String output = setupFile(backupDirectory, exportType);

                List<Task> tasks = taskDao.getAll();

                if (tasks.size() > 0) {
                  doTasksExport(output, tasks);
                }

                if (exportType == ExportType.EXPORT_TYPE_MANUAL) {
                  onFinishExport(output);
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
            })
        .start();
  }

  private void doTasksExport(String output, List<Task> tasks) throws IOException {

    List<BackupContainer.TaskBackup> taskBackups = new ArrayList<>();

    for (Task task : tasks) {
      setProgress(taskBackups.size(), tasks.size());
      long taskId = task.getId();
      taskBackups.add(
          new BackupContainer.TaskBackup(
              task,
              alarmDao.getAlarms(taskId),
              locationDao.getGeofences(taskId),
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
            tagDataDao.getAll(),
            filterDao.getAll(),
            googleTaskListDao.getAccounts(),
            googleTaskListDao.getAllLists(),
            caldavDao.getAccounts(),
            caldavDao.getCalendars()));

    File file = new File(output);
    file.createNewFile();
    FileOutputStream fos = new FileOutputStream(file);
    OutputStreamWriter out = new OutputStreamWriter(fos);
    Gson gson = BuildConfig.DEBUG ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
    out.write(gson.toJson(data));
    out.close();
    fos.close();
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

  /**
   * Creates directories if necessary and returns fully qualified file
   *
   * @return output file name
   */
  private String setupFile(File directory, ExportType exportType) throws IOException {
    if (directory != null) {
      // Check for /sdcard/astrid directory. If it doesn't exist, make it.
      if (directory.exists() || directory.mkdir()) {
        String fileName;
        switch (exportType) {
          case EXPORT_TYPE_SERVICE:
            fileName = String.format(BackupConstants.BACKUP_FILE_NAME, getDateForExport());
            break;
          case EXPORT_TYPE_MANUAL:
            fileName = String.format(BackupConstants.EXPORT_FILE_NAME, getDateForExport());
            break;
          case EXPORT_TYPE_ON_UPGRADE:
            fileName = String.format(BackupConstants.UPGRADE_FILE_NAME, latestSetVersionName);
            break;
          default:
            throw new IllegalArgumentException("Invalid export type"); // $NON-NLS-1$
        }
        return directory.getAbsolutePath() + File.separator + fileName;
      } else {
        // Unable to make the /sdcard/astrid directory.
        throw new IOException(
            context.getString(R.string.DLG_error_sdcard, directory.getAbsolutePath()));
      }
    } else {
      // Unable to access the sdcard because it's not in the mounted state.
      throw new IOException(context.getString(R.string.DLG_error_sdcard_general));
    }
  }

  public enum ExportType {
    EXPORT_TYPE_SERVICE,
    EXPORT_TYPE_MANUAL,
    EXPORT_TYPE_ON_UPGRADE
  }
}
