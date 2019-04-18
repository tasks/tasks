package org.tasks.backup;

import static org.tasks.backup.TasksJsonExporter.UTF_8;
import static org.tasks.data.Place.newPlace;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.Alarm;
import org.tasks.data.AlarmDao;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.Filter;
import org.tasks.data.FilterDao;
import org.tasks.data.Geofence;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;
import org.tasks.data.Tag;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.data.TaskAttachment;
import org.tasks.data.TaskAttachmentDao;
import org.tasks.data.UserActivity;
import org.tasks.data.UserActivityDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class TasksJsonImporter {

  private final TagDataDao tagDataDao;
  private final UserActivityDao userActivityDao;
  private final DialogBuilder dialogBuilder;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final AlarmDao alarmDao;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final GoogleTaskListDao googleTaskListDao;
  private final FilterDao filterDao;
  private final TaskAttachmentDao taskAttachmentDao;
  private final CaldavDao caldavDao;
  private final Preferences preferences;
  private final LocationDao locationDao;

  private Activity activity;
  private Handler handler;
  private int taskCount;
  private int importCount = 0;
  private int skipCount = 0;
  private ProgressDialog progressDialog;
  private Uri input;

  @Inject
  public TasksJsonImporter(
      TagDataDao tagDataDao,
      UserActivityDao userActivityDao,
      DialogBuilder dialogBuilder,
      TaskDao taskDao,
      LocationDao locationDao,
      LocalBroadcastManager localBroadcastManager,
      AlarmDao alarmDao,
      TagDao tagDao,
      GoogleTaskDao googleTaskDao,
      GoogleTaskListDao googleTaskListDao,
      FilterDao filterDao,
      TaskAttachmentDao taskAttachmentDao,
      CaldavDao caldavDao,
      Preferences preferences) {
    this.tagDataDao = tagDataDao;
    this.userActivityDao = userActivityDao;
    this.dialogBuilder = dialogBuilder;
    this.taskDao = taskDao;
    this.locationDao = locationDao;
    this.localBroadcastManager = localBroadcastManager;
    this.alarmDao = alarmDao;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
    this.googleTaskListDao = googleTaskListDao;
    this.filterDao = filterDao;
    this.taskAttachmentDao = taskAttachmentDao;
    this.caldavDao = caldavDao;
    this.preferences = preferences;
  }

  private void setProgressMessage(final String message) {
    handler.post(() -> progressDialog.setMessage(message));
  }

  public void importTasks(Activity activity, Uri input, ProgressDialog progressDialog) {
    this.activity = activity;
    this.input = input;
    this.progressDialog = progressDialog;

    handler = new Handler();

    new Thread(this::performImport).start();
  }

  private void performImport() {
    Gson gson = new Gson();
    InputStream is;
    try {
      is = activity.getContentResolver().openInputStream(this.input);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
    InputStreamReader reader = new InputStreamReader(is, UTF_8);
    JsonObject input = gson.fromJson(reader, JsonObject.class);

    try {
      JsonElement data = input.get("data");
      int version = input.get("version").getAsInt();
      BackupContainer backupContainer = gson.fromJson(data, BackupContainer.class);
      for (TagData tagData : backupContainer.getTags()) {
        if (tagDataDao.getByUuid(tagData.getRemoteId()) == null) {
          tagDataDao.createNew(tagData);
        }
      }
      for (GoogleTaskAccount googleTaskAccount : backupContainer.getGoogleTaskAccounts()) {
        if (googleTaskListDao.getAccount(googleTaskAccount.getAccount()) == null) {
          googleTaskListDao.insert(googleTaskAccount);
        }
      }
      for (Place place : backupContainer.getPlaces()) {
        if (locationDao.getByUid(place.getUid()) == null) {
          locationDao.insert(place);
        }
      }
      for (GoogleTaskList googleTaskList : backupContainer.getGoogleTaskLists()) {
        if (googleTaskListDao.getByRemoteId(googleTaskList.getRemoteId()) == null) {
          googleTaskListDao.insert(googleTaskList);
        }
      }
      for (Filter filter : backupContainer.getFilters()) {
        if (filterDao.getByName(filter.getTitle()) == null) {
          filterDao.insert(filter);
        }
      }
      for (CaldavAccount account : backupContainer.getCaldavAccounts()) {
        if (caldavDao.getAccountByUuid(account.getUuid()) == null) {
          caldavDao.insert(account);
        }
      }
      for (CaldavCalendar calendar : backupContainer.getCaldavCalendars()) {
        if (caldavDao.getCalendarByUuid(calendar.getUuid()) == null) {
          caldavDao.insert(calendar);
        }
      }
      for (BackupContainer.TaskBackup backup : backupContainer.getTasks()) {
        taskCount++;
        setProgressMessage(activity.getString(R.string.import_progress_read, taskCount));
        Task task = backup.task;
        if (taskDao.fetch(task.getUuid()) != null) {
          skipCount++;
          continue;
        }
        taskDao.createNew(task);
        long taskId = task.getId();
        String taskUuid = task.getUuid();
        for (Alarm alarm : backup.alarms) {
          alarm.setTask(taskId);
          alarmDao.insert(alarm);
        }
        for (UserActivity comment : backup.comments) {
          comment.setTargetId(taskUuid);
          if (version < 546) {
            comment.convertPictureUri();
          }
          userActivityDao.createNew(comment);
        }
        for (GoogleTask googleTask : backup.google) {
          googleTask.setTask(taskId);
          googleTaskDao.insert(googleTask);
        }
        for (LegacyLocation location : backup.locations) {
          Place place = newPlace();
          place.setLongitude(location.longitude);
          place.setLatitude(location.latitude);
          place.setName(location.name);
          place.setAddress(location.address);
          place.setUrl(location.url);
          place.setPhone(location.phone);
          locationDao.insert(place);
          Geofence geofence = new Geofence();
          geofence.setTask(taskId);
          geofence.setPlace(place.getUid());
          geofence.setRadius(location.radius);
          geofence.setArrival(location.arrival);
          geofence.setDeparture(location.departure);
          locationDao.insert(geofence);
        }
        for (Tag tag : backup.tags) {
          tag.setTask(taskId);
          tag.setTaskUid(taskUuid);
          tagDao.insert(tag);
        }
        for (Geofence geofence : backup.getGeofences()) {
          geofence.setTask(taskId);
          locationDao.insert(geofence);
        }
        for (TaskAttachment attachment : backup.getAttachments()) {
          attachment.setTaskId(taskUuid);
          if (version < 546) {
            attachment.convertPathUri();
          }
          taskAttachmentDao.insert(attachment);
        }
        for (CaldavTask caldavTask : backup.getCaldavTasks()) {
          caldavTask.setTask(taskId);
          caldavDao.insert(caldavTask);
        }
        importCount++;
      }
      for (Entry<String, Integer> entry : backupContainer.getIntPrefs().entrySet()) {
        preferences.setInt(entry.getKey(), entry.getValue());
      }
      for (Entry<String, Long> entry : backupContainer.getLongPrefs().entrySet()) {
        preferences.setLong(entry.getKey(), entry.getValue());
      }
      for (Entry<String, String> entry : backupContainer.getStringPrefs().entrySet()) {
        preferences.setString(entry.getKey(), entry.getValue());
      }
      for (Entry<String, Boolean> entry : backupContainer.getBoolPrefs().entrySet()) {
        preferences.setBoolean(entry.getKey(), entry.getValue());
      }
      reader.close();
      is.close();
    } catch (IOException e) {
      Timber.e(e);
    } finally {
      localBroadcastManager.broadcastRefresh();
      handler.post(
          () -> {
            if (progressDialog.isShowing()) {
              DialogUtilities.dismissDialog(activity, progressDialog);
              showSummary();
            }
          });
    }
  }

  private void showSummary() {
    Resources r = activity.getResources();
    dialogBuilder
        .newDialog()
        .setTitle(R.string.import_summary_title)
        .setMessage(
            activity.getString(
                R.string.import_summary_message,
                "",
                r.getQuantityString(R.plurals.Ntasks, taskCount, taskCount),
                r.getQuantityString(R.plurals.Ntasks, importCount, importCount),
                r.getQuantityString(R.plurals.Ntasks, skipCount, skipCount),
                r.getQuantityString(R.plurals.Ntasks, 0, 0)))
        .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss())
        .show();
  }

  static class LegacyLocation {
    String name;
    String address;
    String phone;
    String url;
    double latitude;
    double longitude;
    int radius;
    boolean arrival;
    boolean departure;
  }
}
