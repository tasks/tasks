package org.tasks.sync;

import com.todoroo.astrid.sync.SyncResultCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

public class SyncExecutor {

    private final ExecutorService executor = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),
            new DiscardPolicy());

    @Inject
    public SyncExecutor() {
    }

    public void execute(final SyncResultCallback callback, final Runnable command) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                } catch (Exception e) {
                    executor.shutdownNow();
                    callback.finished();
                }
            }
        });
    }
}
