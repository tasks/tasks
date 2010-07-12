package com.todoroo.andlib.utility;

/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Message;
import android.os.Process;

/**
 * <p>UserTask enables proper and easy use of the UI thread. This class allows to
 * perform background operations and publish results on the UI thread without
 * having to manipulate threads and/or handlers.</p>
 *
 * <p>A user task is defined by a computation that runs on a background thread and
 * whose result is published on the UI thread. A user task is defined by 3 generic
 * types, called <code>Params</code>, <code>Progress</code> and <code>Result</code>,
 * and 4 steps, called <code>begin</code>, <code>doInBackground</code>,
 * <code>processProgress<code> and <code>end</code>.</p>
 *
 * <h2>Usage</h2>
 * <p>UserTask must be subclassed to be used. The subclass will override at least
 * one method ({@link #doInBackground(Object[])}), and most often will override a
 * second one ({@link #end(Object)}.)</p>
 *
 * <p>Here is an example of subclassing:</p>
 * <pre>
 * private class DownloadFilesTask extends UserTask&lt;URL, Integer, Long&gt; {
 *     public File doInBackground(URL... urls) {
 *         int count = urls.length;
 *         long totalSize = 0;
 *         for (int i = 0; i < count; i++) {
 *             totalSize += Downloader.downloadFile(urls[i]);
 *             publishProgress((int) ((i / (float) count) * 100));
 *         }
 *     }
 *
 *     public void processProgress(Integer... progress) {
 *         setProgressPercent(progress[0]);
 *     }
 *
 *     public void end(Long result) {
 *         showDialog("Downloaded " + result + " bytes");
 *     }
 * }
 * </pre>
 *
 * <p>Once created, a task is executed very simply:</p>
 * <pre>
 * new DownloadFilesTask().execute(new URL[] { ... });
 * </pre>
 *
 * <h2>User task's generic types</h2>
 * <p>The three types used by a user task are the following:</p>
 * <ol>
 *     <li><code>Params</code>, the type of the parameters sent to the task upon
 *     execution.</li>
 *     <li><code>Progress</code>, the type of the progress units published during
 *     the background computation.</li>
 *     <li><code>Result</code>, the type of the result of the background
 *     computation.</li>
 * </ol>
 * <p>Not all types are always used by a user task. To mark a type as unused,
 * simply use the type {@link Void}:</p>
 * <pre>
 * private class MyTask extends UserTask<Void, Void, Void) { ... }
 * </pre>
 *
 * <h2>The 4 steps</h2>
 * <p>When a user task is executed, the task goes through 4 steps:</p>
 * <ol>
 *     <li>{@link #begin()}, invoked on the UI thread immediately after the task
 *     is executed. This step is normally used to setup the task, for instance by
 *     showing a progress bar in the user interface.</li>
 *     <li>{@link #doInBackground(Object[])}, invoked on the background thread
 *     immediately after {@link #begin()} finishes executing. This step is used
 *     to perform background computation that can take a long time. The parameters
 *     of the user task are passed to this step. The result of the computation must
 *     be returned by this step and will be passed back to the last step. This step
 *     can also use {@link #publishProgress(Object[])} to publish one or more units
 *     of progress. These values are published on the UI thread, in the
 *     {@link #processProgress(Object[])} step.</li>
 *     <li>{@link #processProgress(Object[])}, invoked on the UI thread after a
 *     call to {@link #publishProgress(Object[])}. The timing of the execution is
 *     undefined. This method is used to display any form of progress in the user
 *     interface while the background computation is still executing. For instance,
 *     it can be used to animate a progress bar or show logs in a text field.</li>
 *     <li>{@link #end(Object)}, invoked on the UI thread after the background
 *     computation finishes. The result of the background computation is passed to
 *     this step as a parameter.</li>
 * </ol>
 *
 * <h2>Threading rules</h2>
 * <p>There are a few threading rules that must be followed for this class to
 * work properly:</p>
 * <ul>
 *     <li>The task instance must be created on the UI thread.</li>
 *     <li>{@link #execute(Object[])} must be invoked on the UI thread.</li>
 *     <li>Do not call {@link #begin()}, {@link #end(Object)},
 *     {@link #doInBackground(Object[])}, {@link #processProgress(Object[])}
 *     manually.</li>
 *     <li>The task can be executed only once (an exception will be thrown if
 *     a second execution is attempted.)</li>
 * </ul>
 */
@SuppressWarnings("nls")
public abstract class UserTask<Params, Progress, Result> {
    private static final String LOG_TAG = "UserTask";

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE = 10;

    private static final BlockingQueue<Runnable> sWorkQueue =
            new LinkedBlockingQueue<Runnable>(MAXIMUM_POOL_SIZE);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r, "UserTask #" + mCount.getAndIncrement());
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            return thread;
        }
    };

    private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue, sThreadFactory);

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    protected static InternalHandler sHandler;

    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;

    private volatile Status mStatus = Status.PENDING;

    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link UserTask#end(Object)} has finished.
         */
        FINISHED,
    }

    /**
     * Creates a new user task. This constructor must be invoked on the UI thread.
     */
    public UserTask() {
        if (sHandler == null) {
            sHandler = new InternalHandler();
        }

        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                return doInBackground(mParams);
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                Result result = null;
                try {
                    result = get();
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occured while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    return;
                } catch (Throwable t) {
                    throw new RuntimeException("An error occured while executing "
                            + "doInBackground()", t);
                }

                final Message message = sHandler.obtainMessage(MESSAGE_POST_RESULT,
                        new UserTaskResult<Result>(UserTask.this, result));
                message.sendToTarget();
            }
        };
    }

    /**
     * Returns the current status of this task.
     *
     * @return The current status.
     */
    public final Status getStatus() {
        return mStatus;
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute(Object[])}
     * by the caller of this task.
     *
     * This method can call {@link #publishProgress(Object[])} to publish updates
     * on the UI thread.
     *
     * @params params The parameters of the task.
     *
     * @return A result, defined by the subclass of this task.
     *
     * @see #begin()
     * @see #end(Object)
     * @see #publishProgress(Object[])
     */
    public Result doInBackground(@SuppressWarnings("unused") Params... params) {
        return null;
    }

    /**
     * Runs on the UI thread before {@link #doInBackground(Object[])}.
     *
     * @see #end(Object)
     * @see #doInBackground(Object[])
     */
    public void begin() {
        // ...
    }

    /**
     * Runs on the UI thread after {@link #doInBackground(Object[])}. The
     * specified result is the value returned by {@link #doInBackground(Object[])}
     * or null if the task was cancelled or an exception occured.
     *
     * @see #begin()
     * @see #doInBackground(Object[])
     */
    public void end(@SuppressWarnings("unused") Result result) {
        // ...
    }

    /**
     * Runs on the UI thread after {@link #publishProgress(Object[])} is invoked.
     * The specified values are the values passed to {@link #publishProgress(Object[])}.
     *
     * @see #publishProgress(Object[])
     * @see #doInBackground(Object[])
     */
    public void processProgress(@SuppressWarnings("unused") Progress... values) {
        // ...
    }

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed
     * normally.
     *
     * @return <tt>true</tt> if task was cancelled before it completed
     *
     * @see #cancel(boolean)
     */
    public final boolean isCancelled() {
        return mFuture.isCancelled();
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when <tt>cancel</tt> is called,
     * this task should never run.  If the task has already started,
     * then the <tt>mayInterruptIfRunning</tt> parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete.
     *
     * @return <tt>false</tt> if the task could not be cancelled,
     * typically because it has already completed normally;
     * <tt>true</tt> otherwise
     *
     * @see #isCancelled()
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return The computed result.
     *
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result.
     *
     * @return The computed result.
     *
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted
     *         while waiting.
     * @throws TimeoutException If the wait timed out.
     */
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    /**
     * Executes the task with the specified parameters. The task returns
     * itself (this) so that the caller can keep a reference to it.
     *
     * This method must be invoked on the UI thread.
     *
     * @params params The parameters of the task.
     *
     * @return This instance of UserTask.
     *
     * @throws IllegalStateException If {@link #getStatus()} returns either
     *         {@link com.google.android.photostream.UserTask.Status#RUNNING} or
     *         {@link com.google.android.photostream.UserTask.Status#FINISHED}.
     */
    public final UserTask<Params, Progress, Result> execute(Params... params) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                            + " the task has already been executed "
                            + "(a task can be executed only once)");
            }
        }

        mStatus = Status.RUNNING;

        begin();

        mWorker.mParams = params;

        try {
            sExecutor.execute(mFuture);
        } catch (RejectedExecutionException e) {
            // cannot schedule because of some other error. just die quietly
        }

        return this;
    }

    /**
     * This method can be invoked from {@link #doInBackground(Object[])} to
     * publish updates on the UI thread while the background computation is
     * still running. Each call to this method will trigger the execution of
     * {@link #processProgress(Object[])} on the UI thread.
     *
     * @params values The progress values to update the UI with.
     *
     * @see #processProgress(Object[])
     * @see #doInBackground(Object[])
     */
    protected final void publishProgress(Progress... values) {
        sHandler.obtainMessage(MESSAGE_POST_PROGRESS,
                new UserTaskResult<Progress>(this, values)).sendToTarget();
    }

    protected void finish(Result result) {
        end(result);
        mStatus = Status.FINISHED;
    }

    protected static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void handleMessage(Message msg) {
            UserTaskResult result = (UserTaskResult) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.processProgress(result.mData);
                    break;
            }
        }
    }

    protected static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    protected static class UserTaskResult<Data> {
        final UserTask<?, ?, ?> mTask;
        final Data[] mData;

        UserTaskResult(UserTask<?, ?, ?> task, Data... data) {
            mTask = task;
            mData = data;
        }
    }
}
