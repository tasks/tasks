package org.tasks.sync;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.todoroo.astrid.sync.SyncResultCallback;

import org.tasks.analytics.Tracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

public class SyncExecutor {

    private final ExecutorService executor = newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("sync-executor-%d").build());

    private final Tracker tracker;

    @Inject
    public SyncExecutor(Tracker tracker) {
        this.tracker = tracker;
    }

    public void execute(final SyncResultCallback callback, final Runnable command) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    command.run();
                } catch (Exception e) {
                    Timber.e(e, e.getMessage());
                    tracker.reportException(e);
                    executor.shutdownNow();
                    callback.finished();
                }
            }
        });
    }
}
