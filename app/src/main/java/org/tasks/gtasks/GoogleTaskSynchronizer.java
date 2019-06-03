package org.tasks.gtasks;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.utility.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.billing.Inventory;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.ForApplication;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;
import timber.log.Timber;

public class GoogleTaskSynchronizer {

  private static final String DEFAULT_LIST = "@default"; // $NON-NLS-1$

  private static final Comparator<com.google.api.services.tasks.model.Task> PARENTS_FIRST =
      (o1, o2) -> {
        if (Strings.isNullOrEmpty(o1.getParent())) {
          return Strings.isNullOrEmpty(o2.getParent()) ? 0 : -1;
        } else {
          return Strings.isNullOrEmpty(o2.getParent()) ? 1 : 0;
        }
      };

  private final Context context;
  private final GoogleTaskListDao googleTaskListDao;
  private final GtasksListService gtasksListService;
  private final Preferences preferences;
  private final TaskDao taskDao;
  private final Tracker tracker;
  private final NotificationManager notificationManager;
  private final GoogleTaskDao googleTaskDao;
  private final TaskCreator taskCreator;
  private final DefaultFilterProvider defaultFilterProvider;
  private final PermissionChecker permissionChecker;
  private final GoogleAccountManager googleAccountManager;
  private final LocalBroadcastManager localBroadcastManager;
  private final Inventory inventory;
  private final TaskDeleter taskDeleter;
  private final GtasksInvoker gtasksInvoker;

  @Inject
  public GoogleTaskSynchronizer(
      @ForApplication Context context,
      GoogleTaskListDao googleTaskListDao,
      GtasksListService gtasksListService,
      Preferences preferences,
      TaskDao taskDao,
      Tracker tracker,
      NotificationManager notificationManager,
      GoogleTaskDao googleTaskDao,
      TaskCreator taskCreator,
      DefaultFilterProvider defaultFilterProvider,
      PermissionChecker permissionChecker,
      GoogleAccountManager googleAccountManager,
      LocalBroadcastManager localBroadcastManager,
      Inventory inventory,
      TaskDeleter taskDeleter,
      GtasksInvoker gtasksInvoker) {
    this.context = context;
    this.googleTaskListDao = googleTaskListDao;
    this.gtasksListService = gtasksListService;
    this.preferences = preferences;
    this.taskDao = taskDao;
    this.tracker = tracker;
    this.notificationManager = notificationManager;
    this.googleTaskDao = googleTaskDao;
    this.taskCreator = taskCreator;
    this.defaultFilterProvider = defaultFilterProvider;
    this.permissionChecker = permissionChecker;
    this.googleAccountManager = googleAccountManager;
    this.localBroadcastManager = localBroadcastManager;
    this.inventory = inventory;
    this.taskDeleter = taskDeleter;
    this.gtasksInvoker = gtasksInvoker;
  }

  public static void mergeDates(long remoteDueDate, Task local) {
    if (remoteDueDate > 0 && local.hasDueTime()) {
      DateTime oldDate = newDateTime(local.getDueDate());
      DateTime newDate =
          newDateTime(remoteDueDate)
              .withHourOfDay(oldDate.getHourOfDay())
              .withMinuteOfHour(oldDate.getMinuteOfHour())
              .withSecondOfMinute(oldDate.getSecondOfMinute());
      local.setDueDateAdjustingHideUntil(
          Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, newDate.getMillis()));
    } else {
      local.setDueDateAdjustingHideUntil(remoteDueDate);
    }
  }

  public void sync() {
    List<GoogleTaskAccount> accounts = googleTaskListDao.getAccounts();
    for (int i = 0; i < accounts.size(); i++) {
      GoogleTaskAccount account = accounts.get(i);
      Timber.d("%s: start sync", account);
      try {
        if (i == 0 || inventory.hasPro()) {
          synchronize(account);
          account.setError("");
        } else {
          account.setError(context.getString(R.string.requires_pro_subscription));
        }
      } catch (UserRecoverableAuthIOException e) {
        Timber.e(e);
        sendNotification(context, e.getIntent());
      } catch (Exception e) {
        account.setError(e.getMessage());
        tracker.reportException(e);
      } finally {
        googleTaskListDao.update(account);
        localBroadcastManager.broadcastRefreshList();
        Timber.d("%s: end sync", account);
      }
    }
  }

  private void sendNotification(Context context, Intent intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);

    PendingIntent resolve =
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(
                context, NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
            .setAutoCancel(true)
            .setContentIntent(resolve)
            .setContentTitle(context.getString(R.string.sync_error_permissions))
            .setContentText(
                context.getString(R.string.common_google_play_services_notification_ticker))
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_warning_white_24dp)
            .setTicker(context.getString(R.string.common_google_play_services_notification_ticker));
    notificationManager.notify(Constants.NOTIFICATION_SYNC_ERROR, builder, true, false, false);
  }

  private void synchronize(GoogleTaskAccount account) throws IOException {
    if (!permissionChecker.canAccessAccounts()
        || googleAccountManager.getAccount(account.getAccount()) == null) {
      account.setError(context.getString(R.string.cannot_access_account));
      googleTaskListDao.update(account);
      localBroadcastManager.broadcastRefreshList();
      return;
    }

    GtasksInvoker gtasksInvoker = this.gtasksInvoker.forAccount(account.getAccount());
    pushLocalChanges(account, gtasksInvoker);

    List<TaskList> gtaskLists = new ArrayList<>();
    String nextPageToken = null;
    String eTag = null;
    do {
      TaskLists remoteLists = gtasksInvoker.allGtaskLists(nextPageToken);
      if (remoteLists == null) {
        break;
      }
      eTag = remoteLists.getEtag();
      List<TaskList> items = remoteLists.getItems();
      if (items != null) {
        gtaskLists.addAll(items);
      }
      nextPageToken = remoteLists.getNextPageToken();
    } while (!Strings.isNullOrEmpty(nextPageToken));
    gtasksListService.updateLists(account, gtaskLists);
    Filter defaultRemoteList = defaultFilterProvider.getDefaultRemoteList();
    if (defaultRemoteList instanceof GtasksFilter) {
      GoogleTaskList list =
          gtasksListService.getList(((GtasksFilter) defaultRemoteList).getRemoteId());
      if (list == null) {
        preferences.setString(R.string.p_default_remote_list, null);
      }
    }
    for (GoogleTaskList list : gtasksListService.getListsToUpdate(gtaskLists)) {
      fetchAndApplyRemoteChanges(gtasksInvoker, list);
      if (!preferences.isPositionHackEnabled()) {
        googleTaskDao.reposition(list.getRemoteId());
      }
    }
    if (preferences.isPositionHackEnabled()) {
      for (TaskList list : gtaskLists) {
        List<com.google.api.services.tasks.model.Task> tasks = fetchPositions(gtasksInvoker, list.getId());
        for (com.google.api.services.tasks.model.Task task : tasks) {
          googleTaskDao.updatePosition(task.getId(), task.getParent(), task.getPosition());
        }
        googleTaskDao.reposition(list.getId());
      }
    }
    account.setEtag(eTag);
  }

  private List<com.google.api.services.tasks.model.Task> fetchPositions(GtasksInvoker gtasksInvoker, String listId)
      throws IOException {
    List<com.google.api.services.tasks.model.Task> tasks = new ArrayList<>();
    String nextPageToken = null;
    do {
      Tasks taskList = gtasksInvoker.getAllPositions(listId, nextPageToken);
      if (taskList == null) {
        break;
      }
      List<com.google.api.services.tasks.model.Task> items = taskList.getItems();
      if (items != null) {
        tasks.addAll(items);
      }
      nextPageToken = taskList.getNextPageToken();
    } while (!Strings.isNullOrEmpty(nextPageToken));
    return tasks;
  }

  private void pushLocalChanges(GoogleTaskAccount account, GtasksInvoker gtasksInvoker)
      throws IOException {
    List<Task> tasks = taskDao.getGoogleTasksToPush(account.getAccount());
    for (Task task : tasks) {
      pushTask(task, gtasksInvoker);
    }
  }

  private void pushTask(Task task, GtasksInvoker gtasksInvoker) throws IOException {
    for (GoogleTask deleted : googleTaskDao.getDeletedByTaskId(task.getId())) {
      gtasksInvoker.deleteGtask(deleted.getListId(), deleted.getRemoteId());
      googleTaskDao.delete(deleted);
    }

    GoogleTask gtasksMetadata = googleTaskDao.getByTaskId(task.getId());

    if (gtasksMetadata == null) {
      return;
    }

    com.google.api.services.tasks.model.Task remoteModel;
    boolean newlyCreated = false;

    String remoteId;
    Filter defaultRemoteList = defaultFilterProvider.getDefaultRemoteList();
    String listId =
        defaultRemoteList instanceof GtasksFilter
            ? ((GtasksFilter) defaultRemoteList).getRemoteId()
            : DEFAULT_LIST;

    if (Strings.isNullOrEmpty(gtasksMetadata.getRemoteId())) { // Create case
      String selectedList = gtasksMetadata.getListId();
      if (!Strings.isNullOrEmpty(selectedList)) {
        listId = selectedList;
      }
      remoteModel = new com.google.api.services.tasks.model.Task();
      newlyCreated = true;
    } else { // update case
      remoteId = gtasksMetadata.getRemoteId();
      listId = gtasksMetadata.getListId();
      remoteModel = new com.google.api.services.tasks.model.Task();
      remoteModel.setId(remoteId);
    }

    // If task was newly created but without a title, don't sync--we're in the middle of
    // creating a task which may end up being cancelled. Also don't sync new but already
    // deleted tasks
    if (newlyCreated && (TextUtils.isEmpty(task.getTitle()) || task.getDeletionDate() > 0)) {
      return;
    }

    // Update the remote model's changed properties
    if (task.isDeleted()) {
      remoteModel.setDeleted(true);
    }

    remoteModel.setTitle(task.getTitle());
    remoteModel.setNotes(task.getNotes());
    if (task.hasDueDate()) {
      remoteModel.setDue(GtasksApiUtilities.unixTimeToGtasksDueDate(task.getDueDate()));
    }
    if (task.isCompleted()) {
      remoteModel.setCompleted(
          GtasksApiUtilities.unixTimeToGtasksCompletionTime(task.getCompletionDate()));
      remoteModel.setStatus("completed"); // $NON-NLS-1$
    } else {
      remoteModel.setCompleted(null);
      remoteModel.setStatus("needsAction"); // $NON-NLS-1$
    }

    if (newlyCreated) {
      String localParent =
          gtasksMetadata.getParent() > 0
              ? googleTaskDao.getRemoteId(gtasksMetadata.getParent())
              : null;

      String previous =
          googleTaskDao.getPrevious(listId, gtasksMetadata.getParent(), gtasksMetadata.getOrder());

      com.google.api.services.tasks.model.Task created =
          gtasksInvoker.createGtask(listId, remoteModel, localParent, previous);

      if (created != null) {
        // Update the metadata for the newly created task
        gtasksMetadata.setRemoteId(created.getId());
        gtasksMetadata.setListId(listId);

        gtasksMetadata.setRemoteOrder(Long.parseLong(created.getPosition()));
        gtasksMetadata.setRemoteParent(created.getParent());
      } else {
        return;
      }
    } else {
      try {
        if (!task.isDeleted() && gtasksMetadata.isMoved()) {
          try {
            String localParent =
                gtasksMetadata.getParent() > 0
                    ? googleTaskDao.getRemoteId(gtasksMetadata.getParent())
                    : null;
            String previous =
                googleTaskDao.getPrevious(
                    listId, gtasksMetadata.getParent(), gtasksMetadata.getOrder());
            com.google.api.services.tasks.model.Task result =
                gtasksInvoker.moveGtask(listId, remoteModel.getId(), localParent, previous);
            gtasksMetadata.setRemoteOrder(Long.parseLong(result.getPosition()));
            gtasksMetadata.setRemoteParent(result.getParent());
            gtasksMetadata.setParent(
                Strings.isNullOrEmpty(result.getParent())
                    ? 0
                    : googleTaskDao.getTask(result.getParent()));
          } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 400) {
              Timber.e(e);
            } else {
              throw e;
            }
          }
        }
        gtasksInvoker.updateGtask(listId, remoteModel);
      } catch (HttpNotFoundException e) {
        Timber.e(e);
        googleTaskDao.delete(gtasksMetadata);
        return;
      }
    }

    task.setModificationDate(DateUtilities.now());
    gtasksMetadata.setMoved(false);
    gtasksMetadata.setLastSync(DateUtilities.now() + 1000L);
    if (gtasksMetadata.getId() == Task.NO_ID) {
      googleTaskDao.insert(gtasksMetadata);
    } else {
      googleTaskDao.update(gtasksMetadata);
    }
    task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
    taskDao.save(task);
  }

  private synchronized void fetchAndApplyRemoteChanges(
      GtasksInvoker gtasksInvoker, GoogleTaskList list) throws IOException {
    String listId = list.getRemoteId();
    long lastSyncDate = list.getLastSync();
    List<com.google.api.services.tasks.model.Task> tasks = new ArrayList<>();
    String nextPageToken = null;
    do {
      Tasks taskList =
          gtasksInvoker.getAllGtasksFromListId(listId, lastSyncDate + 1000L, nextPageToken);
      if (taskList == null) {
        break;
      }
      List<com.google.api.services.tasks.model.Task> items = taskList.getItems();
      if (items != null) {
        tasks.addAll(items);
      }
      nextPageToken = taskList.getNextPageToken();
    } while (!Strings.isNullOrEmpty(nextPageToken));

    Collections.sort(tasks, PARENTS_FIRST);

    for (com.google.api.services.tasks.model.Task gtask : tasks) {
      String remoteId = gtask.getId();
      GoogleTask googleTask = googleTaskDao.getByRemoteId(remoteId);
      Task task = null;
      if (googleTask == null) {
        googleTask = new GoogleTask(0, "");
      } else if (googleTask.getTask() > 0) {
        task = taskDao.fetch(googleTask.getTask());
      }
      com.google.api.client.util.DateTime updated = gtask.getUpdated();
      if (updated != null) {
        lastSyncDate = Math.max(lastSyncDate, updated.getValue());
      }
      Boolean isDeleted = gtask.getDeleted();
      Boolean isHidden = gtask.getHidden();
      if ((isDeleted != null && isDeleted) || (isHidden != null && isHidden)) {
        if (task != null) {
          taskDeleter.delete(task);
        }
        continue;
      }
      if (task == null) {
        task = taskCreator.createWithValues("");
      }
      GtasksTaskContainer container = new GtasksTaskContainer(gtask, task, listId, googleTask);

      container.gtaskMetadata.setRemoteOrder(Long.parseLong(gtask.getPosition()));
      container.gtaskMetadata.setRemoteParent(gtask.getParent());
      container.gtaskMetadata.setParent(
          Strings.isNullOrEmpty(gtask.getParent()) ? 0 : googleTaskDao.getTask(gtask.getParent()));
      container.gtaskMetadata.setLastSync(DateUtilities.now() + 1000L);
      write(container);
    }
    list.setLastSync(lastSyncDate);
    googleTaskListDao.insertOrReplace(list);
  }

  private void write(GtasksTaskContainer task) {
    if (!TextUtils.isEmpty(task.task.getTitle())) {
      task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
      task.task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
      task.prepareForSaving();
      if (task.task.isNew()) {
        taskDao.createNew(task.task);
      }
      taskDao.save(task.task);
      synchronizeMetadata(task.task.getId(), task.metadata);
    }
  }

  /**
   * Synchronize metadata for given task id. Deletes rows in database that are not identical to
   * those in the metadata list, creates rows that have no match.
   *
   * @param taskId id of task to perform synchronization on
   * @param metadata list of new metadata items to save
   */
  private void synchronizeMetadata(long taskId, ArrayList<GoogleTask> metadata) {
    for (GoogleTask metadatum : metadata) {
      metadatum.setTask(taskId);
      metadatum.setId(0);
    }

    for (GoogleTask item : googleTaskDao.getAllByTaskId(taskId)) {
      long id = item.getId();

      // clear item id when matching with incoming values
      item.setId(0);
      if (metadata.contains(item)) {
        metadata.remove(item);
      } else {
        // not matched. cut it
        item.setId(id);
        googleTaskDao.delete(item);
      }
    }

    // everything that remains shall be written
    for (GoogleTask values : metadata) {
      googleTaskDao.insert(values);
    }
  }
}
