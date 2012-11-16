package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.actfm.sync.messages.BriefMe;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.actfm.sync.messages.ServerToClientMessage;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

public class ActFmSyncThread {

    private final Queue<Pair<Long, ModelType>> changesQueue;
    private final Object monitor;
    private Thread thread;

    @Autowired
    private ActFmInvoker actFmInvoker;

    @Autowired
    private ActFmPreferenceService actFmPreferenceService;

    private String token;

    public static enum ModelType {
        TYPE_TASK,
        TYPE_TAG
    }

    public ActFmSyncThread(Queue<Pair<Long, ModelType>> queue, Object syncMonitor) {
        DependencyInjectionService.getInstance().inject(this);
        this.changesQueue = queue;
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

    @SuppressWarnings("nls")
    private void sync() {
        try {
            int batchSize = 1;
            List<ClientToServerMessage<?>> messages = new LinkedList<ClientToServerMessage<?>>();
            while(true) {
                synchronized(monitor) {
                    while (changesQueue.isEmpty() && !timeForBackgroundSync()) {
                        try {
                            monitor.wait();
                        } catch (InterruptedException e) {
                            // Ignored
                        }
                    }
                }

                // Stuff in the document
                while (messages.size() < batchSize && !changesQueue.isEmpty()) {
                    Pair<Long, ModelType> tuple = changesQueue.poll();
                    if (tuple != null) {
                        ChangesHappened<?, ?> changes = ClientToServerMessage.instantiateChangesHappened(tuple.getLeft(), tuple.getRight());
                        if (changes != null)
                            messages.add(changes);
                    }
                }

                if (messages.isEmpty() && timeForBackgroundSync()) {
                    messages.add(instantiateBriefMe(Task.class));
                    messages.add(instantiateBriefMe(TagData.class));
                }

                if (!messages.isEmpty() && checkForToken()) {
                    JSONArray payload = new JSONArray();
                    for (ClientToServerMessage<?> message : messages) {
                        JSONObject serialized = message.serializeToJSON();
                        if (serialized != null)
                            payload.put(serialized);
                    }

                    try {
                        JSONObject response = actFmInvoker.invoke("sync", "data", payload, "token", token);
                        // process responses
                        JSONArray serverMessagesJson = response.optJSONArray("messages");
                        if (serverMessagesJson != null) {
                            for (int i = 0; i < serverMessagesJson.length(); i++) {
                                JSONObject serverMessageJson = serverMessagesJson.optJSONObject(i);
                                if (serverMessageJson != null) {
                                    ServerToClientMessage serverMessage = ServerToClientMessage.instantiateMessage(serverMessageJson);
                                    if (serverMessage != null)
                                        serverMessage.processMessage();
                                }
                            }
                        }

                        batchSize = Math.min(batchSize, messages.size()) * 2;
                    } catch (IOException e) {
                        batchSize = Math.max(batchSize / 2, 1);
                    }
                    messages = new LinkedList<ClientToServerMessage<?>>();
                }
            }
        } catch (Exception e) {
            // In the worst case, restart thread if something goes wrong
            Log.e("actfm-sync", "Unexpected sync thread exception", e);
            thread = null;
            startSyncThread();
        }

    }

    private <TYPE extends RemoteModel> BriefMe<TYPE> instantiateBriefMe(Class<TYPE> cls) {
        // TODO: compute last pushed at value for model class
        long pushedAt = 0;
        return new BriefMe<TYPE>(cls, null, pushedAt);
    }

    private boolean timeForBackgroundSync() {
        return true;
    }

    private boolean checkForToken() {
        if(!actFmPreferenceService.isLoggedIn())
            return false;
        token = actFmPreferenceService.getToken();
        return true;
    }

}
