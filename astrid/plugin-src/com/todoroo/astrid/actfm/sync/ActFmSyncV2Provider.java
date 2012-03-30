/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;
import com.todoroo.astrid.tags.TagService;

/**
 * Exposes sync action
 *
 */
public class ActFmSyncV2Provider extends SyncV2Provider {

    private static final int NUM_THREADS = 20;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired TaskService taskService;

    @Autowired TagDataService tagDataService;

    private final PushQueuedArgs<Task> taskPusher = new PushQueuedArgs<Task>() {
        @Override
        public Task getRemoteModelInstance(TodorooCursor<Task> cursor) {
            return new Task(cursor);
        }

        @Override
        public void pushRemoteModel(Task model) {
            actFmSyncService.pushTaskOnSave(model, model.getMergedValues());
        }

    };

    private final PushQueuedArgs<TagData> tagPusher = new PushQueuedArgs<TagData>() {

        @Override
        public void pushRemoteModel(TagData model) {
            actFmSyncService.pushTagDataOnSave(model, model.getMergedValues());
        }

        @Override
        public TagData getRemoteModelInstance(
                TodorooCursor<TagData> cursor) {
            return new TagData(cursor);
        }
    };

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    public String getName() {
        return ContextManager.getString(R.string.actfm_APr_header);
    }

    @Override
    public ActFmPreferenceService getUtilities() {
        return actFmPreferenceService;
    }

    @Override
    public void signOut() {
        actFmPreferenceService.setToken(null);
        actFmPreferenceService.clearLastSyncDate();
        C2DMReceiver.unregister();
    }

    @Override
    public boolean isActive() {
        return actFmPreferenceService.isLoggedIn();
    }

    private static final String LAST_TAG_FETCH_TIME = "actfm_lastTag"; //$NON-NLS-1$

    // --- synchronize active tasks

    @Override
    public void synchronizeActiveTasks(final boolean manual,
            final SyncResultCallback callback) {

        new Thread(new Runnable() {
            public void run() {
                callback.started();
                callback.incrementMax(100);

                final AtomicInteger finisher = new AtomicInteger(2);

                actFmPreferenceService.recordSyncStart();

                startTagFetcher(callback, finisher);

                actFmSyncService.waitUntilEmpty();
                startTaskFetcher(manual, callback, finisher);

                callback.incrementProgress(50);
            }
        }).start();
    }

    /** fetch changes to tags */
    private void startTagFetcher(final SyncResultCallback callback,
            final AtomicInteger finisher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = Preferences.getInt(LAST_TAG_FETCH_TIME, 0);
                try {
                    pushQueuedTags(callback, finisher, time);
                    time = actFmSyncService.fetchTags(time);
                    Preferences.setInt(LAST_TAG_FETCH_TIME, time);
                } catch (JSONException e) {
                    handler.handleException("actfm-sync", e); //$NON-NLS-1$
                } catch (IOException e) {
                    handler.handleException("actfm-sync", e); //$NON-NLS-1$
                } finally {
                    callback.incrementProgress(20);
                    if(finisher.decrementAndGet() == 0) {
                        actFmPreferenceService.recordSuccessfulSync();
                        actFmPreferenceService.stopOngoing();
                        callback.finished();
                    }
                }
            }
        }).start();
    }

    /** @return runnable to fetch changes to tags */
    private void startTaskFetcher(final boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        actFmSyncService.fetchActiveTasks(manual, handler, new Runnable() {
            @Override
            public void run() {
                pushQueuedTasks(callback, finisher);

                callback.incrementProgress(30);
                if(finisher.decrementAndGet() == 0) {
                    actFmPreferenceService.recordSuccessfulSync();
                    actFmPreferenceService.stopOngoing();
                    callback.finished();
                }
            }
        });
    }

    private static interface PushQueuedArgs<T extends RemoteModel> {
        public T getRemoteModelInstance(TodorooCursor<T> cursor);
        public void pushRemoteModel(T model);
    }

    private <T extends RemoteModel> void pushQueued(final SyncResultCallback callback, final AtomicInteger finisher,
            TodorooCursor<T> cursor, boolean awaitTermination, final PushQueuedArgs<T> pusher) {
        try {
            callback.incrementMax(cursor.getCount() * 20);
            finisher.addAndGet(cursor.getCount());

            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            for(int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                final T model = pusher.getRemoteModelInstance(cursor);

                executor.submit(new Runnable() {
                    public void run() {
                        try {
                            pusher.pushRemoteModel(model);
                        } finally {
                            callback.incrementProgress(20);
                            if(finisher.decrementAndGet() == 0) {
                                actFmPreferenceService.recordSuccessfulSync();
                                actFmPreferenceService.stopOngoing();
                                callback.finished();
                            }
                        }
                    }
                });
            }
            executor.shutdown();
            if (awaitTermination)
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        } finally {
            cursor.close();
        }
    }

    private void pushQueuedTasks(final SyncResultCallback callback,
            final AtomicInteger finisher) {
        TodorooCursor<Task> taskCursor = taskService.query(Query.select(Task.PROPERTIES).
                where(Criterion.or(
                        Criterion.and(TaskCriteria.isActive(),
                                Task.REMOTE_ID.isNull()),
                                Criterion.and(Task.REMOTE_ID.isNotNull(),
                                        Task.MODIFICATION_DATE.gt(Task.LAST_SYNC)))));

        pushQueued(callback, finisher, taskCursor, false, taskPusher);
    }

    private void pushQueuedTags(final SyncResultCallback callback,
            final AtomicInteger finisher, int lastTagSyncTime) {
        TodorooCursor<TagData> tagDataCursor = tagDataService.query(Query.select(TagData.PROPERTIES)
                .where(Criterion.or(
                        TagData.REMOTE_ID.eq(0),
                        Criterion.and(TagData.REMOTE_ID.gt(0),
                                TagData.MODIFICATION_DATE.gt(lastTagSyncTime)))));

        pushQueued(callback, finisher, tagDataCursor, true, tagPusher);

    }

    // --- synchronize list

    @Override
    public void synchronizeList(Object list, final boolean manual,
            final SyncResultCallback callback) {

        if(!(list instanceof TagData))
            return;

        final TagData tagData = (TagData) list;
        final boolean noRemoteId = tagData.getValue(TagData.REMOTE_ID) == 0;

        new Thread(new Runnable() {
            public void run() {
                callback.started();
                callback.incrementMax(100);

                final AtomicInteger finisher = new AtomicInteger(3);

                fetchTagData(tagData, noRemoteId, manual, callback, finisher);

                if(!noRemoteId) {
                    actFmSyncService.waitUntilEmpty();
                    fetchTasksForTag(tagData, manual, callback, finisher);
                    fetchUpdatesForTag(tagData, manual, callback, finisher);
                }

                callback.incrementProgress(50);
            }
        }).start();
    }

    private void fetchTagData(final TagData tagData, final boolean noRemoteId,
            final boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String oldName = tagData.getValue(TagData.NAME);
                try {
                    actFmSyncService.fetchTag(tagData);

                    if(noRemoteId) {
                        fetchTasksForTag(tagData, manual, callback, finisher);
                        fetchUpdatesForTag(tagData, manual, callback, finisher);
                    }

                    if(!oldName.equals(tagData.getValue(TagData.NAME))) {
                        TagService.getInstance().rename(oldName,
                                tagData.getValue(TagData.NAME));
                    }
                } catch (IOException e) {
                    exceptionService.reportError("sync-io", e); //$NON-NLS-1$
                } catch (JSONException e) {
                    exceptionService.reportError("sync-json", e); //$NON-NLS-1$
                } finally {
                    callback.incrementProgress(20);
                    if(finisher.decrementAndGet() == 0)
                        callback.finished();
                }
            }
        }).start();
    }

    private void fetchUpdatesForTag(final TagData tagData, boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        actFmSyncService.fetchUpdatesForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                callback.incrementProgress(20);
                if(finisher.decrementAndGet() == 0)
                    callback.finished();
            }
        });
    }

    private void fetchTasksForTag(final TagData tagData, boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        actFmSyncService.fetchTasksForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                pushQueuedTasksByTag(tagData, callback, finisher);

                callback.incrementProgress(30);
                if(finisher.decrementAndGet() == 0)
                    callback.finished();
            }
        });
    }

    private void pushQueuedTasksByTag(TagData tagData, SyncResultCallback callback, AtomicInteger finisher) {
        TodorooCursor<Task> taskCursor = taskService.query(Query.select(Task.PROPERTIES)
                .join(Join.inner(Metadata.TABLE, Criterion.and(Metadata.KEY.eq(TagService.KEY), Metadata.TASK.eq(Task.ID), TagService.TAG.eq(tagData.getId()))))
                .where(Criterion.or(
                        Criterion.and(TaskCriteria.isActive(),
                                Task.REMOTE_ID.isNull()),
                        Criterion.and(Task.REMOTE_ID.isNotNull(),
                                Task.MODIFICATION_DATE.gt(Task.LAST_SYNC)))));
        pushQueued(callback, finisher, taskCursor, false, taskPusher);
    }

}
