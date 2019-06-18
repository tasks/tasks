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
import at.bitfire.dav4jvm.DavCalendar;
import at.bitfire.dav4jvm.DavResource;
import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.Response.HrefRelation;
import at.bitfire.dav4jvm.exception.DavException;
import at.bitfire.dav4jvm.exception.HttpException;
import at.bitfire.dav4jvm.property.CalendarData;
import at.bitfire.dav4jvm.property.DisplayName;
import at.bitfire.dav4jvm.property.GetCTag;
import at.bitfire.dav4jvm.property.GetETag;
import at.bitfire.ical4android.ICalendar;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.fortuna.ical4j.model.property.ProdId;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.injection.ForApplication;
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
  private final Inventory inventory;
  private final Tracker tracker;
  private final CaldavClient client;
  private final Context context;

  @Inject
  public CaldavSynchronizer(
      @ForApplication Context context,
      CaldavDao caldavDao,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      TaskCreator taskCreator,
      TaskDeleter taskDeleter,
      Inventory inventory,
      Tracker tracker,
      CaldavClient client) {
    this.context = context;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.taskCreator = taskCreator;
    this.taskDeleter = taskDeleter;
    this.inventory = inventory;
    this.tracker = tracker;
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
    CaldavClient caldavClient = client.forAccount(account);
    List<Response> resources;
    try {
      resources = caldavClient.getCalendars();
    } catch (IOException | DavException e) {
      setError(account, e.getMessage());
      tracker.reportException(e);
      return;
    }
    Set<String> urls = newHashSet(transform(resources, c -> c.getHref().toString()));
    Timber.d("Found calendars: %s", urls);
    for (CaldavCalendar calendar :
        caldavDao.findDeletedCalendars(account.getUuid(), newArrayList(urls))) {
      taskDeleter.delete(calendar);
    }
    for (Response resource : resources) {
      String url = resource.getHref().toString();

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

  private void sync(CaldavCalendar caldavCalendar, Response resource, OkHttpClient httpClient) {
    Timber.d("sync(%s)", caldavCalendar);
    HttpUrl httpUrl = resource.getHref();
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

      ResponseList members = new ResponseList(HrefRelation.MEMBER);
      davCalendar.calendarQuery("VTODO", null, null, members);

      Set<String> remoteObjects = newHashSet(transform(members, Response::hrefName));

      Iterable<Response> changed =
          filter(
              ImmutableSet.copyOf(members),
              vCard -> {
                GetETag eTag = vCard.get(GetETag.class);
                if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                  return false;
                }
                CaldavTask caldavTask =
                    caldavDao.getTask(caldavCalendar.getUuid(), vCard.hrefName());
                return caldavTask == null || !eTag.getETag().equals(caldavTask.getEtag());
              });

      for (List<Response> items : partition(changed, 30)) {
        if (items.size() == 1) {
          Response vCard = items.get(0);
          GetETag eTag = vCard.get(GetETag.class);
          HttpUrl url = vCard.getHref();
          if (eTag == null || isNullOrEmpty(eTag.getETag())) {
            throw new DavException("Received CalDAV GET response without ETag for " + url);
          }
          Timber.d("SINGLE %s", url);

          org.tasks.caldav.Response response = new org.tasks.caldav.Response(true);
          new DavResource(httpClient, url).get("text/calendar", response);
          processVTodo(vCard.hrefName(), caldavCalendar, eTag.getETag(), response.getBody());
        } else {
          ArrayList<HttpUrl> urls = newArrayList(Iterables.transform(items, Response::getHref));
          ResponseList responses = new ResponseList(HrefRelation.MEMBER);
          davCalendar.multiget(urls, responses);

          Timber.d("MULTI %s", urls);

          for (Response vCard : responses) {
            GetETag eTag = vCard.get(GetETag.class);
            HttpUrl url = vCard.getHref();
            if (eTag == null || isNullOrEmpty(eTag.getETag())) {
              throw new DavException("Received CalDAV GET response without ETag for " + url);
            }
            CalendarData calendarData = vCard.get(CalendarData.class);
            if (calendarData == null || isNullOrEmpty(calendarData.getICalendar())) {
              throw new DavException(
                  "Received CalDAV GET response without CalendarData for " + url);
            }

            processVTodo(
                vCard.hrefName(), caldavCalendar, eTag.getETag(), calendarData.getICalendar());
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
    } catch (Exception e) {
      tracker.reportException(e);
    }

    localBroadcastManager.broadcastRefresh();
  }

  private void pushLocalChanges(
      CaldavCalendar caldavCalendar, OkHttpClient httpClient, HttpUrl httpUrl) {

    for (CaldavTask task : caldavDao.getDeleted(caldavCalendar.getUuid())) {
      deleteRemoteResource(httpClient, httpUrl, task);
    }

    for (Task task : taskDao.getCaldavTasksToPush(caldavCalendar.getUuid())) {
      try {
        pushTask(task, httpClient, httpUrl);
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
        remote.delete(null, response -> null);
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

  private void pushTask(Task task, OkHttpClient httpClient, HttpUrl httpUrl) throws IOException {
    Timber.d("pushing %s", task);
    CaldavTask caldavTask = caldavDao.getTask(task.getId());

    if (caldavTask == null) {
      return;
    }

    if (task.isDeleted()) {
      if (deleteRemoteResource(httpClient, httpUrl, caldavTask)) {
        taskDeleter.delete(task);
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
      org.tasks.caldav.Response response = new org.tasks.caldav.Response();
      remote.put(requestBody, null, false, response);
      GetETag getETag = GetETag.Companion.fromResponse(response.get());
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
    List<at.bitfire.ical4android.Task> tasks =
        at.bitfire.ical4android.Task.Companion.fromReader(new StringReader(vtodo));

    if (tasks.size() == 1) {
      at.bitfire.ical4android.Task remote = tasks.get(0);
      Task task;
      CaldavTask caldavTask = caldavDao.getTask(caldavCalendar.getUuid(), fileName);
      if (caldavTask == null) {
        task = taskCreator.createWithValues("");
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
