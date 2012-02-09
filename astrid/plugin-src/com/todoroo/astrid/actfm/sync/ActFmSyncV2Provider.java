/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StartupService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;
import com.todoroo.astrid.tags.TagService;

/**
 * Exposes sync action
 *
 */
public class ActFmSyncV2Provider extends SyncV2Provider {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired TaskService taskService;

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
    public void synchronizeActiveTasks(boolean manual,
            final SyncResultCallback callback) {

        callback.started();
        callback.incrementMax(100);

        final AtomicInteger finisher = new AtomicInteger(2);

        startTagFetcher(callback, finisher);

        startTaskFetcher(manual, callback, finisher);

        callback.incrementProgress(50);
    }

    /** fetch changes to tags */
    private void startTagFetcher(final SyncResultCallback callback,
            final AtomicInteger finisher) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = Preferences.getInt(LAST_TAG_FETCH_TIME, 0);
                try {
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
                pushQueued(callback, finisher);

                callback.incrementProgress(30);
                if(finisher.decrementAndGet() == 0) {
                    actFmPreferenceService.recordSuccessfulSync();
                    callback.finished();
                }
            }
        });
    }

    private void pushQueued(final SyncResultCallback callback,
            final AtomicInteger finisher) {
        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.PROPERTIES).
                where(Criterion.or(
                        Criterion.and(TaskCriteria.isActive(),
                                Task.ID.gt(StartupService.INTRO_TASK_SIZE),
                                Task.REMOTE_ID.eq(0)),
                        Criterion.and(Task.REMOTE_ID.gt(0),
                                Task.MODIFICATION_DATE.gt(Task.LAST_SYNC)))));

        try {
            callback.incrementMax(cursor.getCount() * 20);
            finisher.addAndGet(cursor.getCount());

            for(int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                final Task task = new Task(cursor);

                new Thread(new Runnable() {
                    public void run() {
                        try {
                            actFmSyncService.pushTaskOnSave(task, task.getMergedValues());
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
        } finally {
            cursor.close();
        }
    }

    // --- synchronize list

    @Override
    public void synchronizeList(Object list, boolean manual,
            final SyncResultCallback callback) {

        if(!(list instanceof TagData))
            return;

        TagData tagData = (TagData) list;
        final boolean noRemoteId = tagData.getValue(TagData.REMOTE_ID) == 0;

        if(noRemoteId && !manual)
            return;

        callback.started();
        callback.incrementMax(100);

        final AtomicInteger finisher = new AtomicInteger(3);

        fetchTagData(tagData, noRemoteId, manual, callback, finisher);

        if(!noRemoteId) {
            fetchTasksForTag(tagData, manual, callback, finisher);
            fetchUpdatesForTag(tagData, manual, callback, finisher);
        }

        callback.incrementProgress(50);
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

    private void fetchUpdatesForTag(TagData tagData, boolean manual, final SyncResultCallback callback,
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

    private void fetchTasksForTag(TagData tagData, boolean manual, final SyncResultCallback callback,
            final AtomicInteger finisher) {
        actFmSyncService.fetchTasksForTag(tagData, manual, new Runnable() {
            @Override
            public void run() {
                callback.incrementProgress(30);
                if(finisher.decrementAndGet() == 0)
                    callback.finished();
            }
        });
    }

}
