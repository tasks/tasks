package org.tasks.etesync;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.tasks.caldav.CaldavUtils.getParent;

import android.content.Context;
import androidx.core.util.Pair;
import at.bitfire.ical4android.ICalendar;
import com.etesync.journalmanager.Crypto.CryptoManager;
import com.etesync.journalmanager.Exceptions;
import com.etesync.journalmanager.Exceptions.HttpException;
import com.etesync.journalmanager.Exceptions.IntegrityException;
import com.etesync.journalmanager.Exceptions.VersionTooNewException;
import com.etesync.journalmanager.JournalEntryManager;
import com.etesync.journalmanager.JournalEntryManager.Entry;
import com.etesync.journalmanager.JournalManager.Journal;
import com.etesync.journalmanager.model.CollectionInfo;
import com.etesync.journalmanager.model.SyncEntry;
import com.etesync.journalmanager.model.SyncEntry.Actions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.ByteArrayOutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.fortuna.ical4j.model.property.ProdId;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.caldav.CaldavConverter;
import org.tasks.caldav.CaldavUtils;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.CaldavTaskContainer;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class EteSynchronizer {

  static {
    ICalendar.Companion.setProdId(
        new ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN"));
  }

  private final CaldavDao caldavDao;
  private final TaskDao taskDao;
  private final TagDataDao tagDataDao;
  private final TagDao tagDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final TaskCreator taskCreator;
  private final TaskDeleter taskDeleter;
  private final Inventory inventory;
  private final EteSyncClient client;
  private final Context context;

  @Inject
  public EteSynchronizer(
      @ForApplication Context context,
      CaldavDao caldavDao,
      TaskDao taskDao,
      TagDataDao tagDataDao,
      TagDao tagDao,
      LocalBroadcastManager localBroadcastManager,
      TaskCreator taskCreator,
      TaskDeleter taskDeleter,
      Inventory inventory,
      EteSyncClient client) {
    this.context = context;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
    this.tagDataDao = tagDataDao;
    this.tagDao = tagDao;
    this.localBroadcastManager = localBroadcastManager;
    this.taskCreator = taskCreator;
    this.taskDeleter = taskDeleter;
    this.inventory = inventory;
    this.client = client;
  }

  public void sync(CaldavAccount account) {
    if (!inventory.hasPro()) {
      setError(account, context.getString(R.string.requires_pro_subscription));
      return;
    }
    if (isNullOrEmpty(account.getPassword())) {
      setError(account, context.getString(R.string.password_required));
      return;
    }
    if (isNullOrEmpty(account.getEncryptionKey())) {
      setError(account, context.getString(R.string.encryption_password_required));
      return;
    }
    try {
      synchronize(account);
    } catch (KeyManagementException
        | NoSuchAlgorithmException
        | HttpException
        | IntegrityException
        | VersionTooNewException e) {
      setError(account, e.getMessage());
    }
  }

  private void synchronize(CaldavAccount account)
      throws KeyManagementException, NoSuchAlgorithmException, Exceptions.HttpException,
          IntegrityException, VersionTooNewException {
    EteSyncClient client = this.client.forAccount(account);
    Map<Journal, CollectionInfo> resources = client.getCalendars();

    Set<String> uids = newHashSet(Iterables.transform(resources.values(), CollectionInfo::getUid));
    Timber.d("Found uids: %s", uids);
    for (CaldavCalendar calendar :
        caldavDao.findDeletedCalendars(account.getUuid(), newArrayList(uids))) {
      taskDeleter.delete(calendar);
    }

    for (Map.Entry<Journal, CollectionInfo> entry : resources.entrySet()) {
      CollectionInfo collection = entry.getValue();
      String uid = collection.getUid();

      CaldavCalendar calendar = caldavDao.getCalendarByUrl(account.getUuid(), uid);
      if (calendar == null) {
        calendar = new CaldavCalendar();
        calendar.setName(collection.getDisplayName());
        calendar.setAccount(account.getUuid());
        calendar.setUrl(collection.getUid());
        calendar.setUuid(UUIDHelper.newUUID());
        caldavDao.insert(calendar);
      } else {
        if (!calendar.getName().equals(collection.getDisplayName())) {
          calendar.setName(collection.getDisplayName());
          caldavDao.update(calendar);
          localBroadcastManager.broadcastRefreshList();
        }
      }
      sync(client, calendar, entry.getKey());
    }
    setError(account, "");
  }

  private void setError(CaldavAccount account, String message) {
    account.setError(message);
    caldavDao.update(account);
    localBroadcastManager.broadcastRefreshList();
    if (!Strings.isNullOrEmpty(message)) {
      Timber.e(message);
    }
  }

  private void sync(EteSyncClient client, CaldavCalendar caldavCalendar, Journal journal)
      throws IntegrityException, Exceptions.HttpException, VersionTooNewException {
    Timber.d("sync(%s)", caldavCalendar);

    Map<String, CaldavTaskContainer> localChanges = newHashMap();
    for (CaldavTaskContainer task : caldavDao.getCaldavTasksToPush(caldavCalendar.getUuid())) {
      localChanges.put(task.getRemoteId(), task);
    }

    String remoteCtag = journal.getLastUid();
    if (Strings.isNullOrEmpty(remoteCtag) || !remoteCtag.equals(caldavCalendar.getCtag())) {
      Timber.v("Applying remote changes");
      client.getSyncEntries(
          journal,
          caldavCalendar,
          syncEntries -> applyEntries(caldavCalendar, syncEntries, localChanges.keySet()));
    } else {
      Timber.d("%s up to date", caldavCalendar.getName());
    }

    List<SyncEntry> changes = new ArrayList<>();
    for (CaldavTask task : caldavDao.getDeleted(caldavCalendar.getUuid())) {
      String vtodo = task.getVtodo();
      if (!Strings.isNullOrEmpty(vtodo)) {
        changes.add(new SyncEntry(vtodo, Actions.DELETE));
      }
    }

    for (CaldavTaskContainer task : localChanges.values()) {
      String vtodo = task.getVtodo();
      boolean existingTask = !Strings.isNullOrEmpty(vtodo);

      if (task.isDeleted()) {
        if (existingTask) {
          changes.add(new SyncEntry(vtodo, Actions.DELETE));
        }
      } else {
        changes.add(new SyncEntry(getVtodo(task), existingTask ? Actions.CHANGE : Actions.ADD));
      }
    }

    remoteCtag = caldavCalendar.getCtag();
    CryptoManager crypto = client.getCrypto(journal);
    List<Pair<Entry, SyncEntry>> updates = new ArrayList<>();
    JournalEntryManager.Entry previous =
        Strings.isNullOrEmpty(remoteCtag) ? null : Entry.getFakeWithUid(remoteCtag);

    for (SyncEntry syncEntry : changes) {
      Entry entry = new Entry();
      entry.update(crypto, syncEntry.toJson(), previous);
      updates.add(Pair.create(entry, syncEntry));
      previous = entry;
    }
    if (updates.size() > 0) {
      Timber.v("Pushing local changes");
      client.pushEntries(journal, from(updates).transform(p -> p.first).toList(), remoteCtag);
      Timber.v("Applying local changes");
      applyEntries(caldavCalendar, updates, emptySet());
    }

    Timber.d("UPDATE %s", caldavCalendar);

    caldavDao.update(caldavCalendar);
    caldavDao.updateParents(caldavCalendar.getUuid());
    localBroadcastManager.broadcastRefresh();
  }

  private void applyEntries(
      CaldavCalendar caldavCalendar, List<Pair<Entry, SyncEntry>> syncEntries, Set<String> dirty) {
    for (Pair<Entry, SyncEntry> entry : syncEntries) {
      Entry journalEntry = entry.first;
      SyncEntry syncEntry = entry.second;
      Actions action = syncEntry.getAction();
      String vtodo = syncEntry.getContent();
      Timber.v("%s: %s", action, vtodo);
      at.bitfire.ical4android.Task task = CaldavUtils.fromVtodo(vtodo);
      String remoteId = task.getUid();
      CaldavTask caldavTask = caldavDao.getTaskByRemoteId(caldavCalendar.getUuid(), remoteId);
      switch (action) {
        case ADD:
        case CHANGE:
          if (dirty.contains(remoteId)) {
            caldavTask.setVtodo(vtodo);
            caldavDao.update(caldavTask);
          } else {
            processVTodo(caldavCalendar, caldavTask, task, vtodo);
          }
          break;
        case DELETE:
          dirty.remove(remoteId);
          if (caldavTask != null) {
            taskDeleter.delete(caldavTask.getTask());
          }
          break;
      }
      caldavCalendar.setCtag(journalEntry.getUid());
      caldavDao.update(caldavCalendar);
    }
  }

  private String getVtodo(CaldavTaskContainer container) {
    Task task = container.getTask();
    CaldavTask caldavTask = container.getCaldavTask();

    at.bitfire.ical4android.Task remoteModel = CaldavConverter.toCaldav(caldavTask, task);
    LinkedList<String> categories = remoteModel.getCategories();
    categories.clear();
    categories.addAll(transform(tagDataDao.getTagDataForTask(task.getId()), TagData::getName));
    if (Strings.isNullOrEmpty(caldavTask.getRemoteId())) {
      String caldavUid = UUIDHelper.newUUID();
      caldavTask.setRemoteId(caldavUid);
      remoteModel.setUid(caldavUid);
    } else {
      remoteModel.setUid(caldavTask.getRemoteId());
    }

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    remoteModel.write(os);
    return new String(os.toByteArray());
  }

  private void processVTodo(
      CaldavCalendar calendar,
      CaldavTask caldavTask,
      at.bitfire.ical4android.Task remote,
      String vtodo) {
    Task task;
    if (caldavTask == null) {
      task = taskCreator.createWithValues("");
      taskDao.createNew(task);
      caldavTask = new CaldavTask(task.getId(), calendar.getUuid(), remote.getUid(), null);
    } else {
      task = taskDao.fetch(caldavTask.getTask());
    }

    CaldavConverter.apply(task, remote);
    tagDao.applyTags(task, tagDataDao, CaldavUtils.getTags(tagDataDao, remote.getCategories()));
    task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
    task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true);
    taskDao.save(task);
    caldavTask.setVtodo(vtodo);
    caldavTask.setLastSync(DateUtilities.now() + 1000L);
    caldavTask.setRemoteParent(getParent(remote));

    if (caldavTask.getId() == Task.NO_ID) {
      caldavTask.setId(caldavDao.insert(caldavTask));
      Timber.d("NEW %s", caldavTask);
    } else {
      caldavDao.update(caldavTask);
      Timber.d("UPDATE %s", caldavTask);
    }
  }
}
