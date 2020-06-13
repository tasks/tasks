package org.tasks.caldav;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.partition;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import android.content.Context;
import androidx.annotation.Nullable;
import at.bitfire.dav4jvm.DavCalendar;
import at.bitfire.dav4jvm.DavResource;
import at.bitfire.dav4jvm.Response;
import at.bitfire.dav4jvm.Response.HrefRelation;
import at.bitfire.dav4jvm.exception.DavException;
import at.bitfire.dav4jvm.exception.HttpException;
import at.bitfire.dav4jvm.exception.ServiceUnavailableException;
import at.bitfire.dav4jvm.exception.UnauthorizedException;
import at.bitfire.dav4jvm.property.CalendarColor;
import at.bitfire.dav4jvm.property.CalendarData;
import at.bitfire.dav4jvm.property.DisplayName;
import at.bitfire.dav4jvm.property.GetCTag;
import at.bitfire.dav4jvm.property.GetETag;
import at.bitfire.dav4jvm.property.SyncToken;
import at.bitfire.ical4android.ICalendar;
import com.google.common.collect.Iterables;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskDeleter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.net.ssl.SSLException;
import net.fortuna.ical4j.model.property.ProdId;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.analytics.Firebase;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.injection.ApplicationContext;
import timber.log.Timber;

public class CaldavSynchronizer {

  static {
    ICalendar.Companion.setProdId(
        new ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN"));
  }

  private final CaldavDao caldavDao;
  private final TaskDao taskDao;
  private final LocalBroadcastManager localBroadcastManager;
  private final TaskDeleter taskDeleter;
  private final Inventory inventory;
  private final Firebase firebase;
  private final CaldavClient client;
  private final iCalendar iCal;
  private final Context context;

  @Inject
  public CaldavSynchronizer(
      @ApplicationContext Context context,
      CaldavDao caldavDao,
      TaskDao taskDao,
      LocalBroadcastManager localBroadcastManager,
      TaskDeleter taskDeleter,
      Inventory inventory,
      Firebase firebase,
      CaldavClient client,
      iCalendar iCal) {
    this.context = context;
    this.caldavDao = caldavDao;
    this.taskDao = taskDao;
    this.localBroadcastManager = localBroadcastManager;
    this.taskDeleter = taskDeleter;
    this.inventory = inventory;
    this.firebase = firebase;
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
    try {
      synchronize(account);
    } catch (SocketTimeoutException
        | SSLException
        | ConnectException
        | UnknownHostException
        | UnauthorizedException
        | ServiceUnavailableException
        | KeyManagementException
        | NoSuchAlgorithmException e) {
      setError(account, e.getMessage());
    } catch (IOException | DavException e) {
      setError(account, e.getMessage());
      if (!(e instanceof HttpException) || ((HttpException) e).getCode() < 500) {
        firebase.reportException(e);
      }
    }
  }

  private void synchronize(CaldavAccount account)
      throws IOException, DavException, KeyManagementException, NoSuchAlgorithmException {
    CaldavClient caldavClient = client.forAccount(account);
    List<Response> resources = caldavClient.getCalendars();
    Set<String> urls = newHashSet(transform(resources, c -> c.getHref().toString()));
    Timber.d("Found calendars: %s", urls);
    for (CaldavCalendar calendar :
        caldavDao.findDeletedCalendars(account.getUuid(), new ArrayList<>(urls))) {
      taskDeleter.delete(calendar);
    }
    for (Response resource : resources) {
      String url = resource.getHref().toString();

      CaldavCalendar calendar = caldavDao.getCalendarByUrl(account.getUuid(), url);
      String remoteName = resource.get(DisplayName.class).getDisplayName();
      CalendarColor calendarColor = resource.get(CalendarColor.class);
      int color = calendarColor == null ? 0 : calendarColor.getColor();
      if (calendar == null) {
        calendar = new CaldavCalendar();
        calendar.setName(remoteName);
        calendar.setAccount(account.getUuid());
        calendar.setUrl(url);
        calendar.setUuid(UUIDHelper.newUUID());
        calendar.setColor(color);
        caldavDao.insert(calendar);
      } else if (!calendar.getName().equals(remoteName) || calendar.getColor() != color) {
        calendar.setColor(color);
        calendar.setName(remoteName);
        caldavDao.update(calendar);
        localBroadcastManager.broadcastRefreshList();
      }
      sync(calendar, resource, caldavClient.getHttpClient());
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

  private void sync(CaldavCalendar caldavCalendar, Response resource, OkHttpClient httpClient)
      throws DavException {
    Timber.d("sync(%s)", caldavCalendar);
    HttpUrl httpUrl = resource.getHref();
    pushLocalChanges(caldavCalendar, httpClient, httpUrl);

    SyncToken syncToken = resource.get(SyncToken.class);
    GetCTag ctag = resource.get(GetCTag.class);
    @Nullable String remoteCtag = null;
    if (syncToken != null) {
      remoteCtag = syncToken.getToken();
    } else if (ctag != null) {
      remoteCtag = ctag.getCTag();
    }
    String localCtag = caldavCalendar.getCtag();

    if (localCtag != null && localCtag.equals(remoteCtag)) {
      Timber.d("%s up to date", caldavCalendar.getName());
      return;
    }

    DavCalendar davCalendar = new DavCalendar(httpClient, httpUrl);

    ResponseList members = new ResponseList(HrefRelation.MEMBER);
    davCalendar.calendarQuery("VTODO", null, null, members);

    Iterable<Response> changed =
        filter(
            members,
            vCard -> {
              GetETag eTag = vCard.get(GetETag.class);
              if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                return false;
              }
              CaldavTask caldavTask = caldavDao.getTask(caldavCalendar.getUuid(), vCard.hrefName());
              return caldavTask == null || !eTag.getETag().equals(caldavTask.getEtag());
            });

    for (List<Response> items : partition(changed, 30)) {
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
          throw new DavException("Received CalDAV GET response without CalendarData for " + url);
        }
        String fileName = vCard.hrefName();
        String vtodo = calendarData.getICalendar();
        at.bitfire.ical4android.Task remote = iCalendar.Companion.fromVtodo(vtodo);
        if (remote == null) {
          Timber.e("Invalid VCALENDAR: %s", fileName);
          return;
        }

        CaldavTask caldavTask = caldavDao.getTask(caldavCalendar.getUuid(), fileName);
        iCal.fromVtodo(caldavCalendar, caldavTask, remote, vtodo, fileName, eTag.getETag());
      }
    }

    List<String> deleted =
        new ArrayList<>(
            difference(
                newHashSet(caldavDao.getObjects(caldavCalendar.getUuid())),
                newHashSet(transform(members, Response::hrefName))));
    if (deleted.size() > 0) {
      Timber.d("DELETED %s", deleted);
      taskDeleter.delete(caldavDao.getTasks(caldavCalendar.getUuid(), deleted));
    }

    caldavCalendar.setCtag(remoteCtag);
    Timber.d("UPDATE %s", caldavCalendar);
    caldavDao.update(caldavCalendar);

    caldavDao.updateParents(caldavCalendar.getUuid());

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
      if (!isNullOrEmpty(caldavTask.getObject())) {
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

    byte[] data = iCal.toVtodo(caldavTask, task);
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
}
