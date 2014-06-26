/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import android.content.Context;
import android.text.TextUtils;

import com.google.api.services.tasks.model.Tasks;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
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
import com.todoroo.astrid.gtasks.api.GtasksInvoker;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncExecutor;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.tasks.date.DateTimeUtils.newDate;

@Singleton
public class GtasksSyncV2Provider extends SyncV2Provider {

    private static final Logger log = LoggerFactory.getLogger(GtasksSyncV2Provider.class);

    private final TaskService taskService;
    private final StoreObjectDao storeObjectDao;
    private final GtasksPreferenceService gtasksPreferenceService;
    private final GtasksSyncService gtasksSyncService;
    private final GtasksListService gtasksListService;
    private final GtasksMetadataService gtasksMetadataService;
    private final GtasksTaskListUpdater gtasksTaskListUpdater;
    private final Context context;
    private final Preferences preferences;
    private final GtasksTokenValidator gtasksTokenValidator;
    private final GtasksMetadata gtasksMetadataFactory;
    private final SyncExecutor executor;

    @Inject
    public GtasksSyncV2Provider(TaskService taskService, StoreObjectDao storeObjectDao, GtasksPreferenceService gtasksPreferenceService,
                                GtasksSyncService gtasksSyncService, GtasksListService gtasksListService, GtasksMetadataService gtasksMetadataService,
                                GtasksTaskListUpdater gtasksTaskListUpdater, @ForApplication Context context, Preferences preferences,
                                GtasksTokenValidator gtasksTokenValidator, GtasksMetadata gtasksMetadata, SyncExecutor executor) {
        this.taskService = taskService;
        this.storeObjectDao = storeObjectDao;
        this.gtasksPreferenceService = gtasksPreferenceService;
        this.gtasksSyncService = gtasksSyncService;
        this.gtasksListService = gtasksListService;
        this.gtasksMetadataService = gtasksMetadataService;
        this.gtasksTaskListUpdater = gtasksTaskListUpdater;
        this.context = context;
        this.preferences = preferences;
        this.gtasksTokenValidator = gtasksTokenValidator;
        this.gtasksMetadataFactory = gtasksMetadata;
        this.executor = executor;
    }

    @Override
    public String getName() {
        return context.getString(R.string.gtasks_GPr_header);
    }

    @Override
    public GtasksPreferenceService getUtilities() {
        return gtasksPreferenceService;
    }

    public void signOut() {
        gtasksPreferenceService.clearLastSyncDate();
        gtasksPreferenceService.setToken(null);
        gtasksPreferenceService.setUserName(null);
        gtasksMetadataService.clearMetadata();
    }

    @Override
    public boolean isActive() {
        return gtasksPreferenceService.isLoggedIn();
    }

    @Override
    public void synchronizeActiveTasks(final SyncResultCallback callback) {
        callback.started();

        gtasksPreferenceService.recordSyncStart();

        executor.execute(callback, new Runnable() {
            @Override
            public void run() {
                String authToken = getValidatedAuthToken();
                final GtasksInvoker invoker = new GtasksInvoker(gtasksTokenValidator, authToken);
                try {
                    gtasksListService.updateLists(invoker.allGtaskLists());
                } catch (IOException e) {
                    handler.handleException("gtasks-sync=io", e); //$NON-NLS-1$
                }

                StoreObject[] lists = gtasksListService.getLists();
                if (lists.length == 0) {
                    finishSync(callback);
                    return;
                }

                final AtomicInteger finisher = new AtomicInteger(lists.length);

                for (final StoreObject list : lists) {
                    executor.execute(callback, new Runnable() {
                        @Override
                        public void run() {
                            synchronizeListHelper(list, invoker, handler);
                            if (finisher.decrementAndGet() == 0) {
                                pushUpdated(invoker);
                                finishSync(callback);
                            }
                        }
                    });
                }
            }
        });
    }

    private synchronized void pushUpdated(GtasksInvoker invoker) {
        TodorooCursor<Task> queued = taskService.query(Query.select(Task.PROPERTIES).
                join(Join.left(Metadata.TABLE, Criterion.and(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY), Task.ID.eq(Metadata.TASK)))).where(
                        Criterion.or(Task.MODIFICATION_DATE.gt(GtasksMetadata.LAST_SYNC),
                                Criterion.and(Task.USER_ID.neq(Task.USER_ID_SELF), GtasksMetadata.ID.isNotNull()), // XXX: Shouldn't this neq("")?
                                      Metadata.KEY.isNull())));
        pushTasks(queued, invoker);
    }

    private synchronized void pushTasks(TodorooCursor<Task> queued, GtasksInvoker invoker) {
        try {
            Task task = new Task();
            for (queued.moveToFirst(); !queued.isAfterLast(); queued.moveToNext()) {
                task.readFromCursor(queued);
                try {
                    gtasksSyncService.pushTaskOnSave(task, task.getMergedValues(), invoker);
                } catch (IOException e) {
                    handler.handleException("gtasks-sync-io", e); //$NON-NLS-1$
                }
            }
        } finally {
            queued.close();
        }
    }

    @Override
    public void synchronizeList(Object list, final SyncResultCallback callback) {
        if (!(list instanceof StoreObject)) {
            return;
        }
        final StoreObject gtasksList = (StoreObject) list;
        if (!GtasksList.TYPE.equals(gtasksList.getType())) {
            return;
        }

        callback.started();

        executor.execute(callback, new Runnable() {
            @Override
            public void run() {
                try {
                    String authToken = getValidatedAuthToken();
                    gtasksSyncService.waitUntilEmpty();
                    final GtasksInvoker service = new GtasksInvoker(gtasksTokenValidator, authToken);
                    synchronizeListHelper(gtasksList, service, null);
                } finally {
                    callback.finished();
                }
            }
        });
    }

    private String getValidatedAuthToken() {
        String authToken = gtasksPreferenceService.getToken();
        try {
            authToken = gtasksTokenValidator.validateAuthToken(context, authToken);
            if (authToken != null) {
                gtasksPreferenceService.setToken(authToken);
            }
        } catch (GoogleTasksException e) {
            log.error(e.getMessage(), e);
            authToken = null;
        }
        return authToken;
    }

    private synchronized void synchronizeListHelper(StoreObject list, GtasksInvoker invoker,
            SyncExceptionHandler errorHandler) {
        String listId = list.getValue(GtasksList.REMOTE_ID);
        long lastSyncDate = 0;
        if (list.containsNonNullValue(GtasksList.LAST_SYNC)) {
            lastSyncDate = list.getValue(GtasksList.LAST_SYNC);
        }

        /**
         * Find tasks which have been associated with the list internally, but have not yet been
         * pushed to Google Tasks (and so haven't yet got a valid ID).
         */
        Criterion not_pushed_tasks = Criterion.and(
                Metadata.KEY.eq(GtasksMetadata.METADATA_KEY),
                GtasksMetadata.LIST_ID.eq(listId),
                GtasksMetadata.ID.eq("")
        );
        TodorooCursor<Task> qs = taskService.query(Query.select(Task.PROPERTIES).
                join(Join.left(Metadata.TABLE, Criterion.and(MetadataCriteria.withKey(GtasksMetadata.METADATA_KEY), Task.ID.eq(Metadata.TASK)))).where(not_pushed_tasks)
        );
        pushTasks(qs, invoker);

        boolean includeDeletedAndHidden = lastSyncDate != 0;
        try {
            Tasks taskList = invoker.getAllGtasksFromListId(listId, includeDeletedAndHidden,
                    includeDeletedAndHidden, lastSyncDate);
            List<com.google.api.services.tasks.model.Task> tasks = taskList.getItems();
            if (tasks != null) {
                for (com.google.api.services.tasks.model.Task t : tasks) {
                    GtasksTaskContainer container = new GtasksTaskContainer(t, listId, gtasksMetadataFactory.createEmptyMetadata(AbstractModel.NO_ID));
                    gtasksMetadataService.findLocalMatch(container);
                    container.gtaskMetadata.setValue(GtasksMetadata.GTASKS_ORDER, Long.parseLong(t.getPosition()));
                    container.gtaskMetadata.setValue(GtasksMetadata.PARENT_TASK, gtasksMetadataService.localIdForGtasksId(t.getParent()));
                    container.gtaskMetadata.setValue(GtasksMetadata.LAST_SYNC, DateUtilities.now() + 1000L);
                    write(container);
                }
                list.setValue(GtasksList.LAST_SYNC, DateUtilities.now());
                storeObjectDao.persist(list);
                gtasksTaskListUpdater.correctOrderAndIndentForList(listId);
            }
        } catch (IOException e) {
            if (errorHandler != null) {
                errorHandler.handleException("gtasks-sync-io", e); //$NON-NLS-1$
            } else {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void write(GtasksTaskContainer task) {
        //  merge astrid dates with google dates

        if(task.task.isSaved()) {
            Task local = taskService.fetchById(task.task.getId(), Task.DUE_DATE, Task.COMPLETION_DATE);
            if (local == null) {
                task.task.clearValue(Task.ID);
                task.task.clearValue(Task.UUID);
            } else {
                mergeDates(task.task, local);
            }
        } else { // Set default importance and reminders for remotely created tasks
            task.task.setImportance(preferences.getIntegerFromString(
                    R.string.p_default_importance_key, Task.IMPORTANCE_SHOULD_DO));
            TaskDao.setDefaultReminders(preferences, task.task);
        }
        if (!TextUtils.isEmpty(task.task.getTitle())) {
            task.task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            gtasksMetadataService.saveTaskAndMetadata(task);
        }
    }

    private void mergeDates(Task remote, Task local) {
        if(remote.hasDueDate() && local.hasDueTime()) {
            Date newDate = newDate(remote.getDueDate());
            Date oldDate = newDate(local.getDueDate());
            newDate.setHours(oldDate.getHours());
            newDate.setMinutes(oldDate.getMinutes());
            newDate.setSeconds(oldDate.getSeconds());
            long setDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    newDate.getTime());
            remote.setDueDate(setDate);
        }
    }
}
