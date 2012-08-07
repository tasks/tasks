/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.widget.Toast;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.R;
import com.todoroo.astrid.data.Task;

/**
 * A helper class for writing synchronization services for Astrid. This class
 * contains logic for merging incoming changes and writing outgoing changes.
 * <p>
 * Use {@link #initiateManual} as the entry point for your synchronization
 * service, which should check if a user is logged in. If not, you should
 * handle that in the UI, otherwise, you should launch your background
 * service to perform synchronization in the background.
 * <p>
 * Your background service should {@link #synchronize}, which in turn
 * invokes  {@link #initiateBackground} to initiate synchronization.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public abstract class SyncProvider<TYPE extends SyncContainer> {

    // --- abstract methods - your services should implement these

    /**
     * @return sync utility instance
     */
    abstract protected SyncProviderUtilities getUtilities();

    /**
     * Perform log in (launching activity if necessary) and sync. This is
     * invoked when users manually request synchronization
     *
     * @param activity
     *            context
     */
    abstract protected void initiateManual(Activity activity);

    /**
     * Perform synchronize. Since this can be called from background services,
     * you should not open up new activities. Instead, if the user is not signed
     * in, your service should do nothing.
     */
    abstract protected void initiateBackground();

    /**
     * Updates the text of a notification and the intent to open when tapped
     * @param context
     * @param notification
     * @return notification id (in Android, there is at most one notification
     *         in the tray for a given id)
     */
    abstract protected int updateNotification(Context context, Notification n);

    /**
     * Create a task on the remote server.
     *
     * @param task
     *            task to create
     */
    abstract protected TYPE create(TYPE task) throws IOException;

    /**
     * Push variables from given task to the remote server, and read the newly
     * updated task.
     *
     * @param task
     *            task proxy to push
     * @param remoteTask
     *            remote task that we merged with. may be null
     * @return task pulled on remote server
     */
    abstract protected TYPE push(TYPE task, TYPE remote) throws IOException;

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

    private final Notification notification;

    @Autowired protected ExceptionService exceptionService;

    public SyncProvider() {
        DependencyInjectionService.getInstance().inject(this);

        // initialize notification
        int icon = android.R.drawable.stat_notify_sync;
        long when = System.currentTimeMillis();
        notification = new Notification(icon, null, when);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    }

    /**
     * Synchronize this provider with sync toast
     * @param context
     */
    public void synchronize(final Context context) {
        synchronize(context, true);
    }

    /**
     * Synchronize this provider
     * @param context
     * @param showSyncToast should we toast to indicate synchronizing?
     */
    public void synchronize(final Context context, final boolean showSyncToast) {
        // display toast
        if(context instanceof Activity) {
            if(getUtilities().isLoggedIn() && getUtilities().shouldShowToast()) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(showSyncToast)
                            makeSyncToast(context);
                    }
                });
            }
            initiateManual((Activity)context);
        } else if(context instanceof SyncBackgroundService) {
            // display notification
            final int notificationId = updateNotification(context, notification);
            final NotificationManager nm = new NotificationManager.AndroidNotificationManager(context);
            nm.notify(notificationId, notification);

            // start next step in background thread
            new Thread(new Runnable() {
                public void run() {
                    try {
                        initiateBackground();
                    } finally {
                        nm.cancel(notificationId);
                        ((SyncBackgroundService)context).stop();
                    }
                }
            }).start();
        } else {
            // unit test
            initiateBackground();
        }
    }

    protected void makeSyncToast(Context context) {
        Toast.makeText(context, R.string.SyP_progress_toast,
                Toast.LENGTH_LONG).show();
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
        sendLocallyCreated(data, remoteNewTaskNameMap);

        // 2. UPDATE: for each updated local task
        sendLocallyUpdated(data);

        // 3. REMOTE: load remote information
        readRemotelyUpdated(data);
    }

    @SuppressWarnings("nls")
    protected String getFinalSyncStatus() {
        if (getUtilities().getLastError() != null || getUtilities().getLastAttemptedSyncDate() != 0) {
            if (getUtilities().getLastAttemptedSyncDate() == 0)
                return "errors";
            else
                return "failed";
        } else {
            return "success";
        }
    }

    protected void readRemotelyUpdated(SyncData<TYPE> data) throws IOException {
        int length;
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

            // don't synchronize new & deleted tasks
            if(!remote.task.isSaved() && (remote.task.isDeleted()))
                continue;

            try {
                write(remote);
            } catch (Exception e) {
                handleException("sync-remote-updated", e, false); //$NON-NLS-1$
            }
        }
    }

    protected void sendLocallyUpdated(SyncData<TYPE> data) throws IOException {
        int length;
        length = data.localUpdated.getCount();
        for(int i = 0; i < length; i++) {
            data.localUpdated.moveToNext();
            TYPE local = read(data.localUpdated);
            try {
                if(local.task == null)
                    continue;

                // if there is a conflict, merge
                int remoteIndex = matchTask((ArrayList<TYPE>)data.remoteUpdated, local);
                if(remoteIndex != -1) {
                    TYPE remote = data.remoteUpdated.get(remoteIndex);

                    remote = push(local, remote);

                    // re-read remote task after merge (with local's title)
                    remote.task.setId(local.task.getId());
                    data.remoteUpdated.set(remoteIndex, remote);
                } else {
                    push(local, null);
                }
            } catch (Exception e) {
                handleException("sync-local-updated", e, false); //$NON-NLS-1$
            }
            write(local);
        }
    }

    protected void sendLocallyCreated(SyncData<TYPE> data,
            HashMap<String, Integer> remoteNewTaskNameMap) throws IOException {
        int length;
        length = data.localCreated.getCount();
        for(int i = 0; i < length; i++) {
            data.localCreated.moveToNext();
            TYPE local = read(data.localCreated);
            try {

                String taskTitle = local.task.getValue(Task.TITLE);

                /* If there exists an incoming remote task with the same name and no
                 * mapping, we don't want to create this on the remote server,
                 * because user could have synchronized this before. Instead,
                 * we create a mapping and do an update.
                 */
                if (remoteNewTaskNameMap.containsKey(taskTitle)) {
                    int remoteIndex = remoteNewTaskNameMap.remove(taskTitle);
                    TYPE remote = data.remoteUpdated.get(remoteIndex);

                    transferIdentifiers(remote, local);
                    remote = push(local, remote);

                    // re-read remote task after merge, update remote task list
                    remote.task.setId(local.task.getId());
                    data.remoteUpdated.set(remoteIndex, remote);

                } else {
                    create(local);
                }
            } catch (Exception e) {
                handleException("sync-local-created", e, false); //$NON-NLS-1$
            }
            write(local);
        }
    }

    // --- exception handling

    /**
     * Deal with a synchronization exception. If requested, will show an error
     * to the user (unless synchronization is happening in background)
     *
     * @param context
     * @param tag
     *            error tag
     * @param e
     *            exception
     * @param showError
     *            whether to display a dialog
     */
    protected void handleException(String tag, Exception e, boolean displayError) {
        //TODO: When Crittercism supports it, report error to them
        final Context context = ContextManager.getContext();
        getUtilities().setLastError(e.toString(), "");

        String message = null;

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$
        }

        // occurs when network error
        else if(e instanceof IOException) {
            exceptionService.reportError(tag + "-io", e); //$NON-NLS-1$
            message = context.getString(R.string.SyP_ioerror);
        }

        // unhandled error
        else {
            message = context.getString(R.string.DLG_error, e.toString());
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
        }

        if(displayError && context instanceof Activity && message != null) {
            DialogUtilities.okDialog((Activity)context,
                    message, null);
        }
    }

    // --- helper classes

    /** data structure builder */
    protected static class SyncData<TYPE extends SyncContainer> {
        public ArrayList<TYPE> remoteUpdated;

        public TodorooCursor<Task> localCreated;
        public TodorooCursor<Task> localUpdated;

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
