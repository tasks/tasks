/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
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
import com.todoroo.astrid.gtasks.GtasksPreferences.OnGetCredentials;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.sync.SyncBackgroundService;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.gtasks.GoogleConnectionManager;
import com.todoroo.gtasks.GoogleLoginException;
import com.todoroo.gtasks.GoogleTaskService;
import com.todoroo.gtasks.GoogleTaskService.ConvenientTaskCreator;
import com.todoroo.gtasks.GoogleTaskTask;
import com.todoroo.gtasks.GoogleTaskView;
import com.todoroo.gtasks.GoogleTasksException;
import com.todoroo.gtasks.actions.Actions;
import com.todoroo.gtasks.actions.ListAction;
import com.todoroo.gtasks.actions.ListActions;
import com.todoroo.gtasks.actions.ListActions.TaskBuilder;
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

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired protected ExceptionService exceptionService;

    public GtasksSyncProvider() {
        super();
        DependencyInjectionService.getInstance().inject(this);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ utility methods
    // ----------------------------------------------------------------------

    /**
     * Sign out of service, deleting all synchronization metadata
     */
    public void signOut() {
        gtasksPreferenceService.clearLastSyncDate();
        gtasksPreferenceService.setToken(null);

        gtasksMetadataService.clearMetadata();
    }

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
    protected void handleException(String tag, Exception e, boolean displayError) {
        final Context context = ContextManager.getContext();
        gtasksPreferenceService.setLastError(e.toString());

        String message = null;

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

            // occurs when network error
        } else if(!(e instanceof GoogleTasksException) && e instanceof IOException) {
            message = context.getString(R.string.SyP_ioerror);
            exceptionService.reportError(tag + "-ioexception", e); //$NON-NLS-1$
        } else {
            message = context.getString(R.string.DLG_error, e.toString());
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
        }

        if(displayError && context instanceof Activity && message != null) {
            DialogUtilities.okDialog((Activity)context,
                    message, null);
        }
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * initiate sync in background
     */
    @Override
    protected void initiateBackground(Service service) {
        try {
            String authToken = gtasksPreferenceService.getToken();

            final GoogleConnectionManager connectionManager;
            if(authToken == null) {
                Log.e("astrid-sync", "No token, unable to sync");
                return;
            } else {
                connectionManager = new GoogleConnectionManager(authToken);
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
            final GtasksPreferences preferenceActivity = (GtasksPreferences)activity;
            preferenceActivity.getCredentials(new OnGetCredentials() {
                @Override
                public void getCredentials(String[] accounts) {
                    LinearLayout layout = new LinearLayout(activity);
                    layout.setPadding(5, -5, 5, 0);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    TextView textView = new TextView(activity);
                    textView.setText(R.string.producteev_PLA_email);
                    layout.addView(textView);
                    final EditText email = new EditText(activity);
                    if(accounts != null && accounts.length > 0)
                        email.setText(accounts[0]);
                    layout.addView(email);
                    textView = new TextView(activity);
                    textView.setText(R.string.producteev_PLA_password);
                    layout.addView(textView);
                    final EditText password = new EditText(activity);
                    password.setTransformationMethod(new PasswordTransformationMethod());
                    layout.addView(password);

                    DialogUtilities.viewDialog(activity,
                            activity.getString(R.string.gtasks_login), layout,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    trySynchronizing(activity, email.getText(), password.getText());
                                }

                            }, null);
                }
            });
        } else {
            activity.startService(new Intent(SyncBackgroundService.SYNC_ACTION, null,
                activity, GtasksBackgroundService.class));
            activity.finish();
        }
    }

    private void trySynchronizing(Activity activity, CharSequence email, CharSequence password) {
        GoogleConnectionManager gcm = new GoogleConnectionManager(email.toString(), password.toString());
        try {
            gcm.authenticate(false);
        } catch (GoogleLoginException e) {
            Toast.makeText(activity, R.string.gtasks_login_error, Toast.LENGTH_LONG).show();
            return;
        } catch (IOException e) {
            Toast.makeText(activity, R.string.SyP_ioerror, Toast.LENGTH_LONG).show();
        }
        String token = gcm.getToken();
        System.err.println("got token " + token);
        gtasksPreferenceService.setToken(token);
        activity.startService(new Intent(SyncBackgroundService.SYNC_ACTION, null,
            activity, GtasksBackgroundService.class));
        activity.finish();
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        FlurryAgent.onEvent("gtasks-started");
        gtasksPreferenceService.recordSyncStart();

        try {
            GoogleTaskView taskView = taskService.getTaskView();
            getActiveList(taskView);

            gtasksListService.updateLists(taskView.getAllLists());

            gtasksTaskListUpdater.createParentSiblingMaps();

            // batched read tasks for each list
            ArrayList<GtasksTaskContainer> remoteTasks = readAllRemoteTasks();

            SyncData<GtasksTaskContainer> syncData = populateSyncData(remoteTasks);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            gtasksTaskListUpdater.updateAllMetadata();

            gtasksPreferenceService.recordSuccessfulSync();
            FlurryAgent.onEvent("gtasks-sync-finished"); //$NON-NLS-1$
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
        // first, pull all tasks. then we can write them
        try {
            data.remoteUpdated = readAllRemoteTasks();
        } catch (JSONException e) {
            throw new GoogleTasksException(e);
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
    private SyncData<GtasksTaskContainer> populateSyncData(ArrayList<GtasksTaskContainer> remoteTasks) throws JSONException {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = gtasksMetadataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = gtasksMetadataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<GtasksTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    private ArrayList<GtasksTaskContainer> readAllRemoteTasks()
            throws JSONException, IOException, GoogleLoginException {
        ArrayList<GtasksTaskContainer> remoteTasks = new ArrayList<GtasksTaskContainer>();
        for(StoreObject dashboard : gtasksListService.getLists()) {
            String listId = dashboard.getValue(GtasksList.REMOTE_ID);
            List<GoogleTaskTask> list = taskService.getTasks(listId);
            addRemoteTasksToList(list, remoteTasks);
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

        ConvenientTaskCreator createdTask;
        try {
            createdTask = taskService.createTask(list, local.task.getValue(Task.TITLE));
            createdTask.parentId(local.parentId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        String remoteId = updateTaskHelper(local, null, createdTask);
        gtasksTaskListUpdater.addRemoteTaskMapping(local.task.getId(), remoteId);
        local.gtaskMetadata.setValue(GtasksMetadata.LIST_ID, remoteId);

        return local;
    }

    private String updateTaskHelper(GtasksTaskContainer local,
            GtasksTaskContainer remote, TaskBuilder<?> builder) throws IOException {

        String idTask = local.gtaskMetadata.getValue(GtasksMetadata.ID);
        String idList = local.gtaskMetadata.getValue(GtasksMetadata.LIST_ID);

        try {

            // moving between lists
            if(remote != null && !idList.equals(remote.gtaskMetadata.getValue(
                    GtasksMetadata.LIST_ID))) {
                taskService.executeActions(a.moveTask(idTask, idList,
                        remote.gtaskMetadata.getValue(GtasksMetadata.LIST_ID), null));
            }

            // other properties
            if(shouldTransmit(local, Task.DUE_DATE, remote))
                builder.taskDate(local.task.getValue(Task.DUE_DATE));
            if(shouldTransmit(local, Task.COMPLETION_DATE, remote))
                builder.completed(local.task.isCompleted());
            if(shouldTransmit(local, Task.DELETION_DATE, remote))
                builder.deleted(local.task.isDeleted());
            if(shouldTransmit(local, Task.NOTES, remote))
                builder.notes(local.task.getValue(Task.NOTES));

            String id = idList;

            // write task (and perform move action if requested)
            if(builder instanceof TaskModifier) {
                ListAction moveAction = l.move(idTask, local.parentId, local.priorSiblingId);
                ListAction action = ((TaskModifier) builder).done();
                if(remote == null || local.parentId != remote.parentId || local.priorSiblingId != remote.priorSiblingId)
                    taskService.executeListActions(idList, action, moveAction);
                else
                    taskService.executeListActions(idList, action);
            } else {
                id = ((ConvenientTaskCreator)builder).go();
                ListAction moveAction = l.move(id, local.parentId, local.priorSiblingId);
                taskService.executeListActions(idList, moveAction);
            }

            return id;
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
    protected void push(GtasksTaskContainer local, GtasksTaskContainer remote) throws IOException {
        try {
            gtasksTaskListUpdater.updateParentAndSibling(local);

            String id = local.gtaskMetadata.getValue(GtasksMetadata.ID);
            TaskModifier modifyTask = l.modifyTask(id);
            if(shouldTransmit(local, Task.TITLE, remote))
                modifyTask.name(local.task.getValue(Task.TITLE));
            updateTaskHelper(local, remote, modifyTask);

        } catch (JSONException e) {
            throw new GoogleTasksException(e);
        }
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
        gtasksMetadataService.saveTaskAndMetadata(task);
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
