package com.todoroo.astrid.actfm.sync;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;

public class ActFmSyncWaitingPool {

    private static volatile ActFmSyncWaitingPool instance;

    public static ActFmSyncWaitingPool getInstance() {
        if (instance == null) {
            synchronized(ActFmSyncWaitingPool.class) {
                if (instance == null) {
                    instance = new ActFmSyncWaitingPool();
                }
            }
        }
        return instance;
    }

    private static final long WAIT_TIME = 15 * 1000L;

    private final ExecutorService singleThreadPool;
    private final List<ClientToServerMessage<?>> pendingMessages;
    private final Runnable delayMessageRunnable = new Runnable() {
        @Override
        public void run() {
            if (pendingMessages.isEmpty())
                return;
            AndroidUtilities.sleepDeep(WAIT_TIME);
            while (!pendingMessages.isEmpty()) {
                ActFmSyncThread.getInstance().enqueueMessage(pendingMessages.remove(0), ActFmSyncThread.DEFAULT_REFRESH_RUNNABLE);
            }
        }
    };

    private ActFmSyncWaitingPool() {
        super();
        singleThreadPool = Executors.newSingleThreadExecutor();
        pendingMessages = Collections.synchronizedList(new LinkedList<ClientToServerMessage<?>>());
    }

    public synchronized void enqueueMessage(ClientToServerMessage<?> message) {
        if (!pendingMessages.contains(message)) {
            pendingMessages.add(message);
            singleThreadPool.submit(delayMessageRunnable);
        }
    }

}
