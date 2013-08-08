package com.todoroo.astrid.actfm.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.service.NotificationManager.AndroidNotificationManager;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.TaskListMetadataChangesHappened;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.TaskListMetadataOutstandingDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.dao.UserActivityOutstandingDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskListMetadataOutstanding;
import com.todoroo.astrid.data.UserActivity;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ActFmSyncThread {

    private static final String ERROR_TAG = "actfm-sync-thread"; //$NON-NLS-1$

    private final List<ClientToServerMessage<?>> pendingMessages;
    private final Map<ClientToServerMessage<?>, SyncMessageCallback> pendingCallbacks;
    private final Object monitor;
    private Thread thread;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    @Autowired
    private TagDataDao tagDataDao;

    @Autowired
    private TagOutstandingDao tagOutstandingDao;

    @Autowired
    private UserActivityDao userActivityDao;

    @Autowired
    private UserActivityOutstandingDao userActivityOutstandingDao;

    @Autowired
    private TaskListMetadataDao taskListMetadataDao;

    @Autowired
    private TaskListMetadataOutstandingDao taskListMetadataOutstandingDao;

    private boolean syncMigration = false;

    private final NotificationManager notificationManager;

    private int notificationId = -1;

    public static interface SyncMessageCallback {
        public void runOnSuccess();
    }

    public static enum ModelType {
        TYPE_TASK,
        TYPE_TAG,
        TYPE_ACTIVITY,
        TYPE_ATTACHMENT,
        TYPE_TASK_LIST_METADATA
    }

    private static volatile ActFmSyncThread instance;

    public static ActFmSyncThread getInstance() {
        if (instance == null) {
            synchronized (ActFmSyncThread.class) {
                if (instance == null) {
                    initializeSyncComponents(PluginServices.getTaskDao(), PluginServices.getTagDataDao(), PluginServices.getUserActivityDao(),
                            PluginServices.getTaskAttachmentDao(), PluginServices.getTaskListMetadataDao());
                }
            }
        }
        return instance;
    }

    public static void initializeSyncComponents(TaskDao taskDao, TagDataDao tagDataDao, UserActivityDao userActivityDao,
                                                TaskAttachmentDao taskAttachmentDao, TaskListMetadataDao taskListMetadataDao) {
        if (instance == null) {
            synchronized (ActFmSyncThread.class) {
                if (instance == null) {
                    List<ClientToServerMessage<?>> syncQueue = Collections.synchronizedList(new LinkedList<ClientToServerMessage<?>>());
                    ActFmSyncMonitor monitor = ActFmSyncMonitor.getInstance();
                    ActFmSyncWaitingPool waitingPool = ActFmSyncWaitingPool.getInstance();

                    instance = new ActFmSyncThread(syncQueue, monitor);

                    taskDao.addListener(new SyncDatabaseListener<Task>(instance, ModelType.TYPE_TASK));
                    tagDataDao.addListener(new SyncDatabaseListener<TagData>(instance, ModelType.TYPE_TAG));
                    userActivityDao.addListener(new SyncDatabaseListener<UserActivity>(instance, ModelType.TYPE_ACTIVITY));
                    taskAttachmentDao.addListener(new SyncDatabaseListener<TaskAttachment>(instance, ModelType.TYPE_ATTACHMENT));
                    taskListMetadataDao.addListener(new TaskListMetadataSyncDatabaseListener(instance, waitingPool, ModelType.TYPE_TASK_LIST_METADATA));

                    instance.startSyncThread();
                }
            }
        }
    }

    private ActFmSyncThread(List<ClientToServerMessage<?>> messageQueue, Object syncMonitor) {
        DependencyInjectionService.getInstance().inject(this);
        this.pendingMessages = messageQueue;
        this.pendingCallbacks = Collections.synchronizedMap(new HashMap<ClientToServerMessage<?>, SyncMessageCallback>());
        this.monitor = syncMonitor;
        this.syncMigration = Preferences.getBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
        this.notificationManager = new AndroidNotificationManager(ContextManager.getContext());
    }

    public synchronized void startSyncThread() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    sync();
                }
            });
            thread.start();
        }
    }

    public static void clearTablePushedAtValues() {
        String[] pushedAtPrefs = new String[]{NameMaps.PUSHED_AT_TASKS, NameMaps.PUSHED_AT_TAGS, NameMaps.PUSHED_AT_ACTIVITY,
                NameMaps.PUSHED_AT_USERS, NameMaps.PUSHED_AT_TASK_LIST_METADATA};
        for (String key : pushedAtPrefs) {
            Preferences.clear(key);
        }
    }

    public synchronized void enqueueMessage(ClientToServerMessage<?> message, SyncMessageCallback callback) {
        if (!RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_ENQUEUE_MESSAGES)) {
            return;
        }
        if (!pendingMessages.contains(message)) {
            pendingMessages.add(message);
            if (callback != null) {
                pendingCallbacks.put(message, callback);
            }
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    public synchronized void setTimeForBackgroundSync(boolean isTimeForBackgroundSync) {
        if (isTimeForBackgroundSync) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    public static final SyncMessageCallback DEFAULT_REFRESH_RUNNABLE = new SyncMessageCallback() {
        @Override
        public void runOnSuccess() {
            Intent refresh = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(refresh);
        }
    };

    private void sync() {
        try {
            while (true) {
                synchronized (monitor) {
                    while (true) {
                        try {
                            if (notificationId >= 0) {
                                notificationManager.cancel(notificationId);
                                notificationId = -1;
                            }
                            monitor.wait();
                            AndroidUtilities.sleepDeep(500L); // Wait briefly for large database operations to finish (e.g. adding a task with several tags may trigger a message before all saves are done--fix this?)

                            if (!syncMigration) {
                                syncMigration = Preferences.getBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
                            }
                        } catch (InterruptedException e) {
                            // Ignored
                        }
                    }
                }
            }
        } catch (Exception e) {
            // In the worst case, restart thread if something goes wrong
            Log.e(ERROR_TAG, "Unexpected sync thread exception", e);
            thread = null;
            startSyncThread();
        }
    }

    public void repopulateQueueFromOutstandingTables() {
        constructChangesHappenedFromOutstandingTable(Task.class, taskDao, taskOutstandingDao);
        constructChangesHappenedFromOutstandingTable(TagData.class, tagDataDao, tagOutstandingDao);
        constructChangesHappenedFromOutstandingTable(UserActivity.class, userActivityDao, userActivityOutstandingDao);
        constructChangesHappenedForTaskListMetadata(taskListMetadataDao, taskListMetadataOutstandingDao);
    }

    private <T extends RemoteModel, OE extends OutstandingEntry<T>> void constructChangesHappenedFromOutstandingTable(Class<T> modelClass, RemoteModelDao<T> modelDao, OutstandingEntryDao<OE> oustandingDao) {
        TodorooCursor<OE> outstanding = oustandingDao.query(Query.select(OutstandingEntry.ENTITY_ID_PROPERTY).groupBy(OutstandingEntry.ENTITY_ID_PROPERTY));
        try {
            for (outstanding.moveToFirst(); !outstanding.isAfterLast(); outstanding.moveToNext()) {
                Long id = outstanding.get(OutstandingEntry.ENTITY_ID_PROPERTY);
                enqueueMessage(new ChangesHappened<T, OE>(id, modelClass, modelDao, oustandingDao), null);
            }
        } finally {
            outstanding.close();
        }
    }

    private void constructChangesHappenedForTaskListMetadata(TaskListMetadataDao dao, TaskListMetadataOutstandingDao outstandingDao) {
        TodorooCursor<TaskListMetadataOutstanding> outstanding = outstandingDao.query(Query.select(OutstandingEntry.ENTITY_ID_PROPERTY).groupBy(OutstandingEntry.ENTITY_ID_PROPERTY));
        try {
            for (outstanding.moveToFirst(); !outstanding.isAfterLast(); outstanding.moveToNext()) {
                Long id = outstanding.get(OutstandingEntry.ENTITY_ID_PROPERTY);
                ActFmSyncWaitingPool.getInstance().enqueueMessage(new TaskListMetadataChangesHappened(id, TaskListMetadata.class, dao, outstandingDao));
            }
        } finally {
            outstanding.close();
        }
    }

    public static class NetworkStateChangedReceiver extends BroadcastReceiver {
        private static long lastSyncFromNetworkChange = 0;
        private static final String PREF_LAST_SYNC_FROM_NETWORK_CHANGE = "p_last_sync_from_net_change"; //$NON-NLS-1$

        @Override
        public void onReceive(Context context, Intent intent) {
            lastSyncFromNetworkChange = Preferences.getLong(PREF_LAST_SYNC_FROM_NETWORK_CHANGE, 0L);
            if (DateUtilities.now() - lastSyncFromNetworkChange > DateUtilities.ONE_MINUTE * 10) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null && NetworkInfo.State.CONNECTED.equals(info.getState()) && PluginServices.getActFmPreferenceService().isLoggedIn()) {
                    ActFmSyncThread syncThread = ActFmSyncThread.getInstance();
                    syncThread.repopulateQueueFromOutstandingTables();
                    Preferences.setLong(PREF_LAST_SYNC_FROM_NETWORK_CHANGE, DateUtilities.now());
                }
            }
        }
    }
}
