/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksBackgroundService;
import com.todoroo.astrid.gtasks.GtasksList;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.gtasks.GoogleConnectionManager;
import com.todoroo.gtasks.GoogleLoginException;
import com.todoroo.gtasks.GoogleTaskService;
import com.todoroo.gtasks.GoogleTaskTask;
import com.todoroo.gtasks.GoogleTaskView;
import com.todoroo.gtasks.GoogleTasksException;
import com.todoroo.gtasks.actions.Action;
import com.todoroo.gtasks.actions.Actions;
import com.todoroo.gtasks.actions.GetTasksAction;
import com.todoroo.gtasks.actions.ListAction;
import com.todoroo.gtasks.actions.ListActions;
import com.todoroo.gtasks.actions.ListActions.TaskBuilder;
import com.todoroo.gtasks.actions.ListActions.TaskCreator;
import com.todoroo.gtasks.actions.ListActions.TaskModifier;
import com.todoroo.gtasks.actions.ListCreationAction;

@SuppressWarnings("nls")
public class GtasksSyncProvider extends SyncProvider<GtasksTaskContainer> {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    /** google task service fields */
    private GoogleTaskService taskService = null;
    private static final Actions a = new Actions();
    private static final ListActions l = new ListActions();

    /** tasks to read id for */
    ArrayList<GtasksTaskContainer> createdWithoutId;
    Semaphore pushedTaskSemaphore = new Semaphore(0);
    AtomicInteger pushedTaskCount = new AtomicInteger(0);

    static {
        AstridDependencyInjector.initialize();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ utility methods
    // ----------------------------------------------------------------------

    @Override
    protected SyncProviderUtilities getUtilities() {
        return gtasksPreferenceService;
    }

    /**
     * Sign out of service, deleting all synchronization metadata
     */
    public void signOut() {
        gtasksPreferenceService.clearLastSyncDate();
        gtasksPreferenceService.setToken(null);

        gtasksMetadataService.clearMetadata();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * initiate sync in background
     */
    @Override
    protected void initiateBackground() {
        try {
            String authToken = gtasksPreferenceService.getToken();

            final GoogleConnectionManager connectionManager;
            if(authToken == null) {
                Log.e("astrid-sync", "No token, unable to sync");
                return;
            } else {
                connectionManager = new GoogleConnectionManager(
                        Preferences.getStringValue(GtasksPreferenceService.PREF_USER_NAME),
                        Preferences.getStringValue(GtasksPreferenceService.PREF_PASSWORD),
                        !Preferences.getBoolean(GtasksPreferenceService.PREF_IS_DOMAIN, false));
            }

            taskService = new GoogleTaskService(connectionManager);
            performSync();
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("gtasks-authenticate", e, true);
        } finally {
            gtasksPreferenceService.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @Override
    protected void initiateManual(final Activity activity) {
        String authToken = gtasksPreferenceService.getToken();
        gtasksPreferenceService.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            Intent intent = new Intent(activity, GtasksLoginActivity.class);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(null, null,
                activity, GtasksBackgroundService.class));
            activity.finish();
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        StatisticsService.reportEvent("gtasks-started");
        gtasksPreferenceService.recordSyncStart();
        if(Constants.DEBUG)
            Log.e("gtasks-debug", "- -------- SYNC STARTED");
        createdWithoutId = new ArrayList<GtasksTaskContainer>();

        try {
            GoogleTaskView taskView = taskService.getTaskView();
            getActiveList(taskView);

            gtasksListService.updateLists(taskView.getAllLists());

            gtasksTaskListUpdater.createParentSiblingMaps();

            // read non-deleted tasks for each list
            SyncData<GtasksTaskContainer> syncData = populateSyncData();
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            gtasksTaskListUpdater.updateAllMetadata();

            gtasksPreferenceService.recordSuccessfulSync();
            StatisticsService.reportEvent("gtasks-sync-finished"); //$NON-NLS-1$

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            if(Constants.DEBUG)
                Log.e("gtasks-debug", "- ------ SYNC FINISHED");
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("gtasks-sync", e, true); //$NON-NLS-1$
        }
    }

    private void getActiveList(GoogleTaskView taskView) throws JSONException,
            IOException, GoogleLoginException {
        String listId;
        if(taskView.getActiveTaskList() != null && taskView.getActiveTaskList().getInfo() != null)
            listId = taskView.getActiveTaskList().getInfo().getId();
        else if(taskView.getAllLists().length == 0) {
            ListCreationAction createList = a.createList(0, ContextManager.getString(R.string.app_name));
            if(Constants.DEBUG)
                Log.e("gtasks-debug", "ACTION: createList(4)");
            taskService.executeActions(createList);
            listId = createList.getNewId();
        } else {
            listId = taskView.getAllLists()[0].getId();
        }

        Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, listId);
    }

    @Override
    protected void readRemotelyUpdated(SyncData<GtasksTaskContainer> data)
            throws IOException {

        // wait for pushed threads
        try {
            pushedTaskSemaphore.acquire(pushedTaskCount.get());
        } catch (InterruptedException e) {
            return;
        }

        // first, pull all tasks. then we can write them
        // include deleted tasks so we can delete them in astrid
        data.remoteUpdated = readAllRemoteTasks(true);

        // match remote tasks to locally created tasks
        HashMap<String, GtasksTaskContainer> locals = new HashMap<String, GtasksTaskContainer>();
        for(GtasksTaskContainer task : createdWithoutId) {
            locals.put(task.task.getValue(Task.TITLE), task);
        }
        ArrayList<Action> moveActions = new ArrayList<Action>();
        for(GtasksTaskContainer remote : data.remoteUpdated) {
            if(remote.task.getId() < 1) {
                GtasksTaskContainer local = locals.get(remote.task.getValue(Task.TITLE));
                if(local != null) {
                    if(Constants.DEBUG)
                        Log.e("gtasks-debug", "FOUND LOCAL - " + remote.task.getValue(Task.TITLE));
                    remote.task.setId(local.task.getId());
                }
            }
        }
        if(moveActions.size() > 0) {
            try {
                taskService.executeActions(moveActions.toArray(new Action[moveActions.size()]));
            } catch (JSONException e) {
                Log.e("gtasks-sync", "Error Running Local Action", e);
            }
        }

        super.readRemotelyUpdated(data);
    }


    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.REMINDER_FLAGS,
            Task.NOTES,
    };

    /**
     * Populate SyncData data structure
     * @throws JSONException
     */
    private SyncData<GtasksTaskContainer> populateSyncData() throws JSONException,
            GoogleLoginException, IOException {

        // fetch remote tasks
        ArrayList<GtasksTaskContainer> remoteTasks = readAllRemoteTasks(false);

        // fetch locally created tasks
        TodorooCursor<Task> localCreated = gtasksMetadataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = gtasksMetadataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<GtasksTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    private ArrayList<GtasksTaskContainer> readAllRemoteTasks(final boolean includeDeleted) {
        final ArrayList<GtasksTaskContainer> remoteTasks = new ArrayList<GtasksTaskContainer>();
        final Semaphore listsFinished = new Semaphore(0);

        // launch threads
        StoreObject[] lists = gtasksListService.getLists();
        for(final StoreObject dashboard : lists) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String listId = dashboard.getValue(GtasksList.REMOTE_ID);
                        if(Constants.DEBUG)
                            Log.e("gtasks-debug", "ACTION: getTasks, " + listId);
                        GetTasksAction action = new GetTasksAction(listId, includeDeleted);
                        taskService.executeActions(action);
                        List<GoogleTaskTask> list = action.getGoogleTasks();
                        addRemoteTasksToList(list, remoteTasks);
                    } catch (Exception e) {
                        handleException("read-remotes", e, false);
                    } finally {
                        listsFinished.release();
                    }
                }
            }).start();
        }

        try {
            listsFinished.acquire(lists.length);
        } catch (InterruptedException e) {
            handleException("wait-for-remotes", e, false);
        }
        return remoteTasks;
    }

    private void addRemoteTasksToList(List<GoogleTaskTask> list,
            ArrayList<GtasksTaskContainer> remoteTasks) {

        int order = 0;
        HashMap<String, String> parents = new HashMap<String, String>();
        HashMap<String, Integer> indentation = new HashMap<String, Integer>();
        HashMap<String, String> parentToPriorSiblingMap = new HashMap<String, String>();

        for(GoogleTaskTask remoteTask : list) {
            if(TextUtils.isEmpty(remoteTask.getName()))
                continue;

            GtasksTaskContainer container = parseRemoteTask(remoteTask);
            String id = remoteTask.getId();

            // update parents, prior sibling
            for(String child : remoteTask.getChild_ids())
                parents.put(child, id);
            String parent = parents.get(id); // can be null, which means top level task
            container.parentId = parent;
            if(parentToPriorSiblingMap.containsKey(parent))
                container.priorSiblingId = parentToPriorSiblingMap.get(parent);
            parentToPriorSiblingMap.put(parent, id);

            // update order, indent
            container.gtaskMetadata.setValue(GtasksMetadata.ORDER, order++);
            int indent = findIndentation(parents, indentation, id);
            indentation.put(id, indent);
            container.gtaskMetadata.setValue(GtasksMetadata.INDENT, indent);

            // update reminder flags for incoming remote tasks to prevent annoying
            if(container.task.hasDueDate() && container.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                container.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);
            gtasksMetadataService.findLocalMatch(container);
            remoteTasks.add(container);
        }
    }

    private int findIndentation(HashMap<String, String> parents,
            HashMap<String, Integer> indentation, String task) {
        if(indentation.containsKey(task))
            return indentation.get(task);

        if(!parents.containsKey(task))
            return 0;

        return findIndentation(parents, indentation, parents.get(task)) + 1;
    }

    @Override
    protected GtasksTaskContainer create(GtasksTaskContainer local) throws IOException {
        String list = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        if(local.gtaskMetadata.containsNonNullValue(GtasksMetadata.LIST_ID))
            list = local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);
        gtasksTaskListUpdater.updateParentAndSibling(local);
        local.gtaskMetadata.setValue(GtasksMetadata.ID, null);
        local.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, list);

        createdWithoutId.add(local);
        try {
            TaskCreator createdTask = l.createTask(local.task.getValue(Task.TITLE));
            createdTask.parentId(local.parentId);
            updateTaskHelper(local, null, createdTask);
            return local;
        } catch (JSONException e) {
            throw new GoogleTasksException(e);
        }
    }

    private void updateTaskHelper(final GtasksTaskContainer local,
            final GtasksTaskContainer remote, TaskBuilder<?> builder) throws IOException {

        final String idTask = local.gtaskMetadata.getValue(GtasksMetadata.ID);
        final String idList = local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);

        try {
            // set properties
            if(shouldTransmit(local, Task.DUE_DATE, remote))
                builder.taskDate(local.task.getValue(Task.DUE_DATE));
            if(shouldTransmit(local, Task.COMPLETION_DATE, remote))
                builder.completed(local.task.isCompleted());
            if(shouldTransmit(local, Task.DELETION_DATE, remote))
                builder.deleted(local.task.isDeleted());
            if(shouldTransmit(local, Task.NOTES, remote))
                builder.notes(local.task.getValue(Task.NOTES));

            // write task (and perform move action if requested)
            final ListAction action;
            if(builder instanceof TaskModifier) {
                if(Constants.DEBUG)
                    Log.e("gtasks-debug", "ACTION: task edit (6), " + idTask);
                action = ((TaskModifier) builder).done();
            } else if(builder instanceof TaskCreator) {
                if(Constants.DEBUG)
                    Log.e("gtasks-debug", "ACTION: task create (7), " + local.task.getValue(Task.TITLE));
                action = ((TaskCreator) builder).done();
            } else
                throw new GoogleTasksException("Unknown builder " + builder.getClass());

            pushedTaskCount.incrementAndGet();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!TextUtils.isEmpty(idTask) &&
                                !TextUtils.isEmpty(local.parentId) && (remote == null || local.parentId != remote.parentId ||
                                local.priorSiblingId != remote.priorSiblingId)) {
                            if(Constants.DEBUG)
                                Log.e("gtasks-debug", "ACTION: move(1) - " + idTask + ", " + local.parentId + ", " + local.priorSiblingId);
                            ListAction moveAction = l.move(idTask, local.parentId, local.priorSiblingId);
                            taskService.executeListActions(idList, action, moveAction);
                        } else if(action.toJson(idList).getJSONObject("entity_delta").length() > 0) {
                            taskService.executeListActions(idList, action);
                        }

                        // moving between lists
                        if(remote != null && !idList.equals(remote.gtaskMetadata.getValue(
                                GtasksMetadata.LIST_ID))) {
                            if(Constants.DEBUG)
                                Log.e("gtasks-debug", "ACTION: moveTask(5), " + idTask + ", " + idList + " to " +
                                    remote.gtaskMetadata.getValue(GtasksMetadata.LIST_ID));
                            taskService.executeActions(a.moveTask(idTask, idList,
                                    remote.gtaskMetadata.getValue(GtasksMetadata.LIST_ID), null));
                        }
                    } catch (IOException e) {
                        handleException("update-task", e, false);
                    } catch (JSONException e) {
                        handleException("update-task-json", e, false);
                    } finally {
                        pushedTaskSemaphore.release();
                    }
                }
            }).start();

        } catch (JSONException e) {
            throw new GoogleTasksException(e);
        }
    }

    /** Create a task container for the given RtmTaskSeries
     * @throws JSONException */
    private GtasksTaskContainer parseRemoteTask(GoogleTaskTask remoteTask) {
        Task task = new Task();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        task.setValue(Task.TITLE, remoteTask.getName());
        task.setValue(Task.CREATION_DATE, DateUtilities.now());
        task.setValue(Task.COMPLETION_DATE, remoteTask.getCompleted_date());
        task.setValue(Task.DELETION_DATE, remoteTask.isDeleted() ? DateUtilities.now() : 0);

        long dueDate = remoteTask.getTask_date();
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate));
        task.setValue(Task.NOTES, remoteTask.getNotes());
        task.setValue(Task.NOTES, remoteTask.getNotes());

        Metadata gtasksMetadata = GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID);
        gtasksMetadata.setValue(GtasksMetadata.ID, remoteTask.getId());
        gtasksMetadata.setValue(GtasksMetadata.LIST_ID, remoteTask.getList_id());

        GtasksTaskContainer container = new GtasksTaskContainer(task, metadata,
                gtasksMetadata);
        return container;
    }

    @Override
    protected GtasksTaskContainer pull(GtasksTaskContainer task) throws IOException {
        // we pull all tasks at the end, so here we just
        // return the task that was requested
        return task;
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     */
    @Override
    protected GtasksTaskContainer push(GtasksTaskContainer local, GtasksTaskContainer remote) throws IOException {
        try {
            gtasksTaskListUpdater.updateParentAndSibling(local);

            String id = local.gtaskMetadata.getValue(GtasksMetadata.ID);
            if(Constants.DEBUG)
                Log.e("gtasks-debug", "ACTION: modifyTask(3) - " + id);
            TaskModifier modifyTask = l.modifyTask(id);
            if(shouldTransmit(local, Task.TITLE, remote))
                modifyTask.name(local.task.getValue(Task.TITLE));
            updateTaskHelper(local, remote, modifyTask);

        } catch (JSONException e) {
            throw new GoogleTasksException(e);
        }

        return pull(remote);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- read / write
    // ----------------------------------------------------------------------

    @Override
    protected GtasksTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return gtasksMetadataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(GtasksTaskContainer task) throws IOException {
        //  merge astrid dates with google dates
        if(task.task.isSaved()) {
            Task local = PluginServices.getTaskService().fetchById(task.task.getId(), Task.DUE_DATE);
            mergeDates(task.task, local);
        }

        gtasksMetadataService.saveTaskAndMetadata(task);
    }

    /** pick up due time from local task */
    private void mergeDates(Task remote, Task local) {
        if(remote.hasDueDate() && local.hasDueTime()) {
            Date newDate = new Date(remote.getValue(Task.DUE_DATE));
            Date oldDate = new Date(local.getValue(Task.DUE_DATE));
            newDate.setHours(oldDate.getHours());
            newDate.setMinutes(oldDate.getMinutes());
            newDate.setSeconds(oldDate.getSeconds());
            remote.setValue(Task.DUE_DATE, remote.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    newDate.getTime()));
        }
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<GtasksTaskContainer> tasks, GtasksTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            GtasksTaskContainer task = tasks.get(i);
            if(AndroidUtilities.equals(task.gtaskMetadata.getValue(GtasksMetadata.ID),
                    target.gtaskMetadata.getValue(GtasksMetadata.ID)))
                return i;
        }
        return -1;
    }

    /**
     * Determine whether this task's property should be transmitted
     * @param task task to consider
     * @param property property to consider
     * @param remoteTask remote task proxy
     * @return
     */
    private boolean shouldTransmit(SyncContainer task, Property<?> property, SyncContainer remoteTask) {
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

    @Override
    protected int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.gtasks_notification_title);
        Intent intent = new Intent(context, GtasksPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return Constants.NOTIFICATION_SYNC;
    }

    @Override
    protected void transferIdentifiers(GtasksTaskContainer source,
            GtasksTaskContainer destination) {
        destination.gtaskMetadata = source.gtaskMetadata;
    }
}
