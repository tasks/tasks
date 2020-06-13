package org.tasks.etesync;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.tasks.Strings.isNullOrEmpty;

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
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import com.etesync.journalmanager.model.CollectionInfo;
import com.etesync.journalmanager.model.SyncEntry;
import com.etesync.journalmanager.model.SyncEntry.Actions;
import com.google.common.collect.Iterables;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskDeleter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.fortuna.ical4j.model.property.ProdId;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.caldav.iCalendar;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.data.CaldavTaskContainer;
import org.tasks.injection.ApplicationContext;
import timber.log.Timber;

public class EteSynchronizer {

  static {
    ICalendar.Companion.setProdId(
        new ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN"));
  }

  private final CaldavDao caldavDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final TaskDeleter taskDeleter;
  private final Inventory inventory;
  private final EteSyncClient client;
  private final iCalendar iCal;
  private final Context context;

  @Inject
  public EteSynchronizer(
      @ApplicationContext Context context,
      CaldavDao caldavDao,
      LocalBroadcastManager localBroadcastManager,
      TaskDeleter taskDeleter,
      Inventory inventory,
      EteSyncClient client,
      iCalendar iCal) {
    this.context = context;
    this.caldavDao = caldavDao;
    this.localBroadcastManager = localBroadcastManager;
    this.taskDeleter = taskDeleter;
    this.inventory = inventory;
    this.client = client;
    this.iCal = iCal;
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
    UserInfo userInfo = client.getUserInfo();
    Map<Journal, CollectionInfo> resources = client.getCalendars(userInfo);

    Set<String> uids = newHashSet(Iterables.transform(resources.values(), CollectionInfo::getUid));
    Timber.d("Found uids: %s", uids);
    for (CaldavCalendar calendar :
        caldavDao.findDeletedCalendars(account.getUuid(), new ArrayList<>(uids))) {
      taskDeleter.delete(calendar);
    }

    for (Map.Entry<Journal, CollectionInfo> entry : resources.entrySet()) {
      CollectionInfo collection = entry.getValue();
      String uid = collection.getUid();

      CaldavCalendar calendar = caldavDao.getCalendarByUrl(account.getUuid(), uid);
      Integer colorInt = collection.getColor();
      int color = colorInt == null ? 0 : colorInt;
      if (calendar == null) {
        calendar = new CaldavCalendar();
        calendar.setName(collection.getDisplayName());
        calendar.setAccount(account.getUuid());
        calendar.setUrl(collection.getUid());
        calendar.setUuid(UUIDHelper.newUUID());
        calendar.setColor(color);
        caldavDao.insert(calendar);
      } else {
        if (!calendar.getName().equals(collection.getDisplayName())
        || calendar.getColor() != color) {
          calendar.setName(collection.getDisplayName());
          calendar.setColor(color);
          caldavDao.update(calendar);
          localBroadcastManager.broadcastRefreshList();
        }
      }
      sync(client, userInfo, calendar, entry.getKey());
    }
    setError(account, "");
  }

  private void setError(CaldavAccount account, String message) {
    account.setError(message);
    caldavDao.update(account);
    localBroadcastManager.broadcastRefreshList();
    if (!isNullOrEmpty(message)) {
      Timber.e(message);
    }
  }

  private void sync(
      EteSyncClient client, UserInfo userInfo, CaldavCalendar caldavCalendar, Journal journal)
      throws IntegrityException, Exceptions.HttpException, VersionTooNewException {
    Timber.d("sync(%s)", caldavCalendar);

    Map<String, CaldavTaskContainer> localChanges = newHashMap();
    for (CaldavTaskContainer task : caldavDao.getCaldavTasksToPush(caldavCalendar.getUuid())) {
      localChanges.put(task.getRemoteId(), task);
    }

    String remoteCtag = journal.getLastUid();
    if (isNullOrEmpty(remoteCtag) || !remoteCtag.equals(caldavCalendar.getCtag())) {
      Timber.v("Applying remote changes");
      client.getSyncEntries(
          userInfo,
          journal,
          caldavCalendar,
          syncEntries -> applyEntries(caldavCalendar, syncEntries, localChanges.keySet()));
    } else {
      Timber.d("%s up to date", caldavCalendar.getName());
    }

    List<SyncEntry> changes = new ArrayList<>();
    for (CaldavTask task : caldavDao.getDeleted(caldavCalendar.getUuid())) {
      String vtodo = task.getVtodo();
      if (!isNullOrEmpty(vtodo)) {
        changes.add(new SyncEntry(vtodo, Actions.DELETE));
      }
    }

    for (CaldavTaskContainer task : localChanges.values()) {
      String vtodo = task.getVtodo();
      boolean existingTask = !isNullOrEmpty(vtodo);

      if (task.isDeleted()) {
        if (existingTask) {
          changes.add(new SyncEntry(vtodo, Actions.DELETE));
        }
      } else {
        changes.add(
            new SyncEntry(
                new String(iCal.toVtodo(task.getCaldavTask(), task.getTask())),
                existingTask ? Actions.CHANGE : Actions.ADD));
      }
    }

    remoteCtag = caldavCalendar.getCtag();
    CryptoManager crypto = client.getCrypto(userInfo, journal);
    List<Pair<Entry, SyncEntry>> updates = new ArrayList<>();
    JournalEntryManager.Entry previous =
        isNullOrEmpty(remoteCtag) ? null : Entry.getFakeWithUid(remoteCtag);

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
      at.bitfire.ical4android.Task task = iCalendar.Companion.fromVtodo(vtodo);
      if (task == null) {
        continue;
      }
      String remoteId = task.getUid();
      CaldavTask caldavTask = caldavDao.getTaskByRemoteId(caldavCalendar.getUuid(), remoteId);
      switch (action) {
        case ADD:
        case CHANGE:
          if (dirty.contains(remoteId)) {
            caldavTask.setVtodo(vtodo);
            caldavDao.update(caldavTask);
          } else {
            iCal.fromVtodo(caldavCalendar, caldavTask, task, vtodo, null, null);
          }
          break;
        case DELETE:
          dirty.remove(remoteId);
          if (caldavTask != null) {
            if (caldavTask.isDeleted()) {
              caldavDao.delete(caldavTask);
            } else {
              taskDeleter.delete(caldavTask.getTask());
            }
          }
          break;
      }
      caldavCalendar.setCtag(journalEntry.getUid());
      caldavDao.update(caldavCalendar);
    }
  }
}
