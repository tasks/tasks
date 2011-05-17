/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.weloveastrid.rmilk.MilkBackgroundService;
import org.weloveastrid.rmilk.MilkDependencyInjector;
import org.weloveastrid.rmilk.MilkLoginActivity;
import org.weloveastrid.rmilk.MilkLoginActivity.SyncLoginCallback;
import org.weloveastrid.rmilk.MilkPreferences;
import org.weloveastrid.rmilk.MilkUtilities;
import org.weloveastrid.rmilk.api.ApplicationInfo;
import org.weloveastrid.rmilk.api.ServiceImpl;
import org.weloveastrid.rmilk.api.ServiceInternalException;
import org.weloveastrid.rmilk.api.data.RtmAuth.Perms;
import org.weloveastrid.rmilk.api.data.RtmList;
import org.weloveastrid.rmilk.api.data.RtmLists;
import org.weloveastrid.rmilk.api.data.RtmTask;
import org.weloveastrid.rmilk.api.data.RtmTask.Priority;
import org.weloveastrid.rmilk.api.data.RtmTaskList;
import org.weloveastrid.rmilk.api.data.RtmTaskNote;
import org.weloveastrid.rmilk.api.data.RtmTaskSeries;
import org.weloveastrid.rmilk.api.data.RtmTasks;
import org.weloveastrid.rmilk.data.MilkListService;
import org.weloveastrid.rmilk.data.MilkMetadataService;
import org.weloveastrid.rmilk.data.MilkNoteHelper;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;

public class MilkSyncProvider extends SyncProvider<MilkTaskContainer> {

    private ServiceImpl rtmService = null;
    private String timeline = null;

    @Autowired private MilkMetadataService milkMetadataService;
    @Autowired private MilkListService milkListService;

    static {
        MilkDependencyInjector.initialize();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- public methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of RTM, deleting all synchronization metadata
     */
    public void signOut(Context context) {
        ContextManager.setContext(context);
        MilkUtilities.INSTANCE.setToken(null);
        MilkUtilities.INSTANCE.clearLastSyncDate();

        DependencyInjectionService.getInstance().inject(this);
        milkMetadataService.clearMetadata();
    }

    @Override
    protected SyncProviderUtilities getUtilities() {
        return MilkUtilities.INSTANCE;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * set up service
     */
    @SuppressWarnings("nls")
    private void initializeService(String authToken) throws ServiceInternalException {
        String appName = null;
        String z = stripslashes(0,"q9883o3384n21snq17501qn38oo1r689", "b");
        String v = stripslashes(16,"19o2n020345219os","a");

        if(authToken == null)
            rtmService = new ServiceImpl(new ApplicationInfo(
                    z, v, appName));
        else
            rtmService = new ServiceImpl(new ApplicationInfo(
                    z, v, appName, authToken));
    }

    /**
     * initiate sync in background
     */
    @Override
    @SuppressWarnings("nls")
    protected void initiateBackground() {
        DependencyInjectionService.getInstance().inject(this);

        try {
            String authToken = MilkUtilities.INSTANCE.getToken();

            // check if we have a token & it works
            if(authToken != null) {
                initializeService(authToken);
                if(!rtmService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                // try completing the authorization if it was partial
                if(rtmService != null) {
                    try {
                        String token = rtmService.completeAuthorization();
                        MilkUtilities.INSTANCE.setToken(token);
                        performSync();

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                // can't do anything, user not logged in

            } else {
                performSync();
            }
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-authenticate", e, true);
        } finally {
            MilkUtilities.INSTANCE.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @SuppressWarnings("nls")
    @Override
    protected void initiateManual(final Activity activity) {
        final Resources r = activity.getResources();
        String authToken = MilkUtilities.INSTANCE.getToken();
        MilkUtilities.INSTANCE.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            // open up a dialog and have the user go to browser
            final String url;
            try {
                initializeService(null);
                url = rtmService.beginAuthorization(Perms.delete);
            } catch (Exception e) {
                handleException("rmilk-auth", e, true);
                return;
            }

            Intent intent = new Intent(activity, MilkLoginActivity.class);
            MilkLoginActivity.setCallback(new SyncLoginCallback() {
                public String verifyLogin(final Handler syncLoginHandler) {
                    if(rtmService == null) {
                        return null;
                    }
                    try {
                        String token = rtmService.completeAuthorization();
                        MilkUtilities.INSTANCE.setToken(token);
                        synchronize(activity);
                        return null;
                    } catch (Exception e) {
                        // didn't work
                        handleException("rtm-verify-login", e, false);
                        rtmService = null;
                        if(e instanceof ServiceInternalException)
                            e = ((ServiceInternalException)e).getEnclosedException();
                        return r.getString(R.string.rmilk_MLA_error, e.getMessage());
                    }
                }
            });
            intent.putExtra(MilkLoginActivity.URL_TOKEN, url);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(MilkBackgroundService.SYNC_ACTION, null,
                    activity, MilkBackgroundService.class));
            activity.finish();
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        try {
            // get RTM timeline
            timeline = rtmService.timelines_create();

            // load RTM lists
            RtmLists lists = rtmService.lists_getList();
            milkListService.setLists(lists);

            // read all tasks
            ArrayList<MilkTaskContainer> remoteChanges = new ArrayList<MilkTaskContainer>();
            Date lastSyncDate = new Date(MilkUtilities.INSTANCE.getLastSyncDate());
            boolean shouldSyncIndividualLists = false;
            String filter = null;
            if(lastSyncDate.getTime() == 0)
                filter = "status:incomplete"; //$NON-NLS-1$ // 1st time sync: get unfinished tasks

            // try the quick synchronization
            try {
                Thread.sleep(1000); // throttle
                RtmTasks tasks = rtmService.tasks_getList(null, filter, lastSyncDate);
                addTasksToList(tasks, remoteChanges);
            } catch (Exception e) {
                handleException("rtm-quick-sync", e, false); //$NON-NLS-1$
                remoteChanges.clear();
                shouldSyncIndividualLists = true;
            }

            if(shouldSyncIndividualLists) {
                for(RtmList list : lists.getLists().values()) {
                    if(list.isSmart())
                        continue;
                    try {
                        Thread.sleep(1500);
                        RtmTasks tasks = rtmService.tasks_getList(list.getId(),
                                filter, lastSyncDate);
                        addTasksToList(tasks, remoteChanges);
                    } catch (Exception e) {
                        handleException("rtm-indiv-sync", e, true); //$NON-NLS-1$
                        continue;
                    }
                }
            }

            SyncData<MilkTaskContainer> syncData = populateSyncData(remoteChanges);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            MilkUtilities.INSTANCE.recordSuccessfulSync();
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-sync", e, true); //$NON-NLS-1$
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.IMPORTANCE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.NOTES,
    };

    /**
     * Populate SyncData data structure
     */
    private SyncData<MilkTaskContainer> populateSyncData(ArrayList<MilkTaskContainer> remoteTasks) {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = milkMetadataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = milkMetadataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<MilkTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    /**
     * Add the tasks read from RTM to the given list
     */
    private void addTasksToList(RtmTasks tasks, ArrayList<MilkTaskContainer> list) {
        for (RtmTaskList taskList : tasks.getLists()) {
            for (RtmTaskSeries taskSeries : taskList.getSeries()) {
                MilkTaskContainer remote = parseRemoteTask(taskSeries);

                // update reminder flags for incoming remote tasks to prevent annoying
                if(remote.task.hasDueDate() && remote.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                    remote.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

                milkMetadataService.findLocalMatch(remote);
                list.add(remote);
            }
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected MilkTaskContainer create(MilkTaskContainer task) throws IOException {
        String listId = null;
        if(task.listId > 0)
            listId = Long.toString(task.listId);
        RtmTaskSeries rtmTask = rtmService.tasks_add(timeline, listId,
                task.task.getValue(Task.TITLE));
        MilkTaskContainer newRemoteTask = parseRemoteTask(rtmTask);
        transferIdentifiers(newRemoteTask, task);
        push(task, newRemoteTask);
        return newRemoteTask;
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(MilkTaskContainer task, Property<?> property, MilkTaskContainer remoteTask) {
        if(!task.task.containsValue(property))
            return false;

        if(remoteTask == null)
            return true;
        if(!remoteTask.task.containsValue(property))
            return true;

        // special cases - match if they're zero or nonzero
        if(property == Task.COMPLETION_DATE ||
                property == Task.DELETION_DATE)
            return !AndroidUtilities.equals((Long)task.task.getValue(property) == 0,
                    (Long)remoteTask.task.getValue(property) == 0);

        return !AndroidUtilities.equals(task.task.getValue(property),
                remoteTask.task.getValue(property));
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     */
    @Override
    protected MilkTaskContainer push(MilkTaskContainer local, MilkTaskContainer remote) throws IOException {
        boolean remerge = false;

        // fetch remote task for comparison
        if(remote == null)
            remote = pull(local);

        String listId = Long.toString(local.listId);
        String taskSeriesId = Long.toString(local.taskSeriesId);
        String taskId = Long.toString(local.taskId);

        if(remote != null && !AndroidUtilities.equals(local.listId, remote.listId))
            rtmService.tasks_moveTo(timeline, Long.toString(remote.listId),
                    listId, taskSeriesId, taskId);

        // either delete or re-create if necessary
        if(shouldTransmit(local, Task.DELETION_DATE, remote)) {
            if(local.task.getValue(Task.DELETION_DATE) > 0)
                rtmService.tasks_delete(timeline, listId, taskSeriesId, taskId);
            else if(remote == null) {
                RtmTaskSeries rtmTask = rtmService.tasks_add(timeline, listId,
                        local.task.getValue(Task.TITLE));
                remote = parseRemoteTask(rtmTask);
                transferIdentifiers(remote, local);
            }
        }

        if(shouldTransmit(local, Task.TITLE, remote))
            rtmService.tasks_setName(timeline, listId, taskSeriesId,
                    taskId, local.task.getValue(Task.TITLE));
        if(shouldTransmit(local, Task.IMPORTANCE, remote))
            rtmService.tasks_setPriority(timeline, listId, taskSeriesId,
                    taskId, Priority.values(local.task.getValue(Task.IMPORTANCE)));
        if(shouldTransmit(local, Task.DUE_DATE, remote))
            rtmService.tasks_setDueDate(timeline, listId, taskSeriesId,
                    taskId, DateUtilities.unixtimeToDate(local.task.getValue(Task.DUE_DATE)),
                    local.task.hasDueTime());
        if(shouldTransmit(local, Task.COMPLETION_DATE, remote)) {
            if(local.task.getValue(Task.COMPLETION_DATE) == 0)
                rtmService.tasks_uncomplete(timeline, listId, taskSeriesId,
                        taskId);
            else {
                rtmService.tasks_complete(timeline, listId, taskSeriesId,
                        taskId);
                // if repeating, pull and merge
                if(local.repeating)
                    remerge = true;
            }
        }

        // tags
        HashSet<String> localTags = new HashSet<String>();
        HashSet<String> remoteTags = new HashSet<String>();
        for(Metadata item : local.metadata)
            if(MilkMetadataService.TAG_KEY.equals(item.getValue(Metadata.KEY)))
                localTags.add(item.getValue(Metadata.VALUE1));
        if(remote != null && remote.metadata != null) {
            for(Metadata item : remote.metadata)
                if(MilkMetadataService.TAG_KEY.equals(item.getValue(Metadata.KEY)))
                    remoteTags.add(item.getValue(Metadata.VALUE1));
        }
        if(!localTags.equals(remoteTags)) {
            String[] tags = localTags.toArray(new String[localTags.size()]);
            rtmService.tasks_setTags(timeline, listId, taskSeriesId,
                    taskId, tags);
        }

        // notes
        if(shouldTransmit(local, Task.NOTES, remote)) {
            String[] titleAndText = MilkNoteHelper.fromNoteField(local.task.getValue(Task.NOTES));
            List<RtmTaskNote> notes = null;
            if(remote != null && remote.remote.getNotes() != null)
                notes = remote.remote.getNotes().getNotes();
            if(notes != null && notes.size() > 0) {
                String remoteNoteId = notes.get(0).getId();
                rtmService.tasks_notes_edit(timeline, remoteNoteId, titleAndText[0],
                        titleAndText[1]);
            } else {
                rtmService.tasks_notes_add(timeline, listId, taskSeriesId,
                        taskId, titleAndText[0], titleAndText[1]);
            }
        }

        remote = pull(local);
        remote.task.setId(local.task.getId());
        if(remerge) {
            // transform local into remote
            local.task = remote.task;
            local.listId = remote.listId;
            local.taskId = remote.taskId;
            local.repeating = remote.repeating;
            local.taskSeriesId = remote.taskSeriesId;
        }

        return remote;
    }

    /** Create a task container for the given RtmTaskSeries */
    private MilkTaskContainer parseRemoteTask(RtmTaskSeries rtmTaskSeries) {
        Task task = new Task();
        RtmTask rtmTask = rtmTaskSeries.getTask();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        task.setValue(Task.TITLE, rtmTaskSeries.getName());
        task.setValue(Task.CREATION_DATE, DateUtilities.dateToUnixtime(rtmTask.getAdded()));
        task.setValue(Task.COMPLETION_DATE, DateUtilities.dateToUnixtime(rtmTask.getCompleted()));
        task.setValue(Task.DELETION_DATE, DateUtilities.dateToUnixtime(rtmTask.getDeleted()));
        if(rtmTask.getDue() != null) {
            task.setValue(Task.DUE_DATE,
                    task.createDueDate(rtmTask.getHasDueTime() ? Task.URGENCY_SPECIFIC_DAY_TIME :
                        Task.URGENCY_SPECIFIC_DAY, DateUtilities.dateToUnixtime(rtmTask.getDue())));
        } else {
            task.setValue(Task.DUE_DATE, 0L);
        }
        task.setValue(Task.IMPORTANCE, rtmTask.getPriority().ordinal());

        if(rtmTaskSeries.getTags() != null) {
            for(String tag : rtmTaskSeries.getTags()) {
                Metadata tagData = new Metadata();
                tagData.setValue(Metadata.KEY, MilkMetadataService.TAG_KEY);
                tagData.setValue(Metadata.VALUE1, tag);
                metadata.add(tagData);
            }
        }

        task.setValue(Task.NOTES, ""); //$NON-NLS-1$
        if(rtmTaskSeries.getNotes() != null && rtmTaskSeries.getNotes().getNotes().size() > 0) {
            boolean firstNote = true;
            Collections.reverse(rtmTaskSeries.getNotes().getNotes()); // reverse so oldest is first
            for(RtmTaskNote note : rtmTaskSeries.getNotes().getNotes()) {
                if(firstNote) {
                    firstNote = false;
                    task.setValue(Task.NOTES, MilkNoteHelper.toNoteField(note));
                } else
                    metadata.add(MilkNoteHelper.create(note));
            }
        }

        MilkTaskContainer container = new MilkTaskContainer(task, metadata, rtmTaskSeries);

        return container;
    }

    @Override
    protected MilkTaskContainer pull(MilkTaskContainer task) throws IOException {
        if(task.taskSeriesId == 0)
            throw new ServiceInternalException("Tried to read an invalid task"); //$NON-NLS-1$
        RtmTaskSeries rtmTask = rtmService.tasks_getTask(Long.toString(task.taskSeriesId),
                task.task.getValue(Task.TITLE));
        if(rtmTask != null)
            return parseRemoteTask(rtmTask);
        return null;
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- read / write
    // ----------------------------------------------------------------------

    @Override
    protected MilkTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return milkMetadataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(MilkTaskContainer task) throws IOException {
        milkMetadataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<MilkTaskContainer> tasks, MilkTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            MilkTaskContainer task = tasks.get(i);
            if(AndroidUtilities.equals(task.listId, target.listId) &&
                    AndroidUtilities.equals(task.taskSeriesId, target.taskSeriesId) &&
                    AndroidUtilities.equals(task.taskId, target.taskId))
                return i;
        }
        return -1;
    }

    @Override
    protected int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.rmilk_notification_title);
        Intent intent = new Intent(context, MilkPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return 0;
    }

    @Override
    protected void transferIdentifiers(MilkTaskContainer source,
            MilkTaskContainer destination) {
        destination.listId = source.listId;
        destination.taskSeriesId = source.taskSeriesId;
        destination.taskId = source.taskId;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper classes
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==115?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }

}
