package org.tasks.caldav;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;

import net.fortuna.ical4j.model.property.ProdId;

import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.data.CaldavTask;
import org.tasks.injection.InjectingAbstractThreadedSyncAdapter;
import org.tasks.injection.SyncAdapterComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import at.bitfire.dav4android.BasicDigestAuthHandler;
import at.bitfire.dav4android.DavCalendar;
import at.bitfire.dav4android.DavResource;
import at.bitfire.dav4android.PropertyCollection;
import at.bitfire.dav4android.exception.DavException;
import at.bitfire.dav4android.exception.HttpException;
import at.bitfire.dav4android.property.CalendarData;
import at.bitfire.dav4android.property.DisplayName;
import at.bitfire.dav4android.property.GetCTag;
import at.bitfire.dav4android.property.GetETag;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.ical4android.iCalendar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.partition;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class CalDAVSyncAdapter extends InjectingAbstractThreadedSyncAdapter {

    static {
        iCalendar.Companion.setProdId(new ProdId("+//IDN tasks.org//android-" + BuildConfig.VERSION_CODE + "//EN"));
    }

    @Inject CaldavDao caldavDao;
    @Inject CaldavAccountManager caldavAccountManager;
    @Inject TaskDao taskDao;
    @Inject LocalBroadcastManager localBroadcastManager;
    @Inject TaskCreator taskCreator;
    @Inject TaskDeleter taskDeleter;

    CalDAVSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // required for dav4android (ServiceLoader)
        Thread.currentThread().setContextClassLoader(getContext().getClassLoader());

        String uuid = account.name;
        CaldavAccount caldavAccount = caldavDao.getAccount(uuid);
        if (caldavAccount == null) {
            Timber.e("Unknown account %s", uuid);
            caldavAccountManager.removeAccount(account);
            return;
        }
        Timber.d("onPerformSync: %s [%s]", caldavAccount.getName(), uuid);
        org.tasks.caldav.Account localAccount = caldavAccountManager.getAccount(caldavAccount.getUuid());
        if (isNullOrEmpty(localAccount.getPassword())) {
            Timber.e("Missing password for %s", caldavAccount.getName());
            syncResult.stats.numAuthExceptions++;
            return;
        }
        syncResult.stats.numAuthExceptions = 0;
        BasicDigestAuthHandler basicDigestAuthHandler = new BasicDigestAuthHandler(null, caldavAccount.getUsername(), localAccount.getPassword());
        OkHttpClient httpClient = new OkHttpClient().newBuilder()
                .addNetworkInterceptor(basicDigestAuthHandler)
                .authenticator(basicDigestAuthHandler)
                .cookieJar(new MemoryCookieStore())
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        URI uri = URI.create(caldavAccount.getUrl());
        HttpUrl httpUrl = HttpUrl.get(uri);
        DavCalendar davCalendar = new DavCalendar(httpClient, httpUrl);
        try {
            pushLocalChanges(caldavAccount, httpClient, httpUrl);

            davCalendar.propfind(0, GetCTag.NAME, DisplayName.NAME);

            PropertyCollection properties = davCalendar.getProperties();
            String remoteName = properties.get(DisplayName.class).getDisplayName();
            if (!caldavAccount.getName().equals(remoteName)) {
                Timber.d("%s -> %s", caldavAccount.getName(), remoteName);
                caldavAccount.setName(remoteName);
                caldavDao.update(caldavAccount);
                localBroadcastManager.broadcastRefreshList();
            }

            String remoteCtag = properties.get(GetCTag.class).getCTag();
            String localCtag = caldavAccount.getCtag();

            if (localCtag != null && localCtag.equals(remoteCtag)) {
                Timber.d("%s up to date", caldavAccount.getName());
                return;
            }

            davCalendar.calendarQuery("VTODO", null, null);

            Set<String> remoteObjects = newHashSet(transform(davCalendar.getMembers(), DavResource::fileName));

            Iterable<DavResource> changed = filter(davCalendar.getMembers(), vCard -> {
                GetETag eTag = (GetETag) vCard.getProperties().get(GetETag.NAME);
                if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                    return false;
                }
                CaldavTask caldavTask = caldavDao.getTask(caldavAccount.getUuid(), vCard.fileName());
                return caldavTask == null || !eTag.getETag().equals(caldavTask.getEtag());
            });

            for (List<DavResource> items : partition(changed, 30)) {
                if (items.size() == 1) {
                    DavResource vCard = items.get(0);
                    PropertyCollection vcardProperties = vCard.getProperties();
                    GetETag eTag = (GetETag) vcardProperties.get(GetETag.NAME);
                    if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                        throw new DavException("Received CalDAV GET response without ETag for " + vCard.getLocation());
                    }
                    ResponseBody responseBody = vCard.get("text/calendar");
                    Reader reader = responseBody.charStream();
                    try {
                        processVTodo(vCard.fileName(), caldavAccount, eTag.getETag(), reader);
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } else {
                    davCalendar.multiget(newArrayList(transform(changed, DavResource::getLocation)));

                    for (DavResource vCard : davCalendar.getMembers()) {
                        PropertyCollection vcardProperties = vCard.getProperties();

                        GetETag eTag = (GetETag) vcardProperties.get(GetETag.NAME);
                        if (eTag == null || isNullOrEmpty(eTag.getETag())) {
                            throw new DavException("Received CalDAV GET response without ETag for " + vCard.getLocation());
                        }
                        CalendarData calendarData = (CalendarData) vcardProperties.get(CalendarData.NAME);
                        if (calendarData == null || isNullOrEmpty(calendarData.getICalendar())) {
                            throw new DavException("Received CalDAV GET response without CalendarData for " + vCard.getLocation());
                        }

                        String vtodo = calendarData.getICalendar();
                        processVTodo(vCard.fileName(), caldavAccount, eTag.getETag(), new StringReader(vtodo));
                    }
                }
            }

            Sets.SetView<String> deleted = difference(
                    newHashSet(caldavDao.getObjects(caldavAccount.getUuid())),
                    newHashSet(remoteObjects));
            List<String> toDelete = newArrayList(deleted);
            taskDeleter.markDeleted(caldavDao.getTasks(caldavAccount.getUuid(), toDelete));
            caldavDao.deleteObjects(caldavAccount.getUuid(), toDelete);

            caldavAccount.setCtag(remoteCtag);
            caldavDao.update(caldavAccount);
        } catch (IOException | HttpException | DavException | CalendarStorageException e) {
            Timber.e(e, e.getMessage());
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }

        localBroadcastManager.broadcastRefresh();
    }

    private void pushLocalChanges(CaldavAccount caldavAccount, OkHttpClient httpClient, HttpUrl httpUrl) {
        List<Task> tasks = taskDao.getCaldavTasksToPush(caldavAccount.getUuid());
        for (com.todoroo.astrid.data.Task task : tasks) {
            try {
                pushTask(task, caldavAccount, httpClient, httpUrl);
            } catch (IOException e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    private boolean deleteRemoteResource(OkHttpClient httpClient, HttpUrl httpUrl, CaldavTask caldavTask) {
        try {
            if (!Strings.isNullOrEmpty(caldavTask.getObject())) {
                DavResource remote = new DavResource(httpClient, httpUrl.newBuilder().addPathSegment(caldavTask.getObject()).build());
                remote.delete(null);
            }
        } catch (HttpException e) {
            if (e.getStatus() != 404) {
                Timber.e(e, e.getMessage());
                return false;
            }
        } catch (IOException e) {
            Timber.e(e.getMessage(), e);
            return false;
        }
        caldavDao.delete(caldavTask);
        return true;
    }

    private void pushTask(Task task, CaldavAccount caldavAccount, OkHttpClient httpClient, HttpUrl httpUrl) throws IOException {
        Timber.d("pushing %s", task);
        List<CaldavTask> deleted = getDeleted(task.getId(), caldavAccount);
        if (!deleted.isEmpty()) {
            for (CaldavTask entry : deleted) {
                deleteRemoteResource(httpClient, httpUrl, entry);
            }
            return;
        }

        CaldavTask caldavMetadata = caldavDao.getTask(task.getId());

        if (caldavMetadata == null) {
            return;
        }

        if (task.isDeleted()) {
            if (deleteRemoteResource(httpClient, httpUrl, caldavMetadata)) {
                caldavDao.delete(caldavMetadata);
            }
            return;
        }

        at.bitfire.ical4android.Task remoteModel = TaskConverter.toCaldav(task);

        if (Strings.isNullOrEmpty(caldavMetadata.getRemoteId())) {
            String caldavUid = UUIDHelper.newUUID();
            caldavMetadata.setRemoteId(caldavUid);
            remoteModel.setUid(caldavUid);
        } else {
            remoteModel.setUid(caldavMetadata.getRemoteId());
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        remoteModel.write(os);
        RequestBody requestBody = RequestBody.create(
                DavCalendar.MIME_ICALENDAR,
                os.toByteArray());
        try {
            DavResource remote = new DavResource(httpClient, httpUrl.newBuilder().addPathSegment(caldavMetadata.getObject()).build());
            remote.put(requestBody, null, false);
        } catch (HttpException e) {
            Timber.e(e.getMessage(), e);
            return;
        }

        long modified = currentTimeMillis();
        task.setModificationDate(modified);
        task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
        taskDao.save(task);
        caldavMetadata.setLastSync(modified);
        caldavDao.update(caldavMetadata);
    }

    private List<CaldavTask> getDeleted(long taskId, CaldavAccount caldavAccount) {
        return caldavDao.getDeleted(taskId, caldavAccount.getUuid());
    }

    private void processVTodo(String fileName, CaldavAccount caldavAccount, String eTag, Reader reader) throws IOException, CalendarStorageException {
        List<at.bitfire.ical4android.Task> tasks;
        try {
            tasks = at.bitfire.ical4android.Task.fromReader(reader);
        } catch (InvalidCalendarException e) {
            Timber.e(e, e.getMessage());
            return;
        }

        if (tasks.size() == 1) {
            at.bitfire.ical4android.Task remote = tasks.get(0);
            Task task;
            CaldavTask caldavTask = caldavDao.getTask(caldavAccount.getUuid(), fileName);
            if (caldavTask == null) {
                task = taskCreator.createWithValues(null, "");
                taskDao.createNew(task);
                caldavTask = new CaldavTask(task.getId(), caldavAccount.getUuid(), remote.getUid(), fileName);
                Timber.d("NEW %s", remote);
            } else {
                task = taskDao.fetch(caldavTask.getTask());
                Timber.d("UPDATE %s", remote);
            }
            TaskConverter.apply(task, remote);
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskDao.save(task);
            caldavTask.setEtag(eTag);
            caldavTask.setLastSync(DateUtilities.now() + 1000L);
            if (caldavTask.getId() == Task.NO_ID) {
                caldavDao.insert(caldavTask);
            } else {
                caldavDao.update(caldavTask);
            }
        } else {
            Timber.e("Received VCALENDAR with %s VTODOs; ignoring %s", tasks.size(), fileName);
        }
    }

    @Override
    protected void inject(SyncAdapterComponent component) {
        component.inject(this);
    }
}
