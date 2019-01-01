package org.tasks.gtasks;

import static org.tasks.date.DateTimeUtils.newDateTime;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;
import com.google.gson.JsonSyntaxException;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.api.HttpNotFoundException;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.gtasks.sync.GtasksTaskContainer;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.utility.Constants;
import java.io.IOException;
import java.util.ArrayList;
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
import org.tasks.data.TagDao;
import org.tasks.data.Tag;
import org.tasks.data.TagData;
import com.todoroo.astrid.tags.TagService;
import org.tasks.injection.ForApplication;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.time.DateTime;
import timber.log.Timber;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GoogleTaskSynchronizer {

  private static final String DEFAULT_LIST = "@default"; // $NON-NLS-1$
  private static final String NOTE_METADATA_KEYWORD = "tasks:additionalMetadata";
  private static final Pattern PATTERN_NOTES_METADATA = Pattern.compile("(.*)\\{" + NOTE_METADATA_KEYWORD + "(\\{.*\\})\\}\\s*", Pattern.DOTALL);
  public static final String LINE_FEED = "\n";
  private static final int NOTE_MAX_LENGTH = 8100; // seams to be 8192
  private Gson gsonBulider = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();


  private final Context context;
  private final GoogleTaskListDao googleTaskListDao;
  private final GtasksSyncService gtasksSyncService;
  private final GtasksListService gtasksListService;
  private final GtasksTaskListUpdater gtasksTaskListUpdater;
  private final Preferences preferences;
  private final GtaskSyncAdapterHelper gtasksSyncAdapterHelper;
  private final TaskDao taskDao;
  private final TagDao tagDao;
  private final TagService tagService;
  private final Tracker tracker;
  private final NotificationManager notificationManager;
  private final GoogleTaskDao googleTaskDao;
  private final TaskCreator taskCreator;
  private final DefaultFilterProvider defaultFilterProvider;
  private final PlayServices playServices;
  private final PermissionChecker permissionChecker;
  private final GoogleAccountManager googleAccountManager;
  private final LocalBroadcastManager localBroadcastManager;
  private final Inventory inventory;
  private final TaskDeleter taskDeleter;

  @Inject
  public GoogleTaskSynchronizer(
      @ForApplication Context context,
      GoogleTaskListDao googleTaskListDao,
      GtasksSyncService gtasksSyncService,
      GtasksListService gtasksListService,
      GtasksTaskListUpdater gtasksTaskListUpdater,
      Preferences preferences,
      GtaskSyncAdapterHelper gtasksSyncAdapterHelper,
      TaskDao taskDao,
      TagDao tagDao,
      TagService tagService,
      Tracker tracker,
      NotificationManager notificationManager,
      GoogleTaskDao googleTaskDao,
      TaskCreator taskCreator,
      DefaultFilterProvider defaultFilterProvider,
      PlayServices playServices,
      PermissionChecker permissionChecker,
      GoogleAccountManager googleAccountManager,
      LocalBroadcastManager localBroadcastManager,
      Inventory inventory,
      TaskDeleter taskDeleter) {
    this.context = context;
    this.googleTaskListDao = googleTaskListDao;
    this.gtasksSyncService = gtasksSyncService;
    this.gtasksListService = gtasksListService;
    this.gtasksTaskListUpdater = gtasksTaskListUpdater;
    this.preferences = preferences;
    this.gtasksSyncAdapterHelper = gtasksSyncAdapterHelper;
    this.taskDao = taskDao;
    this.tagDao = tagDao;
    this.tagService = tagService;
    this.tracker = tracker;
    this.notificationManager = notificationManager;
    this.googleTaskDao = googleTaskDao;
    this.taskCreator = taskCreator;
    this.defaultFilterProvider = defaultFilterProvider;
    this.playServices = playServices;
    this.permissionChecker = permissionChecker;
    this.googleAccountManager = googleAccountManager;
    this.localBroadcastManager = localBroadcastManager;
    this.inventory = inventory;
    this.taskDeleter = taskDeleter;
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
        } else {
          account.setError(context.getString(R.string.requires_pro_subscription));
        }
      } catch (UserRecoverableAuthIOException e) {
        Timber.e(e);
        sendNotification(context, e.getIntent());
      } catch (IOException e) {
        account.setError(e.getMessage());
        Timber.e(e);
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
        new NotificationCompat.Builder(context, NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
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

    GtasksInvoker gtasksInvoker = new GtasksInvoker(context, playServices, account.getAccount());
    pushLocalChanges(account, gtasksInvoker);

    List<TaskList> gtaskLists = new ArrayList<>();
    String nextPageToken = null;
    do {
      TaskLists remoteLists = gtasksInvoker.allGtaskLists(nextPageToken);
      if (remoteLists == null) {
        break;
      }
      List<TaskList> items = remoteLists.getItems();
      if (items != null) {
        gtaskLists.addAll(items);
      }
      nextPageToken = remoteLists.getNextPageToken();
    } while (nextPageToken != null);
    gtasksListService.updateLists(account, gtaskLists);
    Filter defaultRemoteList = defaultFilterProvider.getDefaultRemoteList();
    if (defaultRemoteList instanceof GtasksFilter) {
      GoogleTaskList list =
          gtasksListService.getList(((GtasksFilter) defaultRemoteList).getRemoteId());
      if (list == null) {
        preferences.setString(R.string.p_default_remote_list, null);
      }
    }
    for (final GoogleTaskList list : gtasksListService.getListsToUpdate(gtaskLists)) {
      fetchAndApplyRemoteChanges(gtasksInvoker, list);
    }
    account.setError("");
  }

  private void pushLocalChanges(GoogleTaskAccount account, GtasksInvoker gtasksInvoker)
      throws UserRecoverableAuthIOException {
    List<Task> tasks = taskDao.getGoogleTasksToPush(account.getAccount());
    for (Task task : tasks) {
      try {
        pushTask(task, gtasksInvoker);
      } catch (UserRecoverableAuthIOException e) {
        throw e;
      } catch (IOException e) {
        Timber.e(e);
      }
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
    writeNotesIfNecessary(task, remoteModel);
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

    if (!newlyCreated) {
      try {
        gtasksInvoker.updateGtask(listId, remoteModel);
      } catch (HttpNotFoundException e) {
        Timber.e(e);
        googleTaskDao.delete(gtasksMetadata);
        return;
      }
    } else {
      String parent = gtasksSyncService.getRemoteParentId(gtasksMetadata);
      String priorSibling = gtasksSyncService.getRemoteSiblingId(listId, gtasksMetadata);

      com.google.api.services.tasks.model.Task created =
          gtasksInvoker.createGtask(listId, remoteModel, parent, priorSibling);

      if (created != null) {
        // Update the metadata for the newly created task
        gtasksMetadata.setRemoteId(created.getId());
        gtasksMetadata.setListId(listId);
      } else {
        return;
      }
    }

    task.setModificationDate(DateUtilities.now());
    gtasksMetadata.setLastSync(DateUtilities.now() + 1000L);
    if (gtasksMetadata.getId() == Task.NO_ID) {
      googleTaskDao.insert(gtasksMetadata);
    } else {
      googleTaskDao.update(gtasksMetadata);
    }
    task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
    taskDao.save(task);
  }

  void writeNotesIfNecessary(Task task, com.google.api.services.tasks.model.Task remoteModel) {
    String additionalMetadata = null;
    if (gtasksSyncAdapterHelper.getUseNoteForMetadataSync()) {
      GoogleTaskAdditionalMetadata additionalMetadataObject = new GoogleTaskAdditionalMetadata();
      List<String> tags = createTagList(task);
      if (tags.size() > 0) {
        additionalMetadataObject.setTags(tags);
      }
      int defaultPriority = preferences.getIntegerFromString(R.string.p_default_importance_key, Task.Priority.LOW);
      if (task.getPriority() != defaultPriority) {
        additionalMetadataObject.setImportance(GoogleTaskAdditionalMetadata.Importance.valueOf(task.getPriority()));
      }
      if (task.getHideUntil()>0) {
        additionalMetadataObject.setHideUntil(GtasksApiUtilities.unixTimeToDateTime(task.getHideUntil()));
      }
      int defaultReminderFlags = preferences.getIntegerFromString(R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
      if ((defaultReminderFlags & Task.NOTIFY_AT_DEADLINE) != (task.getReminderFlags() & Task.NOTIFY_AT_DEADLINE)) {
        additionalMetadataObject.setNotifyAtDeadline(task.isNotifyAtDeadline());
      }
      if ((defaultReminderFlags & Task.NOTIFY_AFTER_DEADLINE) != (task.getReminderFlags() & Task.NOTIFY_AFTER_DEADLINE)) {
        additionalMetadataObject.setNotifyAfterDeadline(task.isNotifyAfterDeadline());
      }
      if (task.isNotifyModeNonstop()) {
        additionalMetadataObject.setNotifyModeNonstop(task.isNotifyModeNonstop());
      }
      if (task.isNotifyModeFive()) {
        additionalMetadataObject.setNotifyModeFive(task.isNotifyModeFive());
      }
      if (task.getRecurrence()!=null && task.getRecurrence().trim().length()>0) {
        additionalMetadataObject.setRecurrence(task.getRecurrence());
        if (task.getRepeatUntil() > 0) {
          additionalMetadataObject.setRepeatUntil(GtasksApiUtilities.unixTimeToDateTime(task.getRepeatUntil()));
        }
      }
      additionalMetadata = gsonBulider.toJson(additionalMetadataObject);
      Timber.i("Additional metadata is: " + additionalMetadata);
    } else {
      Timber.w("No synchronication of additional metadata with notes.");
    }
    if (additionalMetadata==null || additionalMetadata.trim().length()<=2) {
       remoteModel.setNotes(task.getNotes());
    } else {
      StringBuilder notes = new StringBuilder();
      if (task.getNotes()!=null && task.getNotes().trim().length()>0) {
        notes.append(task.getNotes());
      }
      if (notes.length() + additionalMetadata.length() < NOTE_MAX_LENGTH) {
        notes.append(LINE_FEED);
        notes.append(LINE_FEED);
        notes.append("{");
        notes.append(NOTE_METADATA_KEYWORD);
        notes.append(additionalMetadata);
        notes.append("}");
      }
      remoteModel.setNotes(notes.toString());
    }
  }

  private List<String> createTagList(Task task) {
    Set<String> tags = new TreeSet<>();
    List<TagData> tagDatas = tagService.getTagDataForTask(task.getId());
    for(TagData tagData: tagDatas) {
      String tagName = tagData.getName();
      Timber.i("Tag '" + tagName + "' found.");
      // Skip empty tags or tags containig { } , ; [ ] # ' \
      if (tagName!=null && tagName.trim().length()>0 && tagName.indexOf('{')<0 && tagName.indexOf('}')<0 && tagName.indexOf('[')<0 && tagName.indexOf(']')<0 && tagName.indexOf(',')<0 && tagName.indexOf(';')<0 && tagName.indexOf('"')<0 && tagName.indexOf('\'')<0 && tagName.indexOf('\\')<0) {
        tags.add(tagName.trim());
      } else {
        Timber.w("Tag '" + tagName + "' skipped.");
      }
    }
    return new ArrayList<>(tags);
  }

  void processNotes(Task task) {
    String notes = task.getNotes();
    if (notes != null) {
      Matcher m = PATTERN_NOTES_METADATA.matcher(notes);
      if (m.matches()) {
        notes = m.group(1).trim();
        String gson = m.group(2);
        try {
          GoogleTaskAdditionalMetadata additionalMetadataObject = gsonBulider.fromJson(gson, GoogleTaskAdditionalMetadata.class);
          List<String> tags = additionalMetadataObject.getTags();
          if (tags != null && tags.size() > 0) {
            for (String tag : tags) {
              createLink(task, tag);
            }
          }
          if (additionalMetadataObject.getHideUntil() != null) {
            task.setHideUntil(GtasksApiUtilities.dateTimeToUnixTime(additionalMetadataObject.getHideUntil()));
          }
          if (additionalMetadataObject.getRecurrence() != null) {
            task.setRecurrence(additionalMetadataObject.getRecurrence());
          }
          if (additionalMetadataObject.getRepeatUntil() != null) {
            task.setRepeatUntil(GtasksApiUtilities.dateTimeToUnixTime(additionalMetadataObject.getRepeatUntil()));
          }
          int defaultImportance = preferences.getIntegerFromString(R.string.p_default_importance_key, Task.Priority.LOW);
          if (additionalMetadataObject.getImportance() != null && additionalMetadataObject.getImportance().getTaskImportance() != defaultImportance) {
            task.setPriority(additionalMetadataObject.getImportance().getTaskImportance());
          }
          int defaultReminderFlags = preferences.getIntegerFromString(R.string.p_default_reminders_key, Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE);
          int reminderFlags = defaultReminderFlags;
          boolean setRemindeFlags = false;
          if (isSetAndNotDefault(additionalMetadataObject.isNotifyAtDeadline(), Task.NOTIFY_AT_DEADLINE, defaultReminderFlags)) {
            setRemindeFlags = true;
            if (additionalMetadataObject.isNotifyAtDeadline()) {
              reminderFlags |= Task.NOTIFY_AT_DEADLINE;
            } else {
              reminderFlags &= ~Task.NOTIFY_AT_DEADLINE;
            }
          }
          if (isSetAndNotDefault(additionalMetadataObject.isNotifyAfterDeadline(), Task.NOTIFY_AFTER_DEADLINE, defaultReminderFlags)) {
            setRemindeFlags = true;
            if (additionalMetadataObject.isNotifyAfterDeadline()) {
              reminderFlags |= Task.NOTIFY_AFTER_DEADLINE;
            } else {
              reminderFlags &= ~Task.NOTIFY_AFTER_DEADLINE;
            }
          }
          if (additionalMetadataObject.isNotifyModeFive() != null) {
            setRemindeFlags = true;
            reminderFlags |= Task.NOTIFY_MODE_FIVE;
          }
          if (additionalMetadataObject.isNotifyModeNonstop() != null) {
            setRemindeFlags = true;
            reminderFlags |= Task.NOTIFY_MODE_NONSTOP;
          }
          if (setRemindeFlags) {
            task.setReminderFlags(reminderFlags);
          }
        } catch (JsonSyntaxException ex) {
          // Ignore corrupt JSON
        }
      }
    }
    task.setNotes(notes);
  }

  private boolean isSetAndNotDefault(Boolean reminderFlag, int reminderFlagConstant, int defaultReminderFlags) {
    return reminderFlag!=null &&
            (
                    (reminderFlag && (defaultReminderFlags & reminderFlagConstant)==0) ||
                            (!reminderFlag && (defaultReminderFlags & reminderFlagConstant)!=0)
            );
  }

  private void createLink(Task task, String tagName) {
    TagData tagData = tagService.getOrCreateTag(tagName);
    if (tagData.getRemoteId() == null || tagDao.getTagByTaskAndTagUid(task.getId(), tagData.getRemoteId()) == null) {
      Tag link = new Tag(task.getId(), task.getUuid(), tagData.getName(), tagData.getRemoteId());
      tagDao.insert(link);
    }
  }

  private synchronized void fetchAndApplyRemoteChanges(
      GtasksInvoker gtasksInvoker, GoogleTaskList list) throws UserRecoverableAuthIOException {
    String listId = list.getRemoteId();
    long lastSyncDate = list.getLastSync();

    boolean includeDeletedAndHidden = lastSyncDate != 0;
    try {
      List<com.google.api.services.tasks.model.Task> tasks = new ArrayList<>();
      String nextPageToken = null;
      do {
        Tasks taskList =
            gtasksInvoker.getAllGtasksFromListId(
                listId,
                includeDeletedAndHidden,
                includeDeletedAndHidden,
                lastSyncDate + 1000L,
                nextPageToken);
        if (taskList == null) {
          break;
        }
        List<com.google.api.services.tasks.model.Task> items = taskList.getItems();
        if (items != null) {
          tasks.addAll(items);
        }
        nextPageToken = taskList.getNextPageToken();
      } while (nextPageToken != null);

      if (!tasks.isEmpty()) {
        for (com.google.api.services.tasks.model.Task gtask : tasks) {
          String remoteId = gtask.getId();
          GoogleTask googleTask = getMetadataByGtaskId(remoteId);
          Task task = null;
          if (googleTask == null) {
            googleTask = new GoogleTask(0, "");
          } else if (googleTask.getTask() > 0) {
            task = taskDao.fetch(googleTask.getTask());
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
            task = taskCreator.createWithValues(null, "");
          }
          GtasksTaskContainer container = new GtasksTaskContainer(gtask, task, listId, googleTask);
          container.gtaskMetadata.setRemoteOrder(Long.parseLong(gtask.getPosition()));
          container.gtaskMetadata.setParent(localIdForGtasksId(gtask.getParent()));
          container.gtaskMetadata.setLastSync(DateUtilities.now() + 1000L);
          write(container);
          lastSyncDate = Math.max(lastSyncDate, container.getUpdateTime());
        }
        list.setLastSync(lastSyncDate);
        googleTaskListDao.insertOrReplace(list);
        gtasksTaskListUpdater.correctOrderAndIndentForList(listId);
      }
    } catch (UserRecoverableAuthIOException e) {
      throw e;
    } catch (IOException e) {
      Timber.e(e);
    }
  }

  private long localIdForGtasksId(String gtasksId) {
    GoogleTask metadata = getMetadataByGtaskId(gtasksId);
    return metadata == null ? Task.NO_ID : metadata.getTask();
  }

  private GoogleTask getMetadataByGtaskId(String gtaskId) {
    return googleTaskDao.getByRemoteId(gtaskId);
  }

  private void write(GtasksTaskContainer task) {
    if (!TextUtils.isEmpty(task.task.getTitle())) {
      task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
      task.task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
      task.prepareForSaving();
      if (task.task.isNew()) {
        taskDao.createNew(task.task);
      }
      processNotes(task.getTask());
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
