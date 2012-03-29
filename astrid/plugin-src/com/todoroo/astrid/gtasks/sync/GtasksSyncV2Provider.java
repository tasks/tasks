package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import android.text.TextUtils;

import com.google.api.services.tasks.model.Tasks;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.api.GoogleTasksException;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

public class GtasksSyncV2Provider extends SyncV2Provider {

    @Autowired TaskService taskService;
    @Autowired MetadataService metadataService;
    @Autowired StoreObjectDao storeObjectDao;
    @Autowired GtasksPreferenceService gtasksPreferenceService;
    @Autowired GtasksSyncService gtasksSyncService;
    @Autowired GtasksListService gtasksListService;
    @Autowired GtasksMetadataService gtasksMetadataService;
    @Autowired GtasksTaskListUpdater gtasksTaskListUpdater;

    static {
        AstridDependencyInjector.initialize();
    }

    private static GtasksSyncV2Provider instance = null;

    protected GtasksSyncV2Provider() {
        // prevent multiple sync providers
    }

    public synchronized static GtasksSyncV2Provider getInstance() {
        if(instance == null)
            instance = new GtasksSyncV2Provider();
        return instance;
    }

    @Override
    public String getName() {
        return ContextManager.getString(R.string.gtasks_GPr_header);
    }

    @Override
    public GtasksPreferenceService getUtilities() {
        return gtasksPreferenceService;
    }

    @Override
    public void signOut() {
        gtasksPreferenceService.clearLastSyncDate();
        gtasksPreferenceService.setToken(null);
        Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, null);
        gtasksMetadataService.clearMetadata();
    }

    @Override
    public boolean isActive() {
        return gtasksPreferenceService.isLoggedIn();
    }

    @Override
    public void synchronizeActiveTasks(final boolean manual, final SyncResultCallback callback) {
        callback.started();
        callback.incrementMax(100);

        gtasksPreferenceService.recordSyncStart();

        new Thread(new Runnable() {
            public void run() {
                callback.incrementProgress(50);
                String authToken = getValidatedAuthToken();
                final GtasksInvoker invoker = new GtasksInvoker(authToken);
                try {
                    gtasksListService.updateLists(invoker.allGtaskLists());
                } catch (IOException e) {
                    handler.handleException("gtasks-sync=io", e); //$NON-NLS-1$
                }

                StoreObject[] lists = gtasksListService.getLists();
                callback.incrementMax(25 * lists.length);
                final AtomicInteger finisher = new AtomicInteger(lists.length);

                pushUpdated(invoker, callback);

                for (final StoreObject list : lists) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synchronizeListHelper(list, invoker, manual, handler, callback);
                            callback.incrementProgress(25);
                            if (finisher.decrementAndGet() == 0) {
                                gtasksPreferenceService.recordSuccessfulSync();
                                gtasksPreferenceService.stopOngoing();
                                callback.finished();
                            }
                        }
                    }).start();
                }
            }
        }).start();
    }

    private synchronized void pushUpdated(GtasksInvoker invoker, SyncResultCallback callback) {
        TodorooCursor<Task> queued = taskService.query(Query.select(Task.PROPERTIES).
                join(Join.left(Metadata.TABLE, Task.ID.eq(Metadata.TASK))).where(
                Criterion.and(Task.USER_ID.eq(Task.USER_ID_SELF),
                        Criterion.or(
                                Criterion.and(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY),
                                        Task.MODIFICATION_DATE.gt(GtasksMetadata.LAST_SYNC)),
                                        Metadata.KEY.isNull()))));
        callback.incrementMax(queued.getCount() * 10);
        try {
            Task task = new Task();
            for (queued.moveToFirst(); !queued.isAfterLast(); queued.moveToNext()) {
                task.readFromCursor(queued);
                try {
                    gtasksSyncService.pushTaskOnSave(task, task.getMergedValues(), invoker, false);
                } catch (IOException e) {
                    handler.handleException("gtasks-sync-io", e); //$NON-NLS-1$
                } finally {
                    callback.incrementProgress(10);
                }
            }
        } finally {
            queued.close();
        }

    }

    @Override
    public void synchronizeList(Object list, final boolean manual, final SyncResultCallback callback) {
        if (!(list instanceof StoreObject))
            return;
        final StoreObject gtasksList = (StoreObject) list;
        if (!GtasksList.TYPE.equals(gtasksList.getValue(StoreObject.TYPE)))
            return;

        callback.started();
        callback.incrementMax(100);

        new Thread(new Runnable() {
            public void run() {
                callback.incrementProgress(50);
                try {
                    String authToken = getValidatedAuthToken();
                    callback.incrementProgress(25);
                    gtasksSyncService.waitUntilEmpty();
                    final GtasksInvoker service = new GtasksInvoker(authToken);
                    synchronizeListHelper(gtasksList, service, manual, null, callback);
                } finally {
                    callback.incrementProgress(25);
                    callback.finished();
                }
            }
        }).start();
    }

    private String getValidatedAuthToken() {
        String authToken = gtasksPreferenceService.getToken();
        try {
            authToken = GtasksTokenValidator.validateAuthToken(ContextManager.getContext(), authToken);
            if (authToken != null)
                gtasksPreferenceService.setToken(authToken);
        } catch (GoogleTasksException e) {
            authToken = null;
        }
        return authToken;
    }


    private synchronized void synchronizeListHelper(StoreObject list, GtasksInvoker invoker,
            boolean manual, SyncExceptionHandler errorHandler, SyncResultCallback callback) {
        String listId = list.getValue(GtasksList.REMOTE_ID);
        long lastSyncDate;
        if (!manual && list.containsNonNullValue(GtasksList.LAST_SYNC)) {
            lastSyncDate = list.getValue(GtasksList.LAST_SYNC);
        } else {
            lastSyncDate = 0;
        }
        boolean includeDeletedAndHidden = lastSyncDate != 0;
        try {
            Tasks taskList = invoker.getAllGtasksFromListId(listId, includeDeletedAndHidden,
                    includeDeletedAndHidden, lastSyncDate);
            List<com.google.api.services.tasks.model.Task> tasks = taskList.getItems();
            if (tasks != null) {
                callback.incrementMax(tasks.size() * 10);
                HashSet<Long> localIds = new HashSet<Long>(tasks.size());
                for (com.google.api.services.tasks.model.Task t : tasks) {
                    GtasksTaskContainer container = parseRemoteTask(t, listId);
                    gtasksMetadataService.findLocalMatch(container);
                    container.gtaskMetadata.setValue(GtasksMetadata.GTASKS_ORDER,
                            Long.parseLong(t.getPosition()));
                    container.gtaskMetadata.setValue(GtasksMetadata.PARENT_TASK,
                            gtasksMetadataService.localIdForGtasksId(t.getParent()));
                    container.gtaskMetadata.setValue(GtasksMetadata.LAST_SYNC,
                            DateUtilities.now() + 1000L);
                    write(container);
                    localIds.add(container.task.getId());
                    callback.incrementProgress(10);
                }
                list.setValue(GtasksList.LAST_SYNC, DateUtilities.now());
                storeObjectDao.persist(list);

                if(lastSyncDate == 0) {
                    Long[] localIdArray = localIds.toArray(new Long[localIds.size()]);
                    Criterion delete = Criterion.and(Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                            GtasksMetadata.LIST_ID.eq(listId),
                            Criterion.not(Metadata.TASK.in(localIdArray)));
                    taskService.deleteWhere(
                            Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).
                                    where(delete)));
                    metadataService.deleteWhere(delete);
                }

                gtasksTaskListUpdater.correctOrderAndIndentForList(listId);
            }
        } catch (IOException e) {
            if (errorHandler != null)
                errorHandler.handleException("gtasks-sync-io", e); //$NON-NLS-1$
        }
    }

    /** Create a task container for the given remote task
     * @throws JSONException */
    private GtasksTaskContainer parseRemoteTask(com.google.api.services.tasks.model.Task remoteTask, String listId) {
        Task task = new Task();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        task.setValue(Task.TITLE, remoteTask.getTitle());
        task.setValue(Task.CREATION_DATE, DateUtilities.now());
        task.setValue(Task.COMPLETION_DATE, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(remoteTask.getCompleted(), 0));
        if (remoteTask.getDeleted() == null || !remoteTask.getDeleted().booleanValue())
            task.setValue(Task.DELETION_DATE, 0L);
        else if (remoteTask.getDeleted().booleanValue())
            task.setValue(Task.DELETION_DATE, DateUtilities.now());
        if (remoteTask.getHidden() != null && remoteTask.getHidden().booleanValue())
            task.setValue(Task.DELETION_DATE, DateUtilities.now());

        long dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue(), 0);
        long createdDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate);
        task.setValue(Task.DUE_DATE, createdDate);
        task.setValue(Task.NOTES, remoteTask.getNotes());

        Metadata gtasksMetadata = GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID);
        gtasksMetadata.setValue(GtasksMetadata.ID, remoteTask.getId());
        gtasksMetadata.setValue(GtasksMetadata.LIST_ID, listId);

        GtasksTaskContainer container = new GtasksTaskContainer(task, metadata,
                gtasksMetadata);
        return container;
    }

    private void write(GtasksTaskContainer task) throws IOException {
        //  merge astrid dates with google dates
        if(task.task.isSaved()) {
            Task local = PluginServices.getTaskService().fetchById(task.task.getId(), Task.DUE_DATE, Task.COMPLETION_DATE);
            if (local == null) {
                task.task.clearValue(Task.ID);
            } else {
                mergeDates(task.task, local);
                if(task.task.isCompleted() && !local.isCompleted())
                    StatisticsService.reportEvent(StatisticsConstants.GTASKS_TASK_COMPLETED);
            }
        } else { // Set default reminders for remotely created tasks
            TaskDao.setDefaultReminders(task.task);
        }
        if (!TextUtils.isEmpty(task.task.getValue(Task.TITLE))) {
            task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            gtasksMetadataService.saveTaskAndMetadata(task);
        }
    }

    private void mergeDates(Task remote, Task local) {
        if(remote.hasDueDate() && local.hasDueTime()) {
            Date newDate = new Date(remote.getValue(Task.DUE_DATE));
            Date oldDate = new Date(local.getValue(Task.DUE_DATE));
            newDate.setHours(oldDate.getHours());
            newDate.setMinutes(oldDate.getMinutes());
            newDate.setSeconds(oldDate.getSeconds());
            long setDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    newDate.getTime());
            remote.setValue(Task.DUE_DATE, setDate);
        }
    }
}
