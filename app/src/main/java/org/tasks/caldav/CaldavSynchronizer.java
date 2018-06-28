package org.tasks.caldav;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.partition;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.DavResponse;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CalendarData;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.GetCTag;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.ical4android.ICalendar;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.fortuna.ical4j.model.property.ProdId;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.injection.ForApplication;
import org.tasks.security.Encryption;
import timber.log.Timber;

public class CaldavSynchronizer {

  static {
    ICalendar.Companion.setProdId(
        new ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN"));
  }

  private final CaldavDao caldavDao;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final TaskCreator taskCreator;
  private final TaskDeleter taskDeleter;
  private final Encryption encryption;
  private final Inventory inventory;
  private final Context context;

  @Inject
  public CaldavSynchronizer(
      @ForApplication Context context,
      CaldavDao caldavDao,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      TaskCreator taskCreator,
      TaskDeleter taskDeleter,
      Encryption encryption,
      Inventory inventory) {
    this.context = context;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.taskCreator = taskCreator;
    this.taskDeleter = taskDeleter;
    this.encryption = encryption;
    this.inventory = inventory;
  }

  public void sync() {
    // required for dav4android (ServiceLoader)
    Thread.currentThread().setContextClassLoader(context.getClassLoader());
    for (CaldavAccount account : caldavDao.getAccounts()) {
      if (!inventory.hasPro()) {
        account.setError(context.getString(R.string.requires_pro_subscription));
        caldavDao.update(account);
        localBroadcastManager.broadcastRefreshList();
        continue;
      }
      if (isNullOrEmpty(account.getPassword())) {
        account.setError(context.getString(R.string.password_required));
        caldavDao.update(account);
        localBroadcastManager.broadcastRefreshList();
        Timber.e("Missing password for %s", account);
        continue;
      }
      CaldavClient caldavClient = new CaldavClient(account, encryption);
      List<DavResponse> resources;
      try {
        resources = caldavClient.getCalendars();
      } catch (IOException | DavException e) {
        account.setError(e.getMessage());
        caldavDao.update(account);
        localBroadcastManager.broadcastRefreshList();
        Timber.e(e);
        continue;
      }
      Set<String> urls = newHashSet(transform(resources, c -> c.getUrl().toString()));
      Timber.d("Found calendars: %s", urls);
      for (CaldavCalendar calendar :
          caldavDao.findDeletedCalendars(account.getUuid(), newArrayList(urls))) {
        taskDeleter.delete(calendar);
      }
      for (DavResponse resource : resources) {
        String url = resource.getUrl().toString();

        CaldavCalendar calendar = caldavDao.getCalendarByUrl(account.getUuid(), url);
        if (calendar == null) {
          calendar = new CaldavCalendar();
          calendar.setName(resource.get(DisplayName.class).getDisplayName());
          calendar.setAccount(account.getUuid());
          calendar.setUrl(url);
          calendar.setUuid(UUIDHelper.newUUID());
          calendar.setId(caldavDao.insert(calendar));
        }
        sync(calendar, resource, caldavClient.getHttpClient());
      }
      account.setError("");
      caldavDao.update(account);
      localBroadcastManager.broadcastRefreshList();
    }
  }

  private void sync(CaldavCalendar caldavCalendar, DavResponse resource, OkHttpClient httpClient) {
    Timber.d("sync(%s)", caldavCalendar);
    HttpUrl httpUrl = resource.getUrl();
    try {
      pushLocalChanges(caldavCalendar, httpClient, httpUrl);

      String remoteName = resource.get(DisplayName.class).getDisplayName();
      if (!caldavCalendar.getName().equals(remoteName)) {
        Timber.d("%s -> %s", caldavCalendar.getName(), remoteName);
        caldavCalendar.setName(remoteName);
        caldavDao.update(caldavCalendar);
        localBroadcastManager.broadcastRefreshList();
      }

      String remoteCtag = resource.get(GetCTag.class).getCTag();
      String localCtag = caldavCalendar.getCtag();

      if (localCtag != null && localCtag.equals(remoteCtag)) {
        Timber.d("%s up to date", caldavCalendar.getName());
        return;
      }

      DavCalendar davCalendar = new DavCalendar(httpClient, httpUrl);

      List<DavResponse> members = davCalendar.calendarQuery("VTODO", null, null).getMembers();

      Set<String> remoteObjects = newHashSet(transform(members, DavResponse::fileName));

      Iterable<DavResponse> changed =
          filter(
              ImmutableSet.copyOf(members),
              vCard -> {
                GetETag eTag = vCard.get(GetETag.class);
                if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                  return false;
                }
                CaldavTask caldavTask =
                    caldavDao.getTask(caldavCalendar.getUuid(), vCard.fileName());
                return caldavTask == null || !eTag.getETag().equals(caldavTask.getEtag());
              });

      for (List<DavResponse> items : partition(changed, 30)) {
        if (items.size() == 1) {
          DavResponse vCard = items.get(0);
          GetETag eTag = vCard.get(GetETag.class);
          if (eTag == null || isNullOrEmpty(eTag.getETag())) {
            throw new DavException(
                "Received CalDAV GET response without ETag for " + vCard.getUrl());
          }
          Timber.d("SINGLE %s", vCard.getUrl());
          DavResponse response = new DavResource(httpClient, vCard.getUrl()).get("text/calendar");
          ResponseBody responseBody = response.getBody();
          Reader reader = null;
          try {
            reader = responseBody.charStream();
            processVTodo(
                vCard.fileName(), caldavCalendar, eTag.getETag(), CharStreams.toString(reader));
          } finally {
            if (reader != null) {
              reader.close();
            }
          }
        } else {
          ArrayList<HttpUrl> urls = newArrayList(Iterables.transform(items, DavResponse::getUrl));
          DavResponse response = davCalendar.multiget(urls);

          Timber.d("MULTI %s", urls);

          for (DavResponse vCard : response.getMembers()) {
            GetETag eTag = vCard.get(GetETag.class);
            if (eTag == null || isNullOrEmpty(eTag.getETag())) {
              throw new DavException(
                  "Received CalDAV GET response without ETag for " + vCard.getUrl());
            }
            CalendarData calendarData = vCard.get(CalendarData.class);
            if (calendarData == null || isNullOrEmpty(calendarData.getICalendar())) {
              throw new DavException(
                  "Received CalDAV GET response without CalendarData for " + vCard.getUrl());
            }

            processVTodo(
                vCard.fileName(), caldavCalendar, eTag.getETag(), calendarData.getICalendar());
          }
        }
      }

      List<String> deleted =
          newArrayList(
              difference(
                  newHashSet(caldavDao.getObjects(caldavCalendar.getUuid())),
                  newHashSet(remoteObjects)));
      if (deleted.size() > 0) {
        Timber.d("DELETED %s", deleted);
        taskDeleter.delete(caldavDao.getTasks(caldavCalendar.getUuid(), deleted));
      }

      caldavCalendar.setCtag(remoteCtag);
      Timber.d("UPDATE %s", caldavCalendar);
      caldavDao.update(caldavCalendar);
    } catch (IOException | DavException e) {
      Timber.e(e);
    } catch (Exception e) {
      Timber.e(e);
    }

    localBroadcastManager.broadcastRefresh();
  }

  private void pushLocalChanges(
      CaldavCalendar caldavCalendar, OkHttpClient httpClient, HttpUrl httpUrl) {
    List<Task> tasks = taskDao.getCaldavTasksToPush(caldavCalendar.getUuid());
    for (com.todoroo.astrid.data.Task task : tasks) {
      try {
        pushTask(task, caldavCalendar, httpClient, httpUrl);
      } catch (IOException e) {
        Timber.e(e);
      }
    }
  }

  private boolean deleteRemoteResource(
      OkHttpClient httpClient, HttpUrl httpUrl, CaldavTask caldavTask) {
    try {
      if (!Strings.isNullOrEmpty(caldavTask.getObject())) {
        DavResource remote =
            new DavResource(
                httpClient, httpUrl.newBuilder().addPathSegment(caldavTask.getObject()).build());
        remote.delete(null);
      }
    } catch (HttpException e) {
      if (e.getCode() != 404) {
        Timber.e(e);
        return false;
      }
    } catch (IOException e) {
      Timber.e(e);
      return false;
    }
    caldavDao.delete(caldavTask);
    return true;
  }

  private void pushTask(
      Task task, CaldavCalendar caldavCalendar, OkHttpClient httpClient, HttpUrl httpUrl)
      throws IOException {
    Timber.d("pushing %s", task);
    List<CaldavTask> deleted = caldavDao.getDeleted(task.getId(), caldavCalendar.getUuid());
    if (!deleted.isEmpty()) {
      for (CaldavTask entry : deleted) {
        deleteRemoteResource(httpClient, httpUrl, entry);
      }
      return;
    }

    CaldavTask caldavTask = caldavDao.getTask(task.getId());

    if (caldavTask == null) {
      return;
    }

    if (task.isDeleted()) {
      if (deleteRemoteResource(httpClient, httpUrl, caldavTask)) {
        caldavDao.delete(caldavTask);
      }
      return;
    }

    at.bitfire.ical4android.Task remoteModel = CaldavConverter.toCaldav(caldavTask, task);

    if (Strings.isNullOrEmpty(caldavTask.getRemoteId())) {
      String caldavUid = UUIDHelper.newUUID();
      caldavTask.setRemoteId(caldavUid);
      remoteModel.setUid(caldavUid);
    } else {
      remoteModel.setUid(caldavTask.getRemoteId());
    }

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    remoteModel.write(os);
    byte[] data = os.toByteArray();
    RequestBody requestBody = RequestBody.create(DavCalendar.Companion.getMIME_ICALENDAR(), data);

    try {
      DavResource remote =
          new DavResource(
              httpClient, httpUrl.newBuilder().addPathSegment(caldavTask.getObject()).build());
      DavResponse response = remote.put(requestBody, null, false);
      GetETag getETag = response.get(GetETag.class);
      if (getETag != null && !isNullOrEmpty(getETag.getETag())) {
        caldavTask.setEtag(getETag.getETag());
        caldavTask.setVtodo(new String(data));
      }
    } catch (HttpException e) {
      Timber.e(e);
      return;
    }

    caldavTask.setLastSync(currentTimeMillis());
    caldavDao.update(caldavTask);
    Timber.d("SENT %s", caldavTask);
  }

  private void processVTodo(
      String fileName, CaldavCalendar caldavCalendar, String eTag, String vtodo) {
    List<at.bitfire.ical4android.Task> tasks;
    try {
      tasks = at.bitfire.ical4android.Task.Companion.fromReader(new StringReader(vtodo));
    } catch (Exception e) {
      Timber.e(e);
      return;
    }

    if (tasks.size() == 1) {
      at.bitfire.ical4android.Task remote = tasks.get(0);
      Task task;
      CaldavTask caldavTask = caldavDao.getTask(caldavCalendar.getUuid(), fileName);
      if (caldavTask == null) {
        task = taskCreator.createWithValues(null, "");
        taskDao.createNew(task);
        caldavTask =
            new CaldavTask(task.getId(), caldavCalendar.getUuid(), remote.getUid(), fileName);
      } else {
        task = taskDao.fetch(caldavTask.getTask());
      }
      CaldavConverter.apply(task, remote);
      task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
      taskDao.save(task);
      caldavTask.setVtodo(vtodo);
      caldavTask.setEtag(eTag);
      caldavTask.setLastSync(DateUtilities.now() + 1000L);
      if (caldavTask.getId() == Task.NO_ID) {
        caldavTask.setId(caldavDao.insert(caldavTask));
        Timber.d("NEW %s", caldavTask);
      } else {
        caldavDao.update(caldavTask);
        Timber.d("UPDATE %s", caldavTask);
      }
    } else {
      Timber.e("Received VCALENDAR with %s VTODOs; ignoring %s", tasks.size(), fileName);
    }
  }
}
