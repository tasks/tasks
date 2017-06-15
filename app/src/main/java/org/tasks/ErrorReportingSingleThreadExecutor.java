package org.tasks;

import android.support.annotation.NonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.tasks.analytics.Tracker;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class ErrorReportingSingleThreadExecutor implements Executor, Thread.UncaughtExceptionHandler {
    private final ExecutorService executorService;
    private final Tracker tracker;

    public ErrorReportingSingleThreadExecutor(String nameFormat, Tracker tracker) {
        executorService = newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(String.format("%s-%%d", nameFormat))
                        .setUncaughtExceptionHandler(this)
                        .build());
        this.tracker = tracker;
    }

    @Override
    public void execute(@NonNull Runnable runnable) {
        try {
            executorService.execute(runnable);
        } catch (Exception e) {
            tracker.reportException(e);
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        tracker.reportException(thread, throwable);
    }
}
