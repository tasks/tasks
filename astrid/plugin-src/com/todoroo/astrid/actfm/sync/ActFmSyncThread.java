package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.actfm.sync.messages.ReplayOutstandingEntries;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.utility.Flags;

public class ActFmSyncThread {

    private static final String ERROR_TAG = "actfm-sync-thread"; //$NON-NLS-1$

    private final List<ClientToServerMessage<?>> pendingMessages;
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

    private String token;

    public static enum ModelType {
        TYPE_TASK,
        TYPE_TAG
    }

    private static volatile ActFmSyncThread instance;

    public static synchronized ActFmSyncThread getInstance() {
        if (instance == null) {
            synchronized(ActFmSyncThread.class) {
                if (instance == null) {
                    initializeSyncComponents(PluginServices.getTaskDao(), PluginServices.getTagDataDao());
                }
            }
        }
        return instance;
    }

    public static ActFmSyncThread initializeSyncComponents(TaskDao taskDao, TagDataDao tagDataDao) {
        if (instance == null) {
            synchronized(ActFmSyncThread.class) {
                if (instance == null) {
                    List<ClientToServerMessage<?>> syncQueue = Collections.synchronizedList(new LinkedList<ClientToServerMessage<?>>());
                    ActFmSyncMonitor monitor = ActFmSyncMonitor.getInstance();

                    taskDao.addListener(new SyncDatabaseListener<Task>(syncQueue, monitor, ModelType.TYPE_TASK));
                    tagDataDao.addListener(new SyncDatabaseListener<TagData>(syncQueue, monitor, ModelType.TYPE_TAG));

                    instance = new ActFmSyncThread(syncQueue, monitor);
                    instance.startSyncThread();
                }
            }
        }
        return instance;
    }

    private ActFmSyncThread(List<ClientToServerMessage<?>> messageQueue, Object syncMonitor) {
        DependencyInjectionService.getInstance().inject(this);
        this.pendingMessages = messageQueue;
        this.monitor = syncMonitor;
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

    public void enqueueMessage(ClientToServerMessage<?> message) {
        pendingMessages.add(message);
        synchronized(monitor) {
            monitor.notifyAll();
        }
    }

    @SuppressWarnings("nls")
    private void sync() {
        try {
            int batchSize = 1;
            List<ClientToServerMessage<?>> messageBatch = new LinkedList<ClientToServerMessage<?>>();
            while(true) {
                synchronized(monitor) {
                    while ((pendingMessages.isEmpty() && !timeForBackgroundSync()) || !actFmPreferenceService.isLoggedIn()) {
                        try {
                            monitor.wait();
                            AndroidUtilities.sleepDeep(500L); // Wait briefly for large database operations to finish (e.g. adding a task with several tags may trigger a message before all saves are done--fix this?)
                        } catch (InterruptedException e) {
                            // Ignored
                        }
                    }
                }

                // Stuff in the document
                while (messageBatch.size() < batchSize && !pendingMessages.isEmpty()) {
                    ClientToServerMessage<?> message = pendingMessages.remove(0);
                    if (message != null)
                        messageBatch.add(message);
                }

                if (messageBatch.isEmpty() && timeForBackgroundSync()) {
                    Flags.checkAndClear(Flags.BG_SYNC);
                    messageBatch.add(BriefMe.instantiateBriefMeForClass(Task.class, NameMaps.PUSHED_AT_TASKS));
                    messageBatch.add(BriefMe.instantiateBriefMeForClass(TagData.class, NameMaps.PUSHED_AT_TAGS));
                }

                if (!messageBatch.isEmpty() && checkForToken()) {
                    JSONArray payload = new JSONArray();
                    for (ClientToServerMessage<?> message : messageBatch) {
                        JSONObject serialized = message.serializeToJSON();
                        if (serialized != null) {
                            payload.put(serialized);
                            if (ActFmInvoker.SYNC_DEBUG)
                                Log.e("actfm-send-message", serialized.toString());
                        }
                    }

                    if (payload.length() == 0)
                        continue;

                    try {
                        JSONObject response = actFmInvoker.postSync(payload, token);
                        // process responses
                        JSONArray serverMessagesJson = response.optJSONArray("messages");
                        String modelPushedAtString = response.optString("time");
                        long modelPushedAt = 0;
                        try {
                            modelPushedAt = DateUtilities.parseIso8601(modelPushedAtString);
                        } catch (ParseException e) {
                            Log.e(ERROR_TAG, "Unparseable date " + modelPushedAtString, e);
                        }
                        if (serverMessagesJson != null) {
                            for (int i = 0; i < serverMessagesJson.length(); i++) {
                                JSONObject serverMessageJson = serverMessagesJson.optJSONObject(i);
                                if (serverMessageJson != null) {
                                    ServerToClientMessage serverMessage = ServerToClientMessage.instantiateMessage(serverMessageJson, modelPushedAt);
                                    if (serverMessage != null) {
                                        serverMessage.processMessage();
                                    } else {
                                        Log.e(ERROR_TAG, "Unable to instantiate message " + serverMessageJson.toString());
                                    }
                                }
                            }
                            JSONArray errors = response.optJSONArray("errors");
                            boolean errorsExist = (errors != null && errors.length() > 0);
                            replayOutstandingChanges(errorsExist);
                        }

                        batchSize = Math.min(batchSize, messageBatch.size()) * 2;
                    } catch (IOException e) {
                        Log.e(ERROR_TAG, "IOException", e);
                        batchSize = Math.max(batchSize / 2, 1);
                    }
                    messageBatch = new LinkedList<ClientToServerMessage<?>>();
                    Intent refresh = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
                    ContextManager.getContext().sendBroadcast(refresh);
                }
            }
        } catch (Exception e) {
            // In the worst case, restart thread if something goes wrong
            Log.e(ERROR_TAG, "Unexpected sync thread exception", e);
            thread = null;
            startSyncThread();
        }

    }

    // Reapplies changes still in the outstanding tables to the local database
    // Called after a batch has finished processing
    private void replayOutstandingChanges(boolean afterErrors) {
        new ReplayOutstandingEntries<Task, TaskOutstanding>(Task.class, NameMaps.TABLE_ID_TASKS, taskDao, taskOutstandingDao, pendingMessages, monitor, afterErrors).execute();
        new ReplayOutstandingEntries<TagData, TagOutstanding>(TagData.class, NameMaps.TABLE_ID_TAGS, tagDataDao, tagOutstandingDao, pendingMessages, monitor, afterErrors).execute();
    }

    private boolean timeForBackgroundSync() {
        return Flags.check(Flags.BG_SYNC);
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
        token = actFmPreferenceService.getToken();
        return true;
    }

}
