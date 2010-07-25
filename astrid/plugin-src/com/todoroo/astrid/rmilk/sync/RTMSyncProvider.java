/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.SynchronizationProvider;
import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.MilkLoginActivity;
import com.todoroo.astrid.rmilk.MilkPreferences;
import com.todoroo.astrid.rmilk.Utilities;
import com.todoroo.astrid.rmilk.MilkLoginActivity.SyncLoginCallback;
import com.todoroo.astrid.rmilk.api.ApplicationInfo;
import com.todoroo.astrid.rmilk.api.ServiceImpl;
import com.todoroo.astrid.rmilk.api.ServiceInternalException;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;
import com.todoroo.astrid.rmilk.api.data.RtmTask;
import com.todoroo.astrid.rmilk.api.data.RtmTaskList;
import com.todoroo.astrid.rmilk.api.data.RtmTaskNote;
import com.todoroo.astrid.rmilk.api.data.RtmTaskSeries;
import com.todoroo.astrid.rmilk.api.data.RtmTasks;
import com.todoroo.astrid.rmilk.api.data.RtmAuth.Perms;
import com.todoroo.astrid.rmilk.api.data.RtmTask.Priority;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkNote;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.tags.TagService;

public class RTMSyncProvider extends SynchronizationProvider<RTMTaskContainer> {

    private ServiceImpl rtmService = null;
    private String timeline = null;
    private MilkDataService dataService = null;

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected DialogUtilities dialogUtilities;

    public RTMSyncProvider() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- public methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of RTM, deleting all synchronization metadata
     */
    public void signOut() {
        Utilities.setToken(null);
        Utilities.clearLastSyncDate();

        dataService = MilkDataService.getInstance();
        dataService.clearMetadata();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- authentication
    // ----------------------------------------------------------------------

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
    @Override
    protected void handleException(String tag, Exception e, boolean showError) {
        Utilities.setLastError(e.toString());

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

        // occurs when network error
        } else if(e instanceof ServiceInternalException &&
                ((ServiceInternalException)e).getEnclosedException() instanceof
                IOException) {
            Exception enclosedException = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-ioexception", enclosedException); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, enclosedException,
                        context.getString(R.string.rmilk_ioerror));
            }
        } else {
            if(e instanceof ServiceInternalException)
                e = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, e, null);
            }
        }
    }

    @Override
    protected void initiate(Context context) {
        dataService = MilkDataService.getInstance();

        // authenticate the user. this will automatically call the next step
        authenticate(context);
    }

    /**
     * Perform authentication with RTM. Will open the SyncBrowser if necessary
     */
    @SuppressWarnings("nls")
    private void authenticate(final Context context) {
        final Resources r = context.getResources();
        FlurryAgent.onEvent("rtm-started");

        Utilities.recordSyncStart();

        try {
            String appName = null;
            String authToken = Utilities.getToken();
            String z = stripslashes(0,"q9883o3384n21snq17501qn38oo1r689", "b");
            String v = stripslashes(16,"19o2n020345219os","a");

            // check if we have a token & it works
            if(authToken != null) {
                rtmService = new ServiceImpl(new ApplicationInfo(
                        z, v, appName, authToken));
                if(!rtmService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                // try completing the authorization if it was partial
                if(rtmService != null) {
                    try {
                        String token = rtmService.completeAuthorization();
                        Utilities.setToken(token);
                        performSync();

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                // open up a dialog and have the user go to browser
                rtmService = new ServiceImpl(new ApplicationInfo(
                        z, v, appName));
                final String url = rtmService.beginAuthorization(Perms.delete);

                Intent intent = new Intent(context, MilkLoginActivity.class);
                MilkLoginActivity.setCallback(new SyncLoginCallback() {
                    public String verifyLogin(final Handler syncLoginHandler) {
                        if(rtmService == null) {
                            return null;
                        }

                        try {
                            String token = rtmService.completeAuthorization();
                            Utilities.setToken(token);
                            synchronize(context);
                            return null;
                        } catch (Exception e) {
                            // didn't work
                            exceptionService.reportError("rtm-verify-login", e);
                            rtmService = null;
                            if(e instanceof ServiceInternalException)
                                e = ((ServiceInternalException)e).getEnclosedException();
                            return r.getString(R.string.rmilk_MLA_error, e.getMessage());
                        }
                    }
                });
                intent.putExtra(MilkLoginActivity.URL_TOKEN, url);
                if(context instanceof Activity)
                    ((Activity)context).startActivityForResult(intent, 0);
                else
                    context.startActivity(intent);

            } else {
                performSync();
            }
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-authenticate", e, true);
        } finally {
            Utilities.stopOngoing();
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
            dataService.setLists(lists);

            // read all tasks
            ArrayList<RTMTaskContainer> remoteChanges = new ArrayList<RTMTaskContainer>();
            Date lastSyncDate = new Date(Utilities.getLastSyncDate());
            boolean shouldSyncIndividualLists = false;
            String filter = null;
            if(lastSyncDate.getTime() == 0)
                filter = "status:incomplete"; //$NON-NLS-1$ // 1st time sync: get unfinished tasks

            // try the quick synchronization
            try {
                Thread.sleep(2000); // throttle
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

            SyncData<RTMTaskContainer> syncData = populateSyncData(remoteChanges);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            Utilities.recordSuccessfulSync();

            FlurryAgent.onEvent("rtm-sync-finished"); //$NON-NLS-1$
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
    private SyncData<RTMTaskContainer> populateSyncData(ArrayList<RTMTaskContainer> remoteTasks) {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<RTMTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    /**
     * Add the tasks read from RTM to the given list
     */
    private void addTasksToList(RtmTasks tasks, ArrayList<RTMTaskContainer> list) {
        for (RtmTaskList taskList : tasks.getLists()) {
            for (RtmTaskSeries taskSeries : taskList.getSeries()) {
                RTMTaskContainer remoteTask = parseRemoteTask(taskSeries);
                dataService.findLocalMatch(remoteTask);
                list.add(remoteTask);
            }
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected void create(RTMTaskContainer task) throws IOException {
        String listId = null;
        if(task.listId > 0)
            listId = Long.toString(task.listId);
        RtmTaskSeries rtmTask = rtmService.tasks_add(timeline, listId,
                task.task.getValue(Task.TITLE));
        RTMTaskContainer newRemoteTask = parseRemoteTask(rtmTask);
        transferIdentifiers(newRemoteTask, task);
        push(task, newRemoteTask);
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(TaskContainer task, Property<?> property, TaskContainer remoteTask) {
        if(!task.task.containsValue(property))
            return false;

        if(remoteTask == null)
            return true;
        if(!remoteTask.task.containsValue(property))
            return true;
        return !AndroidUtilities.equals(task.task.getValue(property),
                remoteTask.task.getValue(property));
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     */
    @Override
    protected void push(RTMTaskContainer local, RTMTaskContainer remote) throws IOException {
        // fetch remote task for comparison
        if(remote == null)
            remote = pull(local);

        String listId = Long.toString(local.listId);
        String taskSeriesId = Long.toString(local.taskSeriesId);
        String taskId = Long.toString(local.taskId);

        if(remote != null && !AndroidUtilities.equals(local.listId, remote.listId))
            rtmService.tasks_moveTo(timeline, Long.toString(remote.listId),
                    listId, taskSeriesId, taskId);

        if(shouldTransmit(local, Task.TITLE, remote))
            rtmService.tasks_setName(timeline, listId, taskSeriesId,
                    taskId, local.task.getValue(Task.TITLE));
        if(shouldTransmit(local, Task.IMPORTANCE, remote))
            rtmService.tasks_setPriority(timeline, listId, taskSeriesId,
                    taskId, Priority.values()[local.task.getValue(Task.IMPORTANCE)]);
        if(shouldTransmit(local, Task.DUE_DATE, remote))
            rtmService.tasks_setDueDate(timeline, listId, taskSeriesId,
                    taskId, DateUtilities.unixtimeToDate(local.task.getValue(Task.DUE_DATE)),
                    local.task.hasDueTime());
        if(shouldTransmit(local, Task.COMPLETION_DATE, remote)) {
            if(local.task.getValue(Task.COMPLETION_DATE) == 0)
                rtmService.tasks_uncomplete(timeline, listId, taskSeriesId,
                        taskId);
            else
                rtmService.tasks_complete(timeline, listId, taskSeriesId,
                        taskId);
        }
        if(shouldTransmit(local, Task.DELETION_DATE, remote) &&
                local.task.getValue(Task.DELETION_DATE) > 0)
            rtmService.tasks_delete(timeline, listId, taskSeriesId,
                    taskId);

        // tags
        HashSet<String> localTags = new HashSet<String>();
        HashSet<String> remoteTags = new HashSet<String>();
        for(Metadata item : local.metadata)
            if(TagService.KEY.equals(item.getValue(Metadata.KEY)))
                localTags.add(item.getValue(TagService.TAG));
        if(remote != null && remote.metadata != null) {
            for(Metadata item : remote.metadata)
                if(TagService.KEY.equals(item.getValue(Metadata.KEY)))
                    remoteTags.add(item.getValue(TagService.TAG));
        }
        if(!localTags.equals(remoteTags)) {
            String[] tags = localTags.toArray(new String[localTags.size()]);
            rtmService.tasks_setTags(timeline, listId, taskSeriesId,
                    taskId, tags);
        }

        // notes
        if(shouldTransmit(local, Task.NOTES, remote)) {
            String[] titleAndText = MilkNote.fromNoteField(local.task.getValue(Task.NOTES));
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
    }

    /** Create a task container for the given RtmTaskSeries */
    private RTMTaskContainer parseRemoteTask(RtmTaskSeries rtmTaskSeries) {
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
                tagData.setValue(Metadata.KEY, TagService.KEY);
                tagData.setValue(TagService.TAG, tag);
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
                    task.setValue(Task.NOTES, MilkNote.toNoteField(note));
                } else
                    metadata.add(MilkNote.create(note));
            }
        }

        RTMTaskContainer container = new RTMTaskContainer(task, metadata, rtmTaskSeries);

        return container;
    }

    @Override
    protected RTMTaskContainer pull(RTMTaskContainer task) throws IOException {
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
    protected RTMTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return dataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(RTMTaskContainer task) throws IOException {
        dataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<RTMTaskContainer> tasks, RTMTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            RTMTaskContainer task = tasks.get(i);
            if(AndroidUtilities.equals(task.listId, target.listId) &&
                    AndroidUtilities.equals(task.taskSeriesId, target.taskSeriesId) &&
                    AndroidUtilities.equals(task.taskId, target.taskId))
                return i;
        }
        return -1;
    }

    @Override
    protected void updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.rmilk_notification_title);
        Intent intent = new Intent(context, MilkPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return ;
    }

    @Override
    protected void transferIdentifiers(RTMTaskContainer source,
            RTMTaskContainer destination) {
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
