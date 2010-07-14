/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

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
import com.todoroo.astrid.api.SynchronizationProvider;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.rmilk.MilkLoginActivity;
import com.todoroo.astrid.rmilk.Utilities;
import com.todoroo.astrid.rmilk.MilkLoginActivity.SyncLoginCallback;
import com.todoroo.astrid.rmilk.api.ApplicationInfo;
import com.todoroo.astrid.rmilk.api.ServiceImpl;
import com.todoroo.astrid.rmilk.api.ServiceInternalException;
import com.todoroo.astrid.rmilk.api.data.RtmList;
import com.todoroo.astrid.rmilk.api.data.RtmLists;
import com.todoroo.astrid.rmilk.api.data.RtmTask;
import com.todoroo.astrid.rmilk.api.data.RtmTaskList;
import com.todoroo.astrid.rmilk.api.data.RtmTaskSeries;
import com.todoroo.astrid.rmilk.api.data.RtmTasks;
import com.todoroo.astrid.rmilk.api.data.RtmAuth.Perms;
import com.todoroo.astrid.rmilk.api.data.RtmTask.Priority;
import com.todoroo.astrid.rmilk.data.MilkDataService;
import com.todoroo.astrid.rmilk.data.MilkTask;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class RTMSyncProvider extends SynchronizationProvider {

    protected ServiceImpl rtmService = null;
    protected String timeline = null;
    protected MilkDataService dataService = null;

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    public RTMSyncProvider() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- public methods
    // ----------------------------------------------------------------------

    @Override
    public void synchronize() {
        Context context = ContextManager.getContext();
        dataService = new MilkDataService(context);

    	// authenticate the user. this will automatically call the next step
        authenticate(context);
    }

    /**
     * Sign out of RTM, deleting all synchronization metadata
     */
    public void signOut() {
        Utilities.setToken(null);
        Utilities.setLastSyncDate(null);
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
     * @param showErrorIfNeeded
     *            whether to display a dialog
     */
    private void handleRtmException(Context context, String tag, Exception e,
            boolean showErrorIfNeeded) {
        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

        // occurs when network error
        } else if(e instanceof ServiceInternalException &&
                ((ServiceInternalException)e).getEnclosedException() instanceof
                IOException) {
            Exception enclosedException = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-ioexception", enclosedException); //$NON-NLS-1$
            if(showErrorIfNeeded) {
                showError(context, enclosedException,
                        context.getString(R.string.rmilk_ioerror));
            }
        } else {
            if(e instanceof ServiceInternalException)
                e = ((ServiceInternalException)e).getEnclosedException();
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
            if(showErrorIfNeeded)
                showError(context, e, null);
        }
    }

    /** Perform authentication with RTM. Will open the SyncBrowser if necessary */
    @SuppressWarnings("nls")
    private void authenticate(final Context context) {
        final Resources r = context.getResources();
        FlurryAgent.onEvent("rtm-started");

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
                        performSync(context);

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
                context.startActivity(intent);

            } else {
                performSync(context);
            }
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleRtmException(context, "rtm-authenticate", e, true);
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    private void performSync(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                performSyncInNewThread(context);
            }
        }).start();
    }

    protected void performSyncInNewThread(final Context context) {
        try {
            // get RTM timeline
            timeline = rtmService.timelines_create();

            // load RTM lists
            RtmLists lists = rtmService.lists_getList();
            dataService.setLists(lists);

            // read all tasks
            ArrayList<Task> remoteChanges = new ArrayList<Task>();
            Date lastSyncDate = Utilities.getLastSyncDate();
            boolean shouldSyncIndividualLists = false;
            String filter = null;
            if(lastSyncDate == null)
                filter = "status:incomplete"; //$NON-NLS-1$ // 1st time sync: get unfinished tasks

            // try the quick synchronization
            try {
                Thread.sleep(2000); // throttle
                RtmTasks tasks = rtmService.tasks_getList(null, filter, lastSyncDate);
                addTasksToList(tasks, remoteChanges);
            } catch (Exception e) {
                handleRtmException(context, "rtm-quick-sync", e, false); //$NON-NLS-1$
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
                        handleRtmException(context, "rtm-indiv-sync", e, true); //$NON-NLS-1$
                        continue;
                    }
                }
            }

            SyncData syncData = populateSyncData(remoteChanges);
            synchronizeTasks(syncData);

            Date syncTime = new Date(System.currentTimeMillis());
            Utilities.setLastSyncDate(syncTime);

            FlurryAgent.onEvent("rtm-sync-finished"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleRtmException(context, "rtm-sync", e, true); //$NON-NLS-1$
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper methods
    // ----------------------------------------------------------------------

    /**
     * Populate SyncData data structure
     */
    private SyncData populateSyncData(ArrayList<Task> remoteTasks) {
        // all synchronized properties
        Property<?>[] properties = new Property<?>[] {
                Task.ID,
                Task.TITLE,
                Task.IMPORTANCE,
                Task.DUE_DATE,
                Task.CREATION_DATE,
                Task.COMPLETION_DATE,
                Task.DELETION_DATE,
                MilkTask.LIST_ID,
                MilkTask.TASK_SERIES_ID,
                MilkTask.TASK_ID,
                MilkTask.REPEATING,
                // TODO tags
        };

        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(properties);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(properties);

        return new SyncData(properties, remoteTasks, localCreated, localUpdated);
    }

    /**
     * Add the tasks read from RTM to the given list
     */
    private void addTasksToList(RtmTasks tasks, ArrayList<Task> list) {
        for (RtmTaskList taskList : tasks.getLists()) {
            for (RtmTaskSeries taskSeries : taskList.getSeries()) {
                Task remoteTask = parseRemoteTask(taskSeries);
                list.add(remoteTask);
            }
        }
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(Task task, Property<?> property, Task remoteTask) {
        if(!task.containsValue(property))
            return false;

        if(remoteTask == null)
            return true;
        if(!remoteTask.containsValue(property))
            return true;
        return !AndroidUtilities.equals(task.getValue(property), remoteTask.getValue(property));
    }

    @Override
    protected void create(Task task) throws IOException {
        String listId = null;
        if(task.containsValue(MilkTask.LIST_ID))
            listId = Long.toString(task.getValue(MilkTask.LIST_ID));

        RtmTaskSeries rtmTask = rtmService.tasks_add(timeline, listId,
                task.getValue(Task.TITLE));
        push(task, parseRemoteTask(rtmTask));
    }

    /** Send changes for the given TaskProxy across the wire */
    @Override
    protected void push(Task task, Task remoteTask) throws IOException {
        RtmId id = new RtmId(task);

        // fetch remote task for comparison
        if(remoteTask == null) {
            remoteTask = read(task);
        }

        if(shouldTransmit(task, Task.TITLE, remoteTask))
            rtmService.tasks_setName(timeline, id.listId, id.taskSeriesId,
                    id.taskId, task.getValue(Task.TITLE));
        if(shouldTransmit(task, Task.IMPORTANCE, remoteTask))
            rtmService.tasks_setPriority(timeline, id.listId, id.taskSeriesId,
                    id.taskId, Priority.values()[task.getValue(Task.IMPORTANCE)]);
        if(shouldTransmit(task, Task.DUE_DATE, remoteTask))
            rtmService.tasks_setDueDate(timeline, id.listId, id.taskSeriesId,
                    id.taskId, DateUtilities.unixtimeToDate(task.getValue(Task.DUE_DATE)),
                    task.hasDueTime());
        if(shouldTransmit(task, Task.COMPLETION_DATE, remoteTask)) {
            if(task.getValue(Task.COMPLETION_DATE) == 0)
                rtmService.tasks_uncomplete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
            else
                rtmService.tasks_complete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
        }
        if(shouldTransmit(task, Task.DELETION_DATE, remoteTask))
            rtmService.tasks_delete(timeline, id.listId, id.taskSeriesId,
                    id.taskId);

        if(shouldTransmit(task, MilkTask.LIST_ID, remoteTask) && remoteTask != null)
            rtmService.tasks_moveTo(timeline, Long.toString(remoteTask.getValue(MilkTask.LIST_ID)),
                    id.listId, id.taskSeriesId, id.taskId);
    }

    /** Create a task proxy for the given RtmTaskSeries */
    private Task parseRemoteTask(RtmTaskSeries rtmTaskSeries) {
        Task task = new Task();

        task.setValue(MilkTask.LIST_ID, Long.parseLong(rtmTaskSeries.getList().getId()));
        task.setValue(MilkTask.TASK_SERIES_ID, Long.parseLong(rtmTaskSeries.getId()));
        task.setValue(Task.TITLE, rtmTaskSeries.getName());
        task.setValue(MilkTask.REPEATING, rtmTaskSeries.hasRecurrence() ? 1 : 0);

        RtmTask rtmTask = rtmTaskSeries.getTask();
        if(rtmTask != null) {
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
        } else {
            // error in upstream code, try to handle gracefully
            Log.e("rtmsync", "Got null task parsing remote task series",  //$NON-NLS-1$//$NON-NLS-2$
                    new Throwable());
        }

        return task;
    }

    @Override
    protected Task matchTask(ArrayList<Task> tasks, Task target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            Task task = tasks.get(i);
            if(task.getValue(MilkTask.LIST_ID).equals(target.getValue(MilkTask.LIST_ID)) &&
                    task.getValue(MilkTask.TASK_SERIES_ID).equals(target.getValue(MilkTask.TASK_SERIES_ID)) &&
                    task.getValue(MilkTask.TASK_ID).equals(target.getValue(MilkTask.TASK_ID)))
                return task;
        }
        return null;
    }

    @Override
    protected Task read(Task task) throws IOException {
        RtmTaskSeries rtmTask = rtmService.tasks_getTask(Long.toString(task.getValue(MilkTask.TASK_SERIES_ID)),
                task.getValue(Task.TITLE));
        if(rtmTask != null)
            return parseRemoteTask(rtmTask);
        return null;
    }

    @Override
    protected void transferIdentifiers(Task source, Task destination) {
        destination.setValue(MilkTask.LIST_ID, source.getValue(MilkTask.LIST_ID));
        destination.setValue(MilkTask.TASK_SERIES_ID, source.getValue(MilkTask.TASK_SERIES_ID));
        destination.setValue(MilkTask.TASK_ID, source.getValue(MilkTask.TASK_ID));
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper classes
    // ----------------------------------------------------------------------

    /** Helper class for storing RTM id's */
    private static class RtmId {
        public String taskId;
        public String taskSeriesId;
        public String listId;

        public RtmId(Task task) {
            taskId = Long.toString(task.getValue(MilkTask.TASK_ID));
            taskSeriesId = Long.toString(task.getValue(MilkTask.TASK_SERIES_ID));
            listId = Long.toString(task.getValue(MilkTask.LIST_ID));
        }
    }

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==115?_-1:_;_=((_>=97)&&(_<=123)?((_-83)%27+97):_);return
        TextUtils.htmlEncode(____==31?___:stripslashes(____+1,__.substring(1),___+((char)_)));
    }

}
