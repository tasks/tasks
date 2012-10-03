package com.todoroo.astrid.actfm.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.actfm.sync.messages.ClientToServerMessage;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class ActFmSyncThread {

    private final Queue<Pair<Long, Class<? extends RemoteModel>>> queue;
    private final Object monitor;
    private Thread thread;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TagDataDao tagDataDao;

    @Autowired
    private TaskOutstandingDao taskOutstandingDao;

    @Autowired
    private TagOutstandingDao tagOutstandingDao;

    public ActFmSyncThread(Queue<Pair<Long, Class<? extends RemoteModel>>> queue, Object syncMonitor) {
        DependencyInjectionService.getInstance().inject(this);
        this.queue = queue;
        this.monitor = syncMonitor;
    }

    public synchronized void startThread() {
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

    private void sync() {
        int batchSize = 1;
        List<ClientToServerMessage<?>> messages = new ArrayList<ClientToServerMessage<?>>();
        while(true) {
            synchronized(monitor) {
                while (queue.isEmpty() && !timeForBackgroundSync()) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
            }

            // Stuff in the document
            while (messages.size() < batchSize && !queue.isEmpty()) {
                Pair<Long, Class<? extends RemoteModel>> tuple = queue.poll();
                if (tuple != null) {
                    messages.add(getChangesHappened(tuple));
                }
            }

            if (messages.isEmpty() && timeForBackgroundSync()) {
                // Populate messages with BriefMes
            }
        }
    }

    private ChangesHappened<?, ?> getChangesHappened(Pair<Long, Class<? extends RemoteModel>> tuple) {
        Class<? extends RemoteModel> modelClass = tuple.getRight();
        if (modelClass.equals(Task.class)) {
            return new ChangesHappened<Task, TaskOutstanding>(tuple.getLeft(), Task.class, taskDao, taskOutstandingDao);
        } else if (modelClass.equals(TagData.class)) {
            return new ChangesHappened<TagData, TagOutstanding>(tuple.getLeft(), TagData.class, tagDataDao, tagOutstandingDao);
        }

        return null;
    }

    private boolean timeForBackgroundSync() {
        return true;
    }

}
