package org.tasks.gtasks;

import static com.google.common.collect.Lists.transform;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.date.DateTimeUtils.newDateTime;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.EOFException;
import java.io.IOException;
import java.net.HttpRetryException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.net.ssl.SSLException;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Firebase;
import org.tasks.billing.Inventory;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.injection.ApplicationContext;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;
import timber.log.Timber;

public class GoogleTaskSynchronizer {

  private static final String DEFAULT_LIST = "@default"; // $NON-NLS-1$
  private static final int MAX_TITLE_LENGTH = 1024;
  private static final int MAX_DESCRIPTION_LENGTH = 8192;

  private static final Comparator<com.google.api.services.tasks.model.Task> PARENTS_FIRST =
      (o1, o2) -> {
        if (isNullOrEmpty(o1.getParent())) {
          return isNullOrEmpty(o2.getParent()) ? 0 : -1;
        } else {
          return isNullOrEmpty(o2.getParent()) ? 1 : 0;
        }
      };

  private final Context context;
  private final GoogleTaskListDao googleTaskListDao;
  private final GtasksListService gtasksListService;
  private final Preferences preferences;
  private final TaskDao taskDao;
  private final Firebase firebase;
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
      @ApplicationContext Context context,
      GoogleTaskListDao googleTaskListDao,
      GtasksListService gtasksListService,
      Preferences preferences,
      TaskDao taskDao,
      Firebase firebase,
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
    this.firebase = firebase;
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

  static void mergeDates(long remoteDueDate, Task local) {
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

  public void sync(GoogleTaskAccount account, int i) {
    Timber.d("%s: start sync", account);
    try {
      if (i == 0 || inventory.hasPro()) {
        synchronize(account);
      } else {
        account.setError(context.getString(R.string.requires_pro_subscription));
      }
    } catch (SocketTimeoutException
        | SSLException
        | SocketException
        | UnknownHostException
        | HttpRetryException
        | EOFException e) {
      Timber.e(e);
      account.setError(e.getMessage());
    } catch (GoogleJsonResponseException e) {
      account.setError(e.getMessage());
      if (e.getStatusCode() == 401) {
        Timber.e(e);
      } else {
        firebase.reportException(e);
      }
    } catch (Exception e) {
      account.setError(e.getMessage());
      firebase.reportException(e);
    } finally {
      googleTaskListDao.update(account);
      localBroadcastManager.broadcastRefreshList();
      Timber.d("%s: end sync", account);
    }
  }

  private void synchronize(GoogleTaskAccount account) throws IOException {
    if (!permissionChecker.canAccessAccounts()
        || googleAccountManager.getAccount(account.getAccount()) == null) {
      account.setError(context.getString(R.string.cannot_access_account));
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
    } while (!isNullOrEmpty(nextPageToken));
    gtasksListService.updateLists(account, gtaskLists);
    Filter defaultRemoteList = defaultFilterProvider.getDefaultList();
    if (defaultRemoteList instanceof GtasksFilter) {
      GoogleTaskList list =
          gtasksListService.getList(((GtasksFilter) defaultRemoteList).getRemoteId());
      if (list == null) {
        preferences.setString(R.string.p_default_list, null);
      }
    }
    for (GoogleTaskList list :
        googleTaskListDao.getByRemoteId(transform(gtaskLists, TaskList::getId))) {
      if (isNullOrEmpty(list.getRemoteId())) {
        firebase.reportException(new RuntimeException("Empty remote id"));
        continue;
      }
      fetchAndApplyRemoteChanges(gtasksInvoker, list);
      if (!preferences.isPositionHackEnabled()) {
        googleTaskDao.reposition(list.getRemoteId());
      }
    }
    if (preferences.isPositionHackEnabled()) {
      for (TaskList list : gtaskLists) {
        List<com.google.api.services.tasks.model.Task> tasks =
            fetchPositions(gtasksInvoker, list.getId());
        for (com.google.api.services.tasks.model.Task task : tasks) {
          googleTaskDao.updatePosition(task.getId(), task.getParent(), task.getPosition());
        }
        googleTaskDao.reposition(list.getId());
      }
    }
    account.setEtag(eTag);
    account.setError("");
  }

  private List<com.google.api.services.tasks.model.Task> fetchPositions(
      GtasksInvoker gtasksInvoker, String listId) throws IOException {
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
    } while (!isNullOrEmpty(nextPageToken));
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

    com.google.api.services.tasks.model.Task remoteModel =
        new com.google.api.services.tasks.model.Task();
    boolean newlyCreated = false;

    String remoteId;
    Filter defaultRemoteList = defaultFilterProvider.getDefaultList();
    String listId =
        defaultRemoteList instanceof GtasksFilter
            ? ((GtasksFilter) defaultRemoteList).getRemoteId()
            : DEFAULT_LIST;

    if (isNullOrEmpty(gtasksMetadata.getRemoteId())) { // Create case
      String selectedList = gtasksMetadata.getListId();
      if (!isNullOrEmpty(selectedList)) {
        listId = selectedList;
      }
      newlyCreated = true;
    } else { // update case
      remoteId = gtasksMetadata.getRemoteId();
      listId = gtasksMetadata.getListId();
      remoteModel.setId(remoteId);
    }

    // If task was newly created but without a title, don't sync--we're in the middle of
    // creating a task which may end up being cancelled. Also don't sync new but already
    // deleted tasks
    if (newlyCreated && (isNullOrEmpty(task.getTitle()) || task.getDeletionDate() > 0)) {
      return;
    }

    // Update the remote model's changed properties
    if (task.isDeleted()) {
      remoteModel.setDeleted(true);
    }

    remoteModel.setTitle(truncate(task.getTitle(), MAX_TITLE_LENGTH));
    remoteModel.setNotes(truncate(task.getNotes(), MAX_DESCRIPTION_LENGTH));
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
      long parent = gtasksMetadata.getParent();
      String localParent = parent > 0 ? googleTaskDao.getRemoteId(parent) : null;
      String previous =
          googleTaskDao.getPrevious(
              listId, isNullOrEmpty(localParent) ? 0 : parent, gtasksMetadata.getOrder());

      com.google.api.services.tasks.model.Task created;
      try {
        created = gtasksInvoker.createGtask(listId, remoteModel, localParent, previous);
      } catch (HttpNotFoundException e) {
        created = gtasksInvoker.createGtask(listId, remoteModel, null, null);
      }

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
            long parent = gtasksMetadata.getParent();
            String localParent = parent > 0 ? googleTaskDao.getRemoteId(parent) : null;
            String previous =
                googleTaskDao.getPrevious(
                    listId,
                    isNullOrEmpty(localParent) ? 0 : parent,
                    gtasksMetadata.getOrder());

            com.google.api.services.tasks.model.Task result =
                gtasksInvoker.moveGtask(listId, remoteModel.getId(), localParent, previous);
            gtasksMetadata.setRemoteOrder(Long.parseLong(result.getPosition()));
            gtasksMetadata.setRemoteParent(result.getParent());
            gtasksMetadata.setParent(
                isNullOrEmpty(result.getParent())
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
        // TODO: don't updateGtask if it was only moved
        gtasksInvoker.updateGtask(listId, remoteModel);
      } catch (HttpNotFoundException e) {
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
    task.suppressSync();
    taskDao.save(task);
  }

  private synchronized void fetchAndApplyRemoteChanges(
      GtasksInvoker gtasksInvoker, GoogleTaskList list) throws IOException {
    String listId = list.getRemoteId();
    long lastSyncDate = list.getLastSync();
    List<com.google.api.services.tasks.model.Task> tasks = new ArrayList<>();
    String nextPageToken = null;
    do {
      Tasks taskList;
      try {
        taskList =
            gtasksInvoker.getAllGtasksFromListId(listId, lastSyncDate + 1000L, nextPageToken);
      } catch (HttpNotFoundException e) {
        firebase.reportException(e);
        return;
      }
      if (taskList == null) {
        break;
      }
      List<com.google.api.services.tasks.model.Task> items = taskList.getItems();
      if (items != null) {
        tasks.addAll(items);
      }
      nextPageToken = taskList.getNextPageToken();
    } while (!isNullOrEmpty(nextPageToken));

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
      if (isDeleted != null && isDeleted) {
        if (task != null) {
          taskDeleter.delete(task);
        }
        continue;
      } else if (isHidden != null && isHidden) {
        if (task == null) {
          continue;
        }
        if (task.isRecurring()) {
          googleTask.setRemoteId("");
        } else {
          taskDeleter.delete(task);
          continue;
        }
      } else {
        googleTask.setRemoteOrder(Long.parseLong(gtask.getPosition()));
        googleTask.setRemoteParent(gtask.getParent());
        googleTask.setParent(
            isNullOrEmpty(gtask.getParent())
                ? 0
                : googleTaskDao.getTask(gtask.getParent()));
        googleTask.setRemoteId(gtask.getId());
      }

      if (task == null) {
        task = taskCreator.createWithValues("");
      }

      task.setTitle(getTruncatedValue(task.getTitle(), gtask.getTitle(), MAX_TITLE_LENGTH));
      task.setCreationDate(DateUtilities.now());
      task.setCompletionDate(
          GtasksApiUtilities.gtasksCompletedTimeToUnixTime(gtask.getCompleted()));
      long dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(gtask.getDue());
      mergeDates(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate), task);
      task.setNotes(getTruncatedValue(task.getNotes(), gtask.getNotes(), MAX_DESCRIPTION_LENGTH));
      googleTask.setListId(listId);
      googleTask.setLastSync(DateUtilities.now() + 1000L);
      write(task, googleTask);
    }
    list.setLastSync(lastSyncDate);
    googleTaskListDao.insertOrReplace(list);
  }

  static String truncate(@Nullable String string, int max) {
    return string == null || string.length() <= max ? string : string.substring(0, max);
  }

  static String getTruncatedValue(@Nullable String currentValue, @Nullable String newValue, int maxLength) {
    return isNullOrEmpty(newValue)
            || newValue.length() < maxLength
            || isNullOrEmpty(currentValue)
            || !currentValue.startsWith(newValue)
        ? newValue
        : currentValue;
  }

  private void write(Task task, GoogleTask googleTask) {
    if (!(isNullOrEmpty(task.getTitle()) && isNullOrEmpty(task.getNotes()))) {
      task.suppressSync();
      task.suppressRefresh();
      if (task.isNew()) {
        taskDao.createNew(task);
      }
      taskDao.save(task);
      googleTask.setTask(task.getId());
      if (googleTask.getId() == 0) {
        googleTaskDao.insert(googleTask);
      } else {
        googleTaskDao.update(googleTask);
      }
    }
  }
}
