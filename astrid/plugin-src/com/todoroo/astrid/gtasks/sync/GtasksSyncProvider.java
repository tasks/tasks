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

import com.google.api.services.tasks.v1.model.TaskList;
import com.google.api.services.tasks.v1.model.TaskLists;
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
import com.todoroo.astrid.dao.TaskDao;
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
import com.todoroo.astrid.gtasks.api.CreateRequest;
import com.todoroo.astrid.gtasks.api.GoogleTasksException;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import com.todoroo.astrid.gtasks.api.GtasksService;
import com.todoroo.astrid.gtasks.api.MoveListRequest;
import com.todoroo.astrid.gtasks.api.MoveRequest;
import com.todoroo.astrid.gtasks.api.PushRequest;
import com.todoroo.astrid.gtasks.api.UpdateRequest;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.gtasks.auth.GtasksTokenValidator;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class GtasksSyncProvider extends SyncProvider<GtasksTaskContainer> {

    @Autowired private GtasksListService gtasksListService;
    @Autowired private GtasksMetadataService gtasksMetadataService;
    @Autowired private GtasksPreferenceService gtasksPreferenceService;
    @Autowired private GtasksTaskListUpdater gtasksTaskListUpdater;

    /** google task service fields */
    private GtasksService taskService = null;

    public GtasksService getGtasksService() {
        return taskService;
    }

    /** tasks to read id for */
    ArrayList<GtasksTaskContainer> createdWithoutId;
    ArrayList<GtasksTaskContainer> createdWithoutParent;
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
        Preferences.setString(GtasksPreferenceService.PREF_USER_NAME, null);
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
            authToken = GtasksTokenValidator.validateAuthToken(authToken);
            gtasksPreferenceService.setToken(authToken);

            taskService = new GtasksService(authToken);
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
        boolean syncSuccess = false;
        gtasksPreferenceService.recordSyncStart();
        if(Constants.DEBUG)
            Log.e("gtasks-debug", "- -------- SYNC STARTED");
        createdWithoutId = new ArrayList<GtasksTaskContainer>();
        createdWithoutParent = new ArrayList<GtasksTaskContainer>();
        try {
            TaskLists allTaskLists = taskService.allGtaskLists();

            //TODO: do something with result of migration check?
            new GtasksLegacyMigrator(taskService, gtasksListService, allTaskLists).checkAndMigrateLegacy();

            getActiveList(allTaskLists);

            gtasksListService.updateLists(allTaskLists);

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

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
            if(Constants.DEBUG)
                Log.e("gtasks-debug", "- ------ SYNC FINISHED");
            syncSuccess = true;
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("gtasks-sync", e, true); //$NON-NLS-1$
        } finally {
            StatisticsService.reportEvent("gtasks-sync-finished",
                    "success", Boolean.toString(syncSuccess)); //$NON-NLS-1$
        }
    }

    private void getActiveList(TaskLists taskView) throws JSONException,
            IOException {
        String listId;
        if(taskView.items.size() == 0) {
            if(Constants.DEBUG)
                Log.e("gtasks-debug", "ACTION: createList(4)");
            TaskList newList = taskService.createGtaskList(ContextManager.getString(R.string.app_name));
            listId = newList.id;
        } else if (Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST) != null) {
            listId = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        } else {
            listId = "@default";
        }

        Preferences.setString(GtasksPreferenceService.PREF_DEFAULT_LIST, listId);
    }

    @Override
    protected void readRemotelyUpdated(SyncData<GtasksTaskContainer> data)
            throws IOException {

        // wait for pushed threads
        try {
            pushedTaskSemaphore.acquire(pushedTaskCount.get());
            pushedTaskCount.set(0);
        } catch (InterruptedException e) {
            return;
        }


        // first, pull all tasks. then we can write them
        // include deleted tasks so we can delete them in astrid
        data.remoteUpdated = readAllRemoteTasks(true);

        // match remote tasks to locally created tasks
        HashMap<String, GtasksTaskContainer> locals = new HashMap<String, GtasksTaskContainer>();
        HashMap<Long, String> localIdsToRemoteIds = new HashMap<Long, String>();
        for(GtasksTaskContainer task : createdWithoutId) {
            locals.put(task.gtaskMetadata.getValue(GtasksMetadata.ID), task);
            localIdsToRemoteIds.put(task.task.getId(), task.gtaskMetadata.getValue(GtasksMetadata.ID));
        }

        verifyCreatedOrder(locals, localIdsToRemoteIds);

        for(GtasksTaskContainer remote : data.remoteUpdated) {
            if(remote.task.getId() < 1) {
                GtasksTaskContainer local = locals.get(remote.gtaskMetadata.getValue(GtasksMetadata.ID));
                if(local != null) {
                    if(Constants.DEBUG)
                        Log.e("gtasks-debug", "FOUND LOCAL - " + remote.task.getId());
                    remote.task.setId(local.task.getId());
                }
            }
        }

        super.readRemotelyUpdated(data);
    }

    private void verifyCreatedOrder(HashMap<String, GtasksTaskContainer> locals,
            HashMap<Long, String> localIdsToRemoteIds) throws IOException {
        for (GtasksTaskContainer t : createdWithoutParent) {
            String toMove = t.gtaskMetadata.getValue(GtasksMetadata.ID);
            String listId = t.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);
            long parentTask = t.gtaskMetadata.getValue(GtasksMetadata.PARENT_TASK);
            if (parentTask > 0) {
                String remoteParent = localIdsToRemoteIds.get(parentTask);
                MoveRequest move = new MoveRequest(taskService, toMove, listId, remoteParent, null);
                move.executePush();
            }
        }
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
    private SyncData<GtasksTaskContainer> populateSyncData() throws JSONException, IOException {

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
                        List<com.google.api.services.tasks.v1.model.Task> list = taskService.getAllGtasksFromListId(listId, includeDeleted).items;
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

    private void addRemoteTasksToList(List<com.google.api.services.tasks.v1.model.Task> remoteTasks,
            ArrayList<GtasksTaskContainer> list) {

        if (remoteTasks != null) {
            int order = 0;
            //HashMap<String, List<String>> children = new HashMap<String, List<String>>();
            HashMap<String, com.google.api.services.tasks.v1.model.Task> idsToTasks = new HashMap<String, com.google.api.services.tasks.v1.model.Task>();
            HashMap<String, Integer> indentation = new HashMap<String, Integer>();
            HashMap<String, String> parentToPriorSiblingMap = new HashMap<String, String>();


            //Build map of String ids to task objects
            for (com.google.api.services.tasks.v1.model.Task task : remoteTasks) {
                String id = task.id;
                idsToTasks.put(id, task);
            }

            for(com.google.api.services.tasks.v1.model.Task remoteTask : remoteTasks) {
                if(TextUtils.isEmpty(remoteTask.title))
                    continue;

                GtasksTaskContainer container = parseRemoteTask(remoteTask);
                String id = remoteTask.id;

                // update parents, prior sibling
                String parent = remoteTask.parent; // can be null, which means top level task
                container.parentId = parent;
                if(parentToPriorSiblingMap.containsKey(parent))
                    container.priorSiblingId = parentToPriorSiblingMap.get(parent);
                parentToPriorSiblingMap.put(parent, id);

                // update order, indent
                container.gtaskMetadata.setValue(GtasksMetadata.ORDER, order++);
                int indent = findIndentation(idsToTasks, indentation, remoteTask);
                indentation.put(id, indent);
                container.gtaskMetadata.setValue(GtasksMetadata.INDENT, indent);

                // update reminder flags for incoming remote tasks to prevent annoying
                if(container.task.hasDueDate() && container.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                    container.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

                gtasksMetadataService.findLocalMatch(container);
                synchronized(list) {
                    list.add(container);
                }
            }
        }
    }

    private int findIndentation(HashMap<String, com.google.api.services.tasks.v1.model.Task> idsToTasks,
            HashMap<String, Integer> indentation, com.google.api.services.tasks.v1.model.Task task) {
        if(indentation.containsKey(task.id))
            return indentation.get(task.id);

        if(TextUtils.isEmpty(task.parent))
            return 0;

        return findIndentation(idsToTasks, indentation, idsToTasks.get(task.parent)) + 1;
    }

    @Override
    protected GtasksTaskContainer create(GtasksTaskContainer local) throws IOException {
        String listId = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        if(local.gtaskMetadata.containsNonNullValue(GtasksMetadata.LIST_ID))
            listId = local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);
        gtasksTaskListUpdater.updateParentAndSibling(local);
        local.gtaskMetadata.setValue(GtasksMetadata.ID, null);
        local.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, listId);

        createdWithoutId.add(local);
        if (local.gtaskMetadata.containsNonNullValue(GtasksMetadata.PARENT_TASK)) {
            createdWithoutParent.add(local);
        }
        com.google.api.services.tasks.v1.model.Task createdTask = new com.google.api.services.tasks.v1.model.Task();

        CreateRequest createRequest = new CreateRequest(taskService, listId, createdTask, local.parentId, local.priorSiblingId);
        updateTaskHelper(local, null, createRequest);
        return local;
    }//*/

    private void localPropertiesToModel(GtasksTaskContainer local, GtasksTaskContainer remote,
            com.google.api.services.tasks.v1.model.Task model) {
        if(shouldTransmit(local, Task.TITLE, remote))
            model.title = local.task.getValue(Task.TITLE);
        if(shouldTransmit(local, Task.DUE_DATE, remote))
            model.due = GtasksApiUtilities.unixTimeToGtasksTime(local.task.getValue(Task.DUE_DATE));
        if(shouldTransmit(local, Task.COMPLETION_DATE, remote)) {
            model.completed = GtasksApiUtilities.unixTimeToGtasksTime(local.task.getValue(Task.COMPLETION_DATE));
            model.status = (local.task.isCompleted() ? "completed" : "needsAction");
        }
        if(shouldTransmit(local, Task.DELETION_DATE, remote))
            model.deleted = local.task.isDeleted();
        if(shouldTransmit(local, Task.NOTES, remote))
            model.notes = local.task.getValue(Task.NOTES);
    }

    private void updateTaskHelper(final GtasksTaskContainer local,
            final GtasksTaskContainer remote, final PushRequest request) throws IOException {

        final String idTask = local.gtaskMetadata.getValue(GtasksMetadata.ID);
        final String idList = local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);

        try {
            // set properties
            localPropertiesToModel(local, null, request.getToPush());

            // write task (and perform move action if requested)
            if(request instanceof UpdateRequest) {
                if(Constants.DEBUG)
                    Log.e("gtasks-debug", "ACTION: task edit (6), " + idTask);
            } else if(request instanceof CreateRequest) {
                if(Constants.DEBUG)
                    Log.e("gtasks-debug", "ACTION: task create (7), " + local.task.getValue(Task.TITLE));
            } else
                throw new GoogleTasksException("Unknown request type " + request.getClass());

            pushedTaskCount.incrementAndGet();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String newIdTask = idTask;
                    try {
                        if (request instanceof CreateRequest) {
                            com.google.api.services.tasks.v1.model.Task createResult = request.executePush();
                            newIdTask = createResult.id;
                            local.gtaskMetadata.setValue(GtasksMetadata.ID, newIdTask);
                        }
                        if(!TextUtils.isEmpty(newIdTask) && (remote == null || local.parentId != remote.parentId ||
                                local.priorSiblingId != remote.priorSiblingId)) {
                            if(Constants.DEBUG)
                                Log.e("gtasks-debug", "ACTION: move(1) - " + newIdTask + ", " + local.parentId + ", " + local.priorSiblingId);
                            //This case basically defaults to whatever local settings are. Future versions could try and merge better
                            MoveRequest moveRequest = new MoveRequest(taskService, newIdTask, idList, local.parentId, local.priorSiblingId);
                            moveRequest.executePush();

                        }
                        if (request instanceof UpdateRequest) {
                            request.executePush();
                        }

                        //Strategy--delete, migrate properties, recreate, update local AND remote ids; happens in MoveListRequest
                        if(remote != null && !idList.equals(remote.gtaskMetadata.getValue(
                                GtasksMetadata.LIST_ID))) {
                            if(Constants.DEBUG)
                                Log.e("gtasks-debug", "ACTION: moveTask(5), " + newIdTask + ", " + idList + " to " +
                                    remote.gtaskMetadata.getValue(GtasksMetadata.LIST_ID));
                            MoveListRequest moveList = new MoveListRequest(taskService, newIdTask, remote.gtaskMetadata.getValue(GtasksMetadata.LIST_ID), idList, null);
                            com.google.api.services.tasks.v1.model.Task result = moveList.executePush();
                            local.gtaskMetadata.setValue(GtasksMetadata.ID, result.id);
                            remote.gtaskMetadata.setValue(GtasksMetadata.ID, result.id);
                        }
                    } catch (IOException e) {
                        handleException("update-task", e, false);
                    } finally {
                        pushedTaskSemaphore.release();
                    }
                }
            }).start();

        } catch (Exception e) {
            throw new GoogleTasksException(e);
        }
    }//*/

    /** Create a task container for the given remote task
     * @throws JSONException */
    private GtasksTaskContainer parseRemoteTask(com.google.api.services.tasks.v1.model.Task remoteTask) {
        Task task = new Task();
        TaskDao.setDefaultReminders(task);

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        task.setValue(Task.TITLE, remoteTask.title);
        task.setValue(Task.CREATION_DATE, DateUtilities.now());
        task.setValue(Task.COMPLETION_DATE, GtasksApiUtilities.gtasksCompletedTimeToUnixTime(remoteTask.completed, 0));
        if (remoteTask.deleted == null || !remoteTask.deleted.booleanValue())
            task.setValue(Task.DELETION_DATE, 0L);
        else if (remoteTask.deleted)
            task.setValue(Task.DELETION_DATE, DateUtilities.now());

        long dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.due, 0);
        long createdDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate);
        task.setValue(Task.DUE_DATE, createdDate);
        task.setValue(Task.NOTES, remoteTask.notes);

        Metadata gtasksMetadata = GtasksMetadata.createEmptyMetadata(AbstractModel.NO_ID);
        gtasksMetadata.setValue(GtasksMetadata.ID, remoteTask.id);
        gtasksMetadata.setValue(GtasksMetadata.LIST_ID, GtasksApiUtilities.extractListIdFromSelfLink(remoteTask));

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
        gtasksTaskListUpdater.updateParentAndSibling(local);

        String id = local.gtaskMetadata.getValue(GtasksMetadata.ID);
        if(Constants.DEBUG)
            Log.e("gtasks-debug", "ACTION: modifyTask(3) - " + id);

        com.google.api.services.tasks.v1.model.Task toUpdate = taskService.getGtask(local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID), id);
        UpdateRequest modifyTask = new UpdateRequest(taskService, local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID), toUpdate);
        updateTaskHelper(local, remote, modifyTask);

        return pull(remote);
    }//*/

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
            Task local = PluginServices.getTaskService().fetchById(task.task.getId(), Task.DUE_DATE, Task.COMPLETION_DATE);
            mergeDates(task.task, local);
            if(task.task.isCompleted() && !local.isCompleted())
                StatisticsService.reportEvent("gtasks-task-completed"); //$NON-NLS-1$
        } else {
            StatisticsService.reportEvent("gtasks-task-created"); //$NON-NLS-1$
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
            long setDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    newDate.getTime());
            remote.setValue(Task.DUE_DATE, setDate);
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
