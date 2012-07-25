/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.files.FileMetadata;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.MetadataService;
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

    @Autowired MetadataService metadataService;

    private final PushQueuedArgs<Task> taskPusher = new PushQueuedArgs<Task>() {
        @Override
        public Task getRemoteModelInstance(TodorooCursor<Task> cursor) {
            return new Task(cursor);
        }

        @Override
        public void pushRemoteModel(Task model) {
            long userId = model.getValue(Task.USER_ID);
            if (userId != Task.USER_ID_SELF && userId != ActFmPreferenceService.userId())
                model.putTransitory(TaskService.TRANS_ASSIGNED, true);
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

    private final PushQueuedArgs<Metadata> filesPusher = new PushQueuedArgs<Metadata>() {

        @Override
        public void pushRemoteModel(Metadata model) {
            long taskId = model.getValue(Metadata.TASK);
            Task localTask = taskService.fetchById(taskId, Task.REMOTE_ID);
            long remoteTaskId = localTask.getValue(Task.REMOTE_ID);

            if (model.getValue(FileMetadata.DELETION_DATE) > 0)
                actFmSyncService.deleteAttachment(model);
            else if (remoteTaskId > 0)
                actFmSyncService.pushAttachment(remoteTaskId, model);
        };

        public Metadata getRemoteModelInstance(TodorooCursor<Metadata> cursor) {
            return new Metadata(cursor);
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

    private static final String LAST_USERS_FETCH_TIME = "actfm_lastUsers";  //$NON-NLS-1$

    // --- synchronize active tasks

    @Override
    public void synchronizeActiveTasks(final boolean manual,
            final SyncResultCallback callback) {

        new Thread(new Runnable() {
            public void run() {
                callback.started();
                callback.incrementMax(140);

                final AtomicInteger finisher = new AtomicInteger(4);

                actFmPreferenceService.recordSyncStart();
                updateUserStatus();

                startUsersFetcher(callback, finisher);

                startTagFetcher(callback, finisher);

                startUpdatesFetcher(manual, callback, finisher);

                actFmSyncService.waitUntilEmpty();
                startTaskFetcher(manual, callback, finisher);

                callback.incrementProgress(50);
            }
        }).start();
    }

    /** fetch user status hash*/
    @SuppressWarnings("nls")
    private void updateUserStatus() {
        try {
            JSONObject status = actFmSyncService.invoke("user_status"); //$NON-NLS-1$
            if (status.has("id"))
                Preferences.setLong(ActFmPreferenceService.PREF_USER_ID, status.optLong("id"));
            if (status.has("name"))
                Preferences.setString(ActFmPreferenceService.PREF_NAME, status.optString("name"));
            if (status.has("first_name"))
                Preferences.setString(ActFmPreferenceService.PREF_FIRST_NAME, status.optString("first_name"));
            if (status.has("last_name"))
                Preferences.setString(ActFmPreferenceService.PREF_LAST_NAME, status.optString("last_name"));
            if (status.has("premium"))
                Preferences.setBoolean(ActFmPreferenceService.PREF_PREMIUM, status.optBoolean("premium"));
            if (status.has("email"))
                Preferences.setString(ActFmPreferenceService.PREF_EMAIL, status.optString("email"));
            if (status.has("picture"))
                Preferences.setString(ActFmPreferenceService.PREF_PICTURE, status.optString("picture"));

            ActFmPreferenceService.reloadThisUser();
        } catch (IOException e) {
            handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
        }
    }

    /** fetch changes to users/friends */
    private void startUsersFetcher(final SyncResultCallback callback,
            final AtomicInteger finisher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = Preferences.getInt(LAST_USERS_FETCH_TIME, 0);
                try {
                    time = actFmSyncService.fetchUsers();
                    Preferences.setInt(LAST_USERS_FETCH_TIME, time);
                } catch (JSONException e) {
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } catch (IOException e) {
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } finally {
                    callback.incrementProgress(20);
                    if(finisher.decrementAndGet() == 0) {
                        actFmPreferenceService.recordSuccessfulSync();
                        callback.finished();
                    }
                }
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
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } catch (IOException e) {
                    handler.handleException("actfm-sync", e, e.toString()); //$NON-NLS-1$
                } finally {
                    callback.incrementProgress(20);
                    if(finisher.decrementAndGet() == 0) {
                        finishSync(callback);
                    }
                }
            }
        }).start();
    }

    /** fetch changes to personal updates and push unpushed updates */
    private void startUpdatesFetcher(final boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        actFmSyncService.fetchPersonalUpdates(manual, new Runnable() { // Also pushes queued updates
            @Override
            public void run() {
                callback.incrementProgress(20);
                if (finisher.decrementAndGet() == 0) {
                    finishSync(callback);
                }
            }
        });

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
                    finishSync(callback);
                }
            }
        });
    }

    private static interface PushQueuedArgs<T extends AbstractModel> {
        public T getRemoteModelInstance(TodorooCursor<T> cursor);
        public void pushRemoteModel(T model);
    }

    private <T extends AbstractModel> void pushQueued(final SyncResultCallback callback, final AtomicInteger finisher,
            TodorooCursor<T> cursor, boolean awaitTermination, final PushQueuedArgs<T> pusher) {
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
                            finishSync(callback);
                        }
                    }
                }
            });
        }
        executor.shutdown();
        if (awaitTermination) {
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
        try {
            pushQueued(callback, finisher, taskCursor, true, taskPusher);
        } finally {
            taskCursor.close();
        }

        if (ActFmPreferenceService.isPremiumUser()) {
            TodorooCursor<Metadata> filesCursor = metadataService.query(Query.select(Metadata.PROPERTIES)
                    .where(Criterion.and(
                            MetadataCriteria.withKey(FileMetadata.METADATA_KEY),
                            Criterion.or(FileMetadata.REMOTE_ID.eq(0), FileMetadata.DELETION_DATE.gt(0)))));
            try {
                pushQueued(callback, finisher, filesCursor, false, filesPusher);
            } finally {
                filesCursor.close();
            }
        }
    }

    private void pushQueuedTags(final SyncResultCallback callback,
            final AtomicInteger finisher, int lastTagSyncTime) {
        TodorooCursor<TagData> tagDataCursor = tagDataService.query(Query.select(TagData.PROPERTIES)
                .where(Criterion.or(
                        TagData.REMOTE_ID.eq(0),
                        Criterion.and(TagData.REMOTE_ID.gt(0),
                                TagData.MODIFICATION_DATE.gt(lastTagSyncTime)))));
        try {
            pushQueued(callback, finisher, tagDataCursor, true, tagPusher);
        } finally {
            tagDataCursor.close();
        }

    }

    // --- synchronize list

    @Override
    public void synchronizeList(Object list, final boolean manual,
            final SyncResultCallback callback) {

        if (list instanceof User) {
            synchronizeUser((User) list, manual, callback);
            return;
        }

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

    private void synchronizeUser(final User user, final boolean manual, final SyncResultCallback callback) {
        if (user.getValue(User.REMOTE_ID) == 0)
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                callback.started();
                callback.incrementMax(100);

                actFmSyncService.waitUntilEmpty();
                actFmSyncService.fetchTasksForUser(user, manual, new Runnable() {
                    public void run() {
                        callback.finished();
                    }
                });
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
        Long[] ids;
        TodorooCursor<Metadata> allTagged = metadataService.query(Query.select(Metadata.TASK).where(Criterion.and(Metadata.KEY.eq(TagService.KEY),
                TagService.TAG.eqCaseInsensitive(tagData.getValue(TagData.NAME)))));
        try {
            ids = new Long[allTagged.getCount()];
            Metadata m = new Metadata();
            int i = 0;
            for (allTagged.moveToFirst(); !allTagged.isAfterLast(); allTagged.moveToNext()) {
                m.readFromCursor(allTagged);
                ids[i] = m.getValue(Metadata.TASK);
                i++;
            }
        } finally {
            allTagged.close();
        }

        TodorooCursor<Task> taskCursor = taskService.query(Query.select(Task.PROPERTIES)
                .join(Join.inner(Metadata.TABLE, Criterion.and(Metadata.KEY.eq(TagService.KEY), Metadata.TASK.eq(Task.ID),
                        TagService.TAG.eqCaseInsensitive(tagData.getValue(TagData.NAME)))))
                .where(Criterion.or(
                        Criterion.and(TaskCriteria.isActive(),
                                Task.REMOTE_ID.isNull()),
                        Criterion.and(Task.REMOTE_ID.isNotNull(),
                                Task.MODIFICATION_DATE.gt(Task.LAST_SYNC)))));
        try {
            pushQueued(callback, finisher, taskCursor, true, taskPusher);
        } finally {
            taskCursor.close();
        }

        TodorooCursor<Metadata> filesCursor = metadataService.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(
                        MetadataCriteria.withKey(FileMetadata.METADATA_KEY),
                        FileMetadata.REMOTE_ID.eq(0),
                        Metadata.TASK.in(ids))));
        try {
            pushQueued(callback, finisher, filesCursor, false, filesPusher);
        } finally {
            filesCursor.close();
        }
    }

}
