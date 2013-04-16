package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.timsu.astrid.R;
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
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.actfm.sync.messages.JSONPayloadBuilder;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.ReplayOutstandingEntries;
import com.todoroo.astrid.actfm.sync.messages.ReplayTaskListMetadataOutstanding;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
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
import com.todoroo.astrid.dao.WaitingOnMeDao;
import com.todoroo.astrid.dao.WaitingOnMeOutstandingDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskListMetadata;
import com.todoroo.astrid.data.TaskListMetadataOutstanding;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.data.User;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.data.WaitingOnMe;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget;

public class ActFmSyncThread {

    private static final String ERROR_TAG = "actfm-sync-thread"; //$NON-NLS-1$

    private final List<ClientToServerMessage<?>> pendingMessages;
    private final Map<ClientToServerMessage<?>, SyncMessageCallback> pendingCallbacks;
    private final Object monitor;
    private Thread thread;

    @Autowired
    private ActFmInvoker actFmInvoker;

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

    @Autowired
    private WaitingOnMeDao waitingOnMeDao;

    @Autowired
    private WaitingOnMeOutstandingDao waitingOnMeOutstandingDao;

    private String token;

    private boolean syncMigration = false;

    private boolean isTimeForBackgroundSync = false;

    private final NotificationManager notificationManager;

    private int notificationId = -1;

    public static interface SyncMessageCallback {
        public void runOnSuccess();
        public void runOnErrors(List<JSONArray> errors);
    }

    public static enum ModelType {
        TYPE_TASK,
        TYPE_TAG,
        TYPE_ACTIVITY,
        TYPE_ATTACHMENT,
        TYPE_TASK_LIST_METADATA,
        TYPE_WAITING_ON_ME
    }

    private static volatile ActFmSyncThread instance;

    public static ActFmSyncThread getInstance() {
        if (instance == null) {
            synchronized(ActFmSyncThread.class) {
                if (instance == null) {
                    initializeSyncComponents(PluginServices.getTaskDao(), PluginServices.getTagDataDao(), PluginServices.getUserActivityDao(),
                            PluginServices.getTaskAttachmentDao(), PluginServices.getTaskListMetadataDao(), PluginServices.getWaitingOnMeDao());
                }
            }
        }
        return instance;
    }

    public static ActFmSyncThread initializeSyncComponents(TaskDao taskDao, TagDataDao tagDataDao, UserActivityDao userActivityDao,
            TaskAttachmentDao taskAttachmentDao, TaskListMetadataDao taskListMetadataDao, WaitingOnMeDao waitingOnMeDao) {
        if (instance == null) {
            synchronized(ActFmSyncThread.class) {
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
                    waitingOnMeDao.addListener(new SyncDatabaseListener<WaitingOnMe>(instance, ModelType.TYPE_WAITING_ON_ME));

                    instance.startSyncThread();
                }
            }
        }
        return instance;
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
        String[] pushedAtPrefs = new String[] { NameMaps.PUSHED_AT_TASKS, NameMaps.PUSHED_AT_TAGS, NameMaps.PUSHED_AT_ACTIVITY,
                NameMaps.PUSHED_AT_USERS, NameMaps.PUSHED_AT_TASK_LIST_METADATA, NameMaps.PUSHED_AT_WAITING_ON_ME };
        for (String key : pushedAtPrefs)
            Preferences.clear(key);
    }

    public synchronized void enqueueMessage(ClientToServerMessage<?> message, SyncMessageCallback callback) {
        if (!RemoteModelDao.getOutstandingEntryFlag(RemoteModelDao.OUTSTANDING_ENTRY_FLAG_ENQUEUE_MESSAGES))
            return;
        if (!pendingMessages.contains(message)) {
            pendingMessages.add(message);
            if (callback != null)
                pendingCallbacks.put(message, callback);
            synchronized(monitor) {
                monitor.notifyAll();
            }
        }
    }

    public synchronized void setTimeForBackgroundSync(boolean isTimeForBackgroundSync) {
        this.isTimeForBackgroundSync = isTimeForBackgroundSync;
        if (isTimeForBackgroundSync)
            synchronized (monitor) {
                monitor.notifyAll();
            }
    }

    public static final SyncMessageCallback DEFAULT_REFRESH_RUNNABLE = new SyncMessageCallback() {
        @Override
        public void runOnSuccess() {
            Intent refresh = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(refresh);
        }

        @Override
        public void runOnErrors(List<JSONArray> errors) {/**/}
    };

    @SuppressWarnings("nls")
    private void sync() {
        try {
            int batchSize = 4;
            List<ClientToServerMessage<?>> messageBatch = new ArrayList<ClientToServerMessage<?>>();
            while(true) {
                synchronized(monitor) {
                    while ((pendingMessages.isEmpty() && !timeForBackgroundSync()) || !actFmPreferenceService.isLoggedIn() || !syncMigration) {
                        try {
                            if ((pendingMessages.isEmpty() || !actFmPreferenceService.isLoggedIn()) && notificationId >= 0) {
                                notificationManager.cancel(notificationId);
                                notificationId = -1;
                            }
                            monitor.wait();
                            AndroidUtilities.sleepDeep(500L); // Wait briefly for large database operations to finish (e.g. adding a task with several tags may trigger a message before all saves are done--fix this?)

                            if (!syncMigration)
                                syncMigration = Preferences.getBoolean(AstridNewSyncMigrator.PREF_SYNC_MIGRATION, false);
                        } catch (InterruptedException e) {
                            // Ignored
                        }
                    }
                }

                boolean recordSyncSuccess = true;
                if (timeForBackgroundSync()) {
                    repopulateQueueFromOutstandingTables();
                    enqueueMessage(BriefMe.instantiateBriefMeForClass(TaskListMetadata.class, NameMaps.PUSHED_AT_TASK_LIST_METADATA), DEFAULT_REFRESH_RUNNABLE);
                    enqueueMessage(BriefMe.instantiateBriefMeForClass(Task.class, NameMaps.PUSHED_AT_TASKS), DEFAULT_REFRESH_RUNNABLE);
                    enqueueMessage(BriefMe.instantiateBriefMeForClass(TagData.class, NameMaps.PUSHED_AT_TAGS), DEFAULT_REFRESH_RUNNABLE);
                    enqueueMessage(BriefMe.instantiateBriefMeForClass(User.class, NameMaps.PUSHED_AT_USERS), DEFAULT_REFRESH_RUNNABLE);
                    enqueueMessage(BriefMe.instantiateBriefMeForClass(WaitingOnMe.class, NameMaps.PUSHED_AT_WAITING_ON_ME), DEFAULT_REFRESH_RUNNABLE);
                    setTimeForBackgroundSync(false);
                }

                while (messageBatch.size() < batchSize && !pendingMessages.isEmpty()) {
                    ClientToServerMessage<?> message = pendingMessages.remove(0);
                    if (message != null)
                        messageBatch.add(message);
                }

                if (!messageBatch.isEmpty() && checkForToken()) {
                    JSONPayloadBuilder payload = new JSONPayloadBuilder();
                    MultipartEntity entity = new MultipartEntity();
                    boolean containsChangesHappened = false;
                    for (int i = 0; i < messageBatch.size(); i++) {
                        ClientToServerMessage<?> message = messageBatch.get(i);
                        boolean success = payload.addMessage(message, entity);
                        if (success) {
                            if (message instanceof ChangesHappened)
                                containsChangesHappened = true;
                        } else {
                            messageBatch.remove(i);
                            i--;
                        }

                    }

                    if (payload.getMessageCount() == 0) {
                        messageBatch.clear();
                        continue;
                    }

                    setupNotification();

                    payload.addJSONObject(getClientVersion());

                    JSONArray errors = null;
                    try {
                        JSONObject response = actFmInvoker.postSync(payload.closeAndReturnString(), entity, containsChangesHappened, token);
                        // process responses
                        String time = response.optString("time");
                        JSONArray serverMessagesJson = response.optJSONArray("messages");
                        if (serverMessagesJson != null) {
                            setWidgetSuppression(true);
                            for (int i = 0; i < serverMessagesJson.length(); i++) {
                                JSONObject serverMessageJson = serverMessagesJson.optJSONObject(i);
                                if (serverMessageJson != null) {
                                    ServerToClientMessage serverMessage = ServerToClientMessage.instantiateMessage(serverMessageJson);
                                    if (serverMessage != null) {
                                        serverMessage.processMessage(time);
                                    } else {
                                        syncLog("Index " + i + " unable to instantiate message " + serverMessageJson.toString());
                                    }
                                }
                            }
                            errors = response.optJSONArray("errors");
                            boolean errorsExist = (errors != null && errors.length() > 0);
                            replayOutstandingChanges(errorsExist);
                            setWidgetSuppression(false);
                        }

                        batchSize = Math.max(12, Math.min(batchSize, messageBatch.size()) * 2);

                        if (recordSyncSuccess) {
                            actFmPreferenceService.setLastError(null, null);
                            actFmPreferenceService.recordSuccessfulSync();
                        }
                    } catch (IOException e) {
                        Log.e(ERROR_TAG, "IOException", e);
                        batchSize = Math.max(batchSize / 2, 1);
                    }

                    Set<SyncMessageCallback> callbacksExecutedThisLoop = new HashSet<SyncMessageCallback>();
                    Map<Integer, List<JSONArray>> errorMap = buildErrorMap(errors);
                    for (int i = 0; i < messageBatch.size(); i++) {
                        ClientToServerMessage<?> message = messageBatch.get(i);
                        try {
                            SyncMessageCallback r = pendingCallbacks.remove(message);
                            if (r != null && !callbacksExecutedThisLoop.contains(r)) {
                                List<JSONArray> errorList = errorMap.get(i);
                                if (errorList == null || errorList.isEmpty())
                                    r.runOnSuccess();
                                else
                                    r.runOnErrors(errorList);

                                callbacksExecutedThisLoop.add(r);
                            }
                        } catch (Exception e) {
                            Log.e(ERROR_TAG, "Unexpected exception executing sync callback", e);
                        }
                    }

                    messageBatch.clear();
                }
            }
        } catch (Exception e) {
            // In the worst case, restart thread if something goes wrong
            Log.e(ERROR_TAG, "Unexpected sync thread exception", e);
            Crittercism.logHandledException(e);
            thread = null;
            startSyncThread();
        }

    }

    private Map<Integer, List<JSONArray>> buildErrorMap(JSONArray errors) {
        Map<Integer, List<JSONArray>> result = new HashMap<Integer, List<JSONArray>>();
        if (errors != null) {
            for (int i = 0; i < errors.length(); i++) {
                JSONArray error = errors.optJSONArray(i);
                if (error != null && error.length() > 0) {
                    int index = error.optInt(0);
                    List<JSONArray> errorList = result.get(index);
                    if (errorList == null) {
                        errorList = new LinkedList<JSONArray>();
                        result.put(index, errorList);
                    }
                    errorList.add(error);
                }
            }
        }
        return result;
    }

    // Reapplies changes still in the outstanding tables to the local database
    // Called after a batch has finished processing
    private void replayOutstandingChanges(boolean afterErrors) {
        syncLog("Replaying outstanding changes"); //$NON-NLS-1$
        new ReplayOutstandingEntries<Task, TaskOutstanding>(Task.class, NameMaps.TABLE_ID_TASKS, taskDao, taskOutstandingDao, afterErrors).execute();
        new ReplayOutstandingEntries<TagData, TagOutstanding>(TagData.class, NameMaps.TABLE_ID_TAGS, tagDataDao, tagOutstandingDao, afterErrors).execute();
        new ReplayTaskListMetadataOutstanding(taskListMetadataDao, taskListMetadataOutstandingDao, afterErrors).execute();
    }

    private boolean timeForBackgroundSync() {
        return isTimeForBackgroundSync;
    }

    private void setWidgetSuppression(boolean suppress) {
        long date = suppress ? DateUtilities.now() : 0;
        TasksWidget.suppressUpdateFlag = date;

        if (date == 0) {
            Context context = ContextManager.getContext();
            if (context != null) {
                TasksWidget.updateWidgets(context);
            }
        }
    }

    private JSONObject clientVersion = null;

    @SuppressWarnings("nls")
    private JSONObject getClientVersion() {
        if (clientVersion == null) {
            try {
                PackageManager pm = ContextManager.getContext().getPackageManager();
                PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
                JSONObject message = new JSONObject();
                message.put(ClientToServerMessage.TYPE_KEY, "ClientVersion");
                message.put("platform", "android");
                message.put("versionName", pi.versionName);
                message.put("versionCode", pi.versionCode);
                clientVersion = message;
            } catch (Exception e) {
                Log.e(ERROR_TAG, "Error getting client version", e);
            }
        }
        return clientVersion;
    }

    public void repopulateQueueFromOutstandingTables() {
        syncLog("Constructing queue from outstanding tables"); //$NON-NLS-1$
        constructChangesHappenedFromOutstandingTable(Task.class, taskDao, taskOutstandingDao);
        constructChangesHappenedFromOutstandingTable(TagData.class, tagDataDao, tagOutstandingDao);
        constructChangesHappenedFromOutstandingTable(UserActivity.class, userActivityDao, userActivityOutstandingDao);
        constructChangesHappenedFromOutstandingTable(WaitingOnMe.class, waitingOnMeDao, waitingOnMeOutstandingDao);
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

    private void setupNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(ContextManager.getContext());
            builder.setContentText(ContextManager.getString(R.string.actfm_sync_ongoing))
            .setContentTitle(ContextManager.getString(R.string.app_name))
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(PendingIntent.getActivity(ContextManager.getContext().getApplicationContext(), 0, new Intent(), 0));


            notificationManager.notify(0, builder.getNotification());
            notificationId = 0;
        } catch (Exception e) {
            Log.e(ERROR_TAG, "Exception creating notification", e); //$NON-NLS-1$
        } catch (Error e) {
            Log.e(ERROR_TAG, "Error creating notification", e); //$NON-NLS-1$
        }
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
        token = actFmPreferenceService.getToken();
        return true;
    }

    public static void syncLog(String message) {
        if (ActFmInvoker.SYNC_DEBUG)
            Log.e(ERROR_TAG, message);
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
