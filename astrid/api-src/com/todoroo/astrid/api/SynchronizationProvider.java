/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.utility.Constants;

/**
 * A helper class for writing synchronization services for Astrid. This class
 * contains logic for merging incoming changes and writing outgoing changes.
 * <p>
 * Use {@link initiate} as the entry point for your synchronization service,
 * which should handle authentication and then call {@link synchronizeTasks} to
 * initiate synchronization.
 *
 * @author timsu
 *
 */
public abstract class SynchronizationProvider<TYPE extends TaskContainer> {

    // --- abstract methods - your services should implement these

    /**
     * Perform authenticate and other pre-synchronization steps, then
     * synchronize.
     * @param context either the parent activity, or a background service
     */
    abstract protected void initiate(Context context);

    /**
     * @param context
     * @return title of notification
     */
    abstract protected String getNotificationTitle(Context context);

    /**
     * Create a task on the remote server.
     *
     * @return task to create
     */
    abstract protected void create(TYPE task) throws IOException;

    /**
     * Push variables from given task to the remote server.
     *
     * @param task
     *            task proxy to push
     * @param remoteTask
     *            remote task that we merged with. may be null
     */
    abstract protected void push(TYPE task, TYPE remote) throws IOException;

    /**
     * Fetch remote task. Used to re-read merged tasks
     *
     * @param task
     *            task with id's to re-read
     * @return new Task
     */
    abstract protected TYPE pull(TYPE task) throws IOException;

    /**
     * Reads a task container from a task in the database
     *
     * @param task
     */
    abstract protected TYPE read(TodorooCursor<Task> task) throws IOException;

    /**
     * Save task. Used to save local tasks that have been updated and remote
     * tasks that need to be created locally
     *
     * @param task
     */
    abstract protected void write(TYPE task) throws IOException;

    /**
     * Finds a task in the list with the same remote identifier(s) as
     * the task passed in
     *
     * @return task from list if matches, null otherwise
     */
    abstract protected int matchTask(ArrayList<TYPE> tasks, TYPE target);

    /**
     * Transfer remote identifier(s) from one task to another
     */
    abstract protected void transferIdentifiers(TYPE source,
            TYPE destination);

    // --- implementation

    @Autowired
    private ExceptionService exceptionService;

    private final Notification notification;
    private PendingIntent notificationIntent;
    private String notificationTitle;

    public SynchronizationProvider() {
        DependencyInjectionService.getInstance().inject(this);

        // initialize notification
        int icon = android.R.drawable.stat_notify_sync;
        long when = System.currentTimeMillis();
        notification = new Notification(icon, null, when);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    }

    public void synchronize(final Context context) {
        // display toast
        if(context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.SyP_progress_toast,
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        // display notification
        notificationTitle = getNotificationTitle(context);
        notificationIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);
        postUpdate(context, context.getString(R.string.SyP_progress));
        final NotificationManager nm = new NotificationManager.AndroidNotificationManager(context);
        nm.notify(Constants.NOTIFICATION_SYNC, notification);

        // start next step in background thread
        new Thread(new Runnable() {
            public void run() {
                try {
                    initiate(context);
                } finally {
                    nm.cancel(Constants.NOTIFICATION_SYNC);
                }
            }
        }).start();
    }

    // --- utilities

    /**
     * Utility method for showing synchronization errors. If message is null,
     * the contents of the throwable is displayed. It is assumed that the error
     * was logged separately.
     */
    protected void showError(final Context context, Throwable e, String message) {
        exceptionService.displayAndReportError(context, message, e);
    }

    /**
     * Utility method to update the UI if we're an active sync, or output to
     * console if we're a background sync.
     */
    protected void postUpdate(Context context, String string) {
        notification.setLatestEventInfo(context,
                notificationTitle, string, notificationIntent);
    }

    // --- synchronization logic

    /**
     * Helper to synchronize remote tasks with our local database.
     *
     * This initiates the following process: 1. local changes are read 2. remote
     * changes are read 3. local tasks are merged with remote changes and pushed
     * across 4. remote changes are then read in
     *
     * @param data synchronization data structure
     */
    protected void synchronizeTasks(SyncData<TYPE> data) throws IOException {
        int length;

        // create internal data structures
        HashMap<String, Integer> remoteNewTaskNameMap = new HashMap<String, Integer>();
        length = data.remoteUpdated.size();
        for(int i = 0; i < length; i++) {
            TYPE remote = data.remoteUpdated.get(i);
            if(remote.task.getId() != Task.NO_ID)
                continue;
            remoteNewTaskNameMap.put(remote.task.getValue(Task.TITLE), i);
        }

        // 1. CREATE: grab newly created tasks and create them remotely
        length = data.localCreated.getCount();
        for(int i = 0; i < length; i++) {
            data.localCreated.moveToNext();
            TYPE local = read(data.localCreated);

            String taskTitle = local.task.getValue(Task.TITLE);

            /* If there exists an incoming remote task with the same name and no
             * mapping, we don't want to create this on the remote server,
             * because user could have synchronized like this before. Instead,
             * we create a mapping and do an update.
             */
            if (remoteNewTaskNameMap.containsKey(taskTitle)) {
                int remoteIndex = remoteNewTaskNameMap.remove(taskTitle);
                TYPE remote = data.remoteUpdated.get(remoteIndex);

                transferIdentifiers(remote, local);
                push(local, remote);

                // re-read remote task after merge, update remote task list
                remote = pull(remote);
                remote.task.setId(local.task.getId());
                data.remoteUpdated.set(remoteIndex, remote);
            } else {
                create(local);
            }
            write(local);
        }

        // 2. UPDATE: for each updated local task
        length = data.localUpdated.getCount();
        for(int i = 0; i < length; i++) {
            data.localUpdated.moveToNext();
            TYPE local = read(data.localUpdated);
            if(local.task == null)
                continue;

            // if there is a conflict, merge
            int remoteIndex = matchTask((ArrayList<TYPE>)data.remoteUpdated, local);
            if(remoteIndex != -1) {
                TYPE remote = data.remoteUpdated.get(remoteIndex);
                push(local, remote);

                // re-read remote task after merge
                remote = pull(remote);
                remote.task.setId(local.task.getId());
                data.remoteUpdated.set(remoteIndex, remote);
            } else {
                push(local, null);
            }
        }

        // 3. REMOTE: load remote information

        // Rearrange remoteTasks so completed tasks get synchronized first.
        // This prevents bugs where a repeated task has two copies come down
        // the wire, the new version and the completed old version. The new
        // version would get merged, then completed, if done in the wrong order.

        Collections.sort(data.remoteUpdated, new Comparator<TYPE>() {
            private static final int SENTINEL = -2;
            private final int check(TYPE o1, TYPE o2, LongProperty property) {
                long o1Property = o1.task.getValue(property);
                long o2Property = o2.task.getValue(property);
                if(o1Property != 0 && o2Property != 0)
                    return 0;
                else if(o1Property != 0)
                    return -1;
                else if(o2Property != 0)
                    return 1;
                return SENTINEL;
            }
            public int compare(TYPE o1, TYPE o2) {
                int comparison = check(o1, o2, Task.DELETION_DATE);
                if(comparison != SENTINEL)
                    return comparison;
                comparison = check(o1, o2, Task.COMPLETION_DATE);
                if(comparison != SENTINEL)
                    return comparison;
                return 0;
            }
        });

        length = data.remoteUpdated.size();
        for(int i = 0; i < length; i++) {
            TYPE remote = data.remoteUpdated.get(i);
            write(remote);
        }
    }

    // --- helper classes

    /** data structure builder */
    protected static class SyncData<TYPE extends TaskContainer> {
        public final ArrayList<TYPE> remoteUpdated;

        public final TodorooCursor<Task> localCreated;
        public final TodorooCursor<Task> localUpdated;

        public SyncData(ArrayList<TYPE> remoteUpdated,
                TodorooCursor<Task> localCreated,
                TodorooCursor<Task> localUpdated) {
            super();
            this.remoteUpdated = remoteUpdated;
            this.localCreated = localCreated;
            this.localUpdated = localUpdated;
        }

    }
}
