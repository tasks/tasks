/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.rmilk.MilkPreferences;
import com.todoroo.astrid.rmilk.MilkUtilities;
import com.todoroo.astrid.rmilk.api.ServiceInternalException;
import com.todoroo.astrid.rmilk.api.data.RtmTask;
import com.todoroo.astrid.rmilk.api.data.RtmTaskList;
import com.todoroo.astrid.rmilk.api.data.RtmTaskNote;
import com.todoroo.astrid.rmilk.api.data.RtmTaskSeries;
import com.todoroo.astrid.rmilk.api.data.RtmTasks;
import com.todoroo.astrid.rmilk.api.data.RtmTask.Priority;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkNote;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.Preferences;

public class ProducteevSyncProvider extends SynchronizationProvider<ProducteevTaskContainer> {

    private MilkDataService dataService = null;
    private ProducteevInvoker invoker = null;
    private int defaultDashboard;

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    @Autowired
    protected DialogUtilities dialogUtilities;

    public ProducteevSyncProvider() {
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
        ProducteevUtilities.setToken(null);
        ProducteevUtilities.clearLastSyncDate();

        dataService.clearMetadata(); // TODO clear metadata
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
        ProducteevUtilities.setLastError(e.toString());

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
        FlurryAgent.onEvent("producteev-started");

        ProducteevUtilities.recordSyncStart();

        try {
            String authToken = ProducteevUtilities.getToken();

            String z = stripslashes(0, "71o3346pr40o5o4nt4n7t6n287t4op28","2");
            String v = stripslashes(2, "9641n76n9s1736q1578q1o1337q19233","4ae");
            invoker = new ProducteevInvoker(z, v);

            // check if we have a token & it works
            if(authToken != null) {
                invoker.setToken(authToken);
            }

            if(authToken == null) {
                String email = Preferences.getStringValue(R.string.producteev_PPr_email);
                String password = Preferences.getStringValue(R.string.producteev_PPr_password);

                invoker.authenticate(email, password);
            }

            performSync();
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("rtm-authenticate", e, true);
        } finally {
            ProducteevUtilities.stopOngoing();
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    @SuppressWarnings("nls")
    protected void performSync() {
        try {
            // load user information
            JSONObject user = invoker.usersView(null);
            defaultDashboard = user.getJSONObject("user").getInt("default_dashboard");

            // read all tasks
            JSONObject tasks = invoker.tasksShowList(defaultDashboard,
                    ProducteevUtilities.getLastServerSyncTime());

            SyncData<ProducteevTaskContainer> syncData = populateSyncData(tasks);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            MilkUtilities.recordSuccessfulSync();

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
    private SyncData<ProducteevTaskContainer> populateSyncData(ArrayList<ProducteevTaskContainer> remoteTasks) {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<ProducteevTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    /**
     * Add the tasks read from RTM to the given list
     */
    private void addTasksToList(RtmTasks tasks, ArrayList<ProducteevTaskContainer> list) {
        for (RtmTaskList taskList : tasks.getLists()) {
            for (RtmTaskSeries taskSeries : taskList.getSeries()) {
                ProducteevTaskContainer remoteTask = parseRemoteTask(taskSeries);
                dataService.findLocalMatch(remoteTask);
                list.add(remoteTask);
            }
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected void create(ProducteevTaskContainer task) throws IOException {
        String listId = null;
        if(task.dashboard > 0)
            listId = Long.toString(task.listId);
        RtmTaskSeries rtmTask = rtmService.tasks_add(timeline, listId,
                task.task.getValue(Task.TITLE));
        ProducteevTaskContainer newRemoteTask = parseRemoteTask(rtmTask);
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
    protected void push(ProducteevTaskContainer local, ProducteevTaskContainer remote) throws IOException {
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
                    taskId, Priority.values()[local.task.getValue(Task.IMPORTANCE)]);
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

        if(remerge) {
            remote = pull(local);
            remote.task.setId(local.task.getId());
            write(remote);
        }
    }

    /** Create a task container for the given RtmTaskSeries */
    private ProducteevTaskContainer parseRemoteTask(RtmTaskSeries rtmTaskSeries) {
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

        ProducteevTaskContainer container = new ProducteevTaskContainer(task, metadata, rtmTaskSeries);

        return container;
    }

    @Override
    protected ProducteevTaskContainer pull(ProducteevTaskContainer task) throws IOException {
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
    protected ProducteevTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return dataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(ProducteevTaskContainer task) throws IOException {
        dataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<ProducteevTaskContainer> tasks, ProducteevTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            ProducteevTaskContainer task = tasks.get(i);
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
    protected void transferIdentifiers(ProducteevTaskContainer source,
            ProducteevTaskContainer destination) {
        destination.listId = source.listId;
        destination.taskSeriesId = source.taskSeriesId;
        destination.taskId = source.taskId;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper classes
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==116?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }

}
