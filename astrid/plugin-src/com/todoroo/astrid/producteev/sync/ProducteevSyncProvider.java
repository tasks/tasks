/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.service.NotificationManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.producteev.ProducteevBackgroundService;
import com.todoroo.astrid.producteev.ProducteevFilterExposer;
import com.todoroo.astrid.producteev.ProducteevLoginActivity;
import com.todoroo.astrid.producteev.ProducteevPreferences;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.api.ApiResponseParseException;
import com.todoroo.astrid.producteev.api.ApiServiceException;
import com.todoroo.astrid.producteev.api.ApiUtilities;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncContainer;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class ProducteevSyncProvider extends SyncProvider<ProducteevTaskContainer> {

    private static final long TASK_ID_UNSYNCED = 1L;
    private ProducteevDataService dataService = null;
    private ProducteevInvoker invoker = null;
    private final ProducteevUtilities preferences = ProducteevUtilities.INSTANCE;

    /** producteev user id. set during sync */
    private long userId;

    /** map of producteev dashboard id + label name  to id's */
    private final HashMap<String, Long> labelMap = new HashMap<String, Long>();

    static {
        AstridDependencyInjector.initialize();
    }

    @Autowired
    protected ExceptionService exceptionService;

    public ProducteevSyncProvider() {
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
        preferences.setToken(null);
        Preferences.setString(R.string.producteev_PPr_email, null);
        Preferences.setString(R.string.producteev_PPr_password, null);
        Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_SYNC, null);
        Preferences.setStringFromInteger(R.string.producteev_PPr_defaultdash_key,
                ProducteevUtilities.DASHBOARD_DEFAULT);
        preferences.clearLastSyncDate();

        dataService = ProducteevDataService.getInstance();
        dataService.clearMetadata();
    }

    @Override
    protected SyncProviderUtilities getUtilities() {
        return ProducteevUtilities.INSTANCE;
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ initiating sync
    // ----------------------------------------------------------------------

    /**
     * initiate sync in background
     */
    @Override
    protected void initiateBackground() {
        dataService = ProducteevDataService.getInstance();

        try {
            String authToken = preferences.getToken();
            invoker = getInvoker();

            String email = Preferences.getStringValue(R.string.producteev_PPr_email);
            String password = Preferences.getStringValue(R.string.producteev_PPr_password);

            // check if we have a token & it works
            if(authToken != null) {
                invoker.setCredentials(authToken, email, password);
                performSync();
            } else {
                if (email == null && password == null) {
                    // we can't do anything, user is not logged in
                } else {
                    invoker.authenticate(email, password);
                    preferences.setToken(invoker.getToken());
                    performSync();
                }
            }
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("pdv-authenticate", e, true);
        } finally {
            preferences.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @Override
    protected void initiateManual(Activity activity) {
        String authToken = preferences.getToken();
        ProducteevUtilities.INSTANCE.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            // display login-activity
            Intent intent = new Intent(activity, ProducteevLoginActivity.class);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(null, null,
                    activity, ProducteevBackgroundService.class));
        }
    }

    public static ProducteevInvoker getInvoker() {
        String z = stripslashes(0, "71o3346pr40o5o4nt4n7t6n287t4op28","2");
        String v = stripslashes(2, "9641n76n9s1736q1578q1o1337q19233","4ae");
        return new ProducteevInvoker(z, v);
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        preferences.recordSyncStart();
        boolean syncSuccess = false;

        try {
            // load user information
            JSONObject user = invoker.usersView(null).getJSONObject("user");
            saveUserData(user);

            String lastServerSync = Preferences.getStringValue(ProducteevUtilities.PREF_SERVER_LAST_SYNC);

            // read dashboards
            JSONArray dashboards = invoker.dashboardsShowList(lastServerSync);
            dataService.updateDashboards(dashboards);

            // read labels and tasks for each dashboard
            ArrayList<ProducteevTaskContainer> remoteTasks = new ArrayList<ProducteevTaskContainer>();
            for(StoreObject dashboard : dataService.getDashboards()) {
                long dashboardId = dashboard.getValue(ProducteevDashboard.REMOTE_ID);
                JSONArray labels = invoker.labelsShowList(dashboardId, null);
                readLabels(labels);

                try {
                    // This invocation throws ApiServiceException for workspaces that need to be upgraded
                    JSONArray tasks = invoker.tasksShowList(dashboardId, lastServerSync);
                    for(int i = 0; i < tasks.length(); i++) {
                        ProducteevTaskContainer remote = parseRemoteTask(tasks.getJSONObject(i));

                        if(remote.pdvTask.getValue(ProducteevTask.CREATOR_ID) != userId &&
                                remote.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID) != userId)
                            remote.task.setFlag(Task.FLAGS, Task.FLAG_IS_READONLY, true);
                        else
                            remote.task.setFlag(Task.FLAGS, Task.FLAG_IS_READONLY, false);

                        // update reminder flags for incoming remote tasks to prevent annoying
                        if(remote.task.hasDueDate() && remote.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                            remote.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

                        dataService.findLocalMatch(remote);

                        remoteTasks.add(remote);
                    }
                } catch (ApiServiceException ase) {
                    // catch it here, so that other dashboards can still be synchronized
                    handleException("pdv-sync", ase, true); //$NON-NLS-1$
                }
            }

            SyncData<ProducteevTaskContainer> syncData = populateSyncData(remoteTasks);
            try {
                synchronizeTasks(syncData);
                AndroidUtilities.sleepDeep(3000L);
                checkForCreatedDuringSync();
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_SYNC, invoker.time());
            preferences.recordSuccessfulSync();

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

            // notification/activities stuff
            processNotifications();

            syncSuccess = true;
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("pdv-sync", e, true); //$NON-NLS-1$
        } finally {
            StatisticsService.reportEvent("pdv-sync-finished",
                    "success", Boolean.toString(syncSuccess)); //$NON-NLS-1$
        }
    }

    private void checkForCreatedDuringSync() {
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);
        try {
            SyncData<ProducteevTaskContainer> localCreatedData = new SyncData<ProducteevTaskContainer>(null, localCreated, null);
            sendLocallyCreated(localCreatedData, new HashMap<String, Integer>());
        } catch (IOException e) {
            handleException("gtasks-sync", e, true);
        } finally {
            System.err.println("Sent " + localCreated.getCount() + " new tasks");
            localCreated.close();
        }
    }

    /**
     * @param activities
     * @return
     * @throws JSONException
     */
    private String[] parseActivities(JSONArray activities) throws JSONException {
        int count = (activities == null ? 0 : activities.length());
        String[] activitiesList = new String[count];
        if(activities == null)
            return activitiesList;
        for(int i = 0; i < activities.length(); i++) {
            String message = activities.getJSONObject(i).getJSONObject("activity").getString("message");
            activitiesList[i] = ApiUtilities.decode(message);
        }
        return activitiesList;
    }


    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    private void saveUserData(JSONObject user) throws JSONException {
        long defaultDashboard = user.getLong("default_dashboard");
        userId = user.getLong("id_user");
        Preferences.setLong(ProducteevUtilities.PREF_DEFAULT_DASHBOARD, defaultDashboard);
        Preferences.setLong(ProducteevUtilities.PREF_USER_ID, userId);

        // save the default dashboard preference if unset
        int defaultDashSetting = Preferences.getIntegerFromString(R.string.producteev_PPr_defaultdash_key,
                ProducteevUtilities.DASHBOARD_DEFAULT);
        if(defaultDashSetting == ProducteevUtilities.DASHBOARD_DEFAULT)
            Preferences.setStringFromInteger(R.string.producteev_PPr_defaultdash_key, (int) defaultDashboard);
    }

    // all synchronized properties
    private static final Property<?>[] PROPERTIES = new Property<?>[] {
            Task.ID,
            Task.TITLE,
            Task.IMPORTANCE,
            Task.DUE_DATE,
            Task.CREATION_DATE,
            Task.COMPLETION_DATE,
            Task.DELETION_DATE,
            Task.REMINDER_FLAGS,
            Task.NOTES,
            Task.RECURRENCE
    };

    /**
     * Populate SyncData data structure
     * @throws JSONException
     */
    private SyncData<ProducteevTaskContainer> populateSyncData(ArrayList<ProducteevTaskContainer> remoteTasks) throws JSONException {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = dataService.getLocallyCreated(PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = dataService.getLocallyUpdated(PROPERTIES);

        return new SyncData<ProducteevTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected ProducteevTaskContainer create(ProducteevTaskContainer local) throws IOException {
        Task localTask = local.task;
        long dashboard = ProducteevUtilities.INSTANCE.getDefaultDashboard();
        if(local.pdvTask.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
            dashboard = local.pdvTask.getValue(ProducteevTask.DASHBOARD_ID);
        long responsibleId = local.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID);

        if(dashboard == ProducteevUtilities.DASHBOARD_NO_SYNC) {
            // set a bogus task id, then return without creating
            local.pdvTask.setValue(ProducteevTask.ID, TASK_ID_UNSYNCED);
            return local;
        }

        JSONObject response = invoker.tasksCreate(localTask.getValue(Task.TITLE),
                responsibleId, dashboard, createDeadline(localTask), createReminder(localTask),
                localTask.isCompleted() ? 2 : 1, createStars(localTask));
        ProducteevTaskContainer newRemoteTask;
        try {
            newRemoteTask = parseRemoteTask(response);
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
        }
        transferIdentifiers(newRemoteTask, local);
        push(local, newRemoteTask);
        return newRemoteTask;
    }

    /** Create a task container for the given ProducteevTask
     * @throws JSONException */
    private ProducteevTaskContainer parseRemoteTask(JSONObject remoteTask) throws JSONException {
        Task task = new Task();
        TaskDao.setDefaultReminders(task);
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        if(remoteTask.has("task"))
            remoteTask = remoteTask.getJSONObject("task");

        task.setValue(Task.TITLE, ApiUtilities.decode(remoteTask.getString("title")));
        task.setValue(Task.CREATION_DATE, ApiUtilities.producteevToUnixTime(remoteTask.getString("time_created"), 0));
        task.setValue(Task.COMPLETION_DATE, remoteTask.getInt("status") == 2 ? DateUtilities.now() : 0);
        task.setValue(Task.DELETION_DATE, remoteTask.getInt("deleted") == 1 ? DateUtilities.now() : 0);

        long dueDate = ApiUtilities.producteevToUnixTime(remoteTask.getString("deadline"), 0);
        if(remoteTask.optInt("all_day", 0) == 1)
            task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate));
        else
            task.setValue(Task.DUE_DATE, Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueDate));
        task.setValue(Task.IMPORTANCE, 5 - remoteTask.getInt("star"));

        JSONArray labels = remoteTask.getJSONArray("labels");
        for(int i = 0; i < labels.length(); i++) {
            JSONObject label = labels.getJSONObject(i).getJSONObject("label");
            if(label.getInt("deleted") != 0)
                continue;

            Metadata tagData = new Metadata();
            tagData.setValue(Metadata.KEY, TagService.KEY);
            tagData.setValue(TagService.TAG, ApiUtilities.decode(label.getString("title")));
            metadata.add(tagData);
        }

        ProducteevTaskContainer container = new ProducteevTaskContainer(task, metadata, remoteTask);

        JSONArray notes = remoteTask.getJSONArray("notes");
        for(int i = notes.length() - 1; i >= 0; i--) {
            JSONObject note = notes.getJSONObject(i).getJSONObject("note");
            PluginServices.getMetadataService().deleteWhere(Criterion.and(Metadata.KEY.eq(NoteMetadata.METADATA_KEY),
                    NoteMetadata.EXT_ID.eq(note.getString("id_note"))));
            if(note.getLong("deleted") != 0)
                continue;

            long creator = note.getLong("id_creator");
            metadata.add(ApiUtilities.createNoteMetadata(note, creatorName(container, creator)));
        }

        return container;
    }

    private String creatorName(ProducteevTaskContainer container, long creator) {
        StoreObject[] dashboards = dataService.getDashboards();
        for(int i = 0; i < dashboards.length; i++) {
            Long dashboard = container.pdvTask.getValue(ProducteevTask.DASHBOARD_ID);
            if(dashboard.equals(dashboards[i].getValue(ProducteevDashboard.REMOTE_ID))) {
                return ProducteevDashboard.getUserFromDashboard(dashboards[i], creator);
            }
        }
        return null;
    }

    @Override
    protected ProducteevTaskContainer pull(ProducteevTaskContainer task) throws IOException {
        if(!task.pdvTask.containsNonNullValue(ProducteevTask.ID))
            throw new ApiServiceException("Tried to read an invalid task"); //$NON-NLS-1$

        JSONObject remote = invoker.tasksView(task.pdvTask.getValue(ProducteevTask.ID));
        try {
            return parseRemoteTask(remote);
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
        }
    }

    /**
     * Send changes for the given Task across the wire. If a remoteTask is
     * supplied, we attempt to intelligently only transmit the values that
     * have changed.
     */
    @Override
    protected ProducteevTaskContainer push(ProducteevTaskContainer local, ProducteevTaskContainer remote) throws IOException {
        boolean remerge = false;

        long idTask = local.pdvTask.getValue(ProducteevTask.ID);
        long idDashboard = local.pdvTask.getValue(ProducteevTask.DASHBOARD_ID);
        long idResponsible = local.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID);

        // if local is marked do not sync, handle accordingly
        if(idDashboard == ProducteevUtilities.DASHBOARD_NO_SYNC) {
            return local;
        }

        // fetch remote task for comparison
        if(remote == null)
            remote = pull(local);

        // either delete or re-create if necessary
        if(shouldTransmit(local, Task.DELETION_DATE, remote)) {
            if(local.task.getValue(Task.DELETION_DATE) > 0)
                invoker.tasksDelete(idTask);
            else {
                // if we create, we transfer identifiers to old remote
                // in case it is used by caller for other purposes
                ProducteevTaskContainer newRemote = create(local);
                transferIdentifiers(newRemote, remote);
                remote = newRemote;
            }
        }

        // dashboard
        if(remote != null && idDashboard != remote.pdvTask.getValue(ProducteevTask.DASHBOARD_ID)) {
            invoker.tasksSetWorkspace(idTask, idDashboard);
            remote = pull(local);
        } else if(remote == null && idTask == TASK_ID_UNSYNCED) {
            // was un-synced, create remote
            remote = create(local);
        }

        // responsible
        if(remote != null && idResponsible !=
                remote.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID)) {
            invoker.tasksSetResponsible(idTask, idResponsible);
        }

        // core properties
        if(shouldTransmit(local, Task.TITLE, remote))
            invoker.tasksSetTitle(idTask, local.task.getValue(Task.TITLE));
        if(shouldTransmit(local, Task.IMPORTANCE, remote))
            invoker.tasksSetStar(idTask, createStars(local.task));
        if(shouldTransmit(local, Task.DUE_DATE, remote)) {
            if(local.task.hasDueDate())
                invoker.tasksSetDeadline(idTask, createDeadline(local.task), local.task.hasDueTime() ? 0 : 1);
            else
                invoker.tasksUnsetDeadline(idTask);
        }

        boolean isPDVRepeating = ((local.pdvTask.containsNonNullValue(ProducteevTask.REPEATING_SETTING) &&
                local.pdvTask.getValue(ProducteevTask.REPEATING_SETTING).length()>0) ||
                (remote != null && remote.pdvTask.containsNonNullValue(ProducteevTask.REPEATING_SETTING) &&
                        remote.pdvTask.getValue(ProducteevTask.REPEATING_SETTING).length()>0));

        boolean isAstridRepeating = local.task.containsNonNullValue(Task.RECURRENCE) &&
            local.task.getValue(Task.RECURRENCE).length() > 0;

        if (isAstridRepeating && isPDVRepeating) {
            // Astrid-repeat overrides PDV-repeat
            invoker.tasksUnsetRepeating(idTask);
        }

        if(shouldTransmit(local, Task.COMPLETION_DATE, remote)) {
            invoker.tasksSetStatus(idTask, local.task.isCompleted() ? 2 : 1);
            if (local.task.isCompleted() && !isAstridRepeating &&
                    isPDVRepeating) {
                local.task.setValue(Task.COMPLETION_DATE, 0L);
                remerge = true;
            }
        }


        try {
            // tags
            transmitTags(local, remote, idTask, idDashboard);

            // notes
            if(!TextUtils.isEmpty(local.task.getValue(Task.NOTES))) {
                String note = local.task.getValue(Task.NOTES);
                JSONObject result = invoker.tasksNoteCreate(idTask, note);
                local.metadata.add(ApiUtilities.createNoteMetadata(result.getJSONObject("note"), null));
                local.task.setValue(Task.NOTES, "");
            }

            remote = pull(local);
            remote.task.setId(local.task.getId());
            if(remerge) {
                // transform local into remote
                local.task = remote.task;
                local.pdvTask.setValue(ProducteevTask.ID, remote.pdvTask.getValue(ProducteevTask.ID));
                local.pdvTask.setValue(ProducteevTask.DASHBOARD_ID, remote.pdvTask.getValue(ProducteevTask.DASHBOARD_ID));
                local.pdvTask.setValue(ProducteevTask.CREATOR_ID, remote.pdvTask.getValue(ProducteevTask.CREATOR_ID));
                local.pdvTask.setValue(ProducteevTask.RESPONSIBLE_ID, remote.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID));
                if(remote.pdvTask.containsNonNullValue(ProducteevTask.REPEATING_SETTING))
                    local.pdvTask.setValue(ProducteevTask.REPEATING_SETTING, remote.pdvTask.getValue(ProducteevTask.REPEATING_SETTING));
            }

            return remote;
        } catch (JSONException e) {
            throw new ApiResponseParseException(e);
        }
    }

    /**
     * Transmit tags
     *
     * @param local
     * @param remote
     * @param idTask
     * @param idDashboard
     * @throws ApiServiceException
     * @throws JSONException
     * @throws IOException
     */
    private void transmitTags(ProducteevTaskContainer local,
            ProducteevTaskContainer remote, long idTask, long idDashboard) throws ApiServiceException, JSONException, IOException {
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
            long[] labels = new long[localTags.size()];
            int index = 0;
            for(String label : localTags) {
                String pdvLabel = idDashboard + label;
                final long id;
                if(!labelMap.containsKey(pdvLabel)) {
                    JSONObject result = invoker.labelsCreate(idDashboard, label).getJSONObject("label");
                    id = putLabelIntoCache(result);
                } else
                    id = labelMap.get(pdvLabel);
                labels[index++] = id;
            }
            invoker.tasksChangeLabel(idTask, labels);
        }
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
        if(task.task.isSaved()) {
            Task local = PluginServices.getTaskService().fetchById(task.task.getId(), Task.COMPLETION_DATE, Task.FLAGS);
            task.task.setFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION, local.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION));
            if(task.task.isCompleted() && !local.isCompleted())
                StatisticsService.reportEvent("pdv-task-completed"); //$NON-NLS-1$
        }
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
            if (target.pdvTask.containsNonNullValue(ProducteevTask.ID) &&
                    task.pdvTask.getValue(ProducteevTask.ID).equals(target.pdvTask.getValue(ProducteevTask.ID)))
                return i;
        }
        return -1;
    }

    /**
     * get stars in producteev format
     * @param local
     * @return
     */
    private Integer createStars(Task local) {
        return 5 - local.getValue(Task.IMPORTANCE);
    }

    /**
     * get reminder in producteev format
     * @param local
     * @return
     */
    private Integer createReminder(Task local) {
        if(local.getFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AT_DEADLINE))
            return 8;
        return null;
    }

    /**
     * get deadline in producteev format
     * @param task
     * @return
     */
    private String createDeadline(Task task) {
        if(!task.hasDueDate())
            return "";
        return ApiUtilities.unixTimeToProducteev(task.getValue(Task.DUE_DATE));
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
        String notificationTitle = context.getString(R.string.producteev_notification_title);
        Intent intent = new Intent(context, ProducteevPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return Constants.NOTIFICATION_SYNC;
    }

    @Override
    protected void transferIdentifiers(ProducteevTaskContainer source,
            ProducteevTaskContainer destination) {
        destination.pdvTask = source.pdvTask;
    }

    /**
     * Read labels into label map
     * @param dashboardId
     * @throws JSONException
     * @throws ApiServiceException
     * @throws IOException
     */
    private void readLabels(JSONArray labels) throws JSONException, ApiServiceException, IOException {
        for(int i = 0; i < labels.length(); i++) {
            JSONObject label = labels.getJSONObject(i).getJSONObject("label");
            putLabelIntoCache(label);
        }
    }

    /**
     * Puts a single label into the cache
     * @param dashboardId
     * @param label
     * @throws JSONException
     */
    private long putLabelIntoCache(JSONObject label)
            throws JSONException {
        String name = ApiUtilities.decode(label.getString("title"));
        long dashboard = label.getLong("id_dashboard");
        labelMap.put(dashboard + name, label.getLong("id_label"));
        return label.getLong("id_label");
    }

    /**
     * Show workspace notifications
     *
     * @throws ApiResponseParseException
     * @throws ApiServiceException
     * @throws IOException
     * @throws JSONException
     */
    private void processNotifications() throws ApiResponseParseException,
            ApiServiceException, IOException, JSONException {
        String lastNotificationId = Preferences.getStringValue(ProducteevUtilities.PREF_SERVER_LAST_NOTIFICATION);
        String lastActivityId = Preferences.getStringValue(ProducteevUtilities.PREF_SERVER_LAST_ACTIVITY);
        JSONArray notifications = invoker.activitiesShowNotifications(null, (lastNotificationId == null ? null : new Long(lastNotificationId)));
        String[] notificationsList = parseActivities(notifications);
        // update lastIds
        if (notifications.length() > 0) {
            lastNotificationId = ""+notifications.getJSONObject(0).getJSONObject("activity").getLong("id_activity");
        }

        // display notifications from producteev-log
        Context context = ContextManager.getContext();
        final NotificationManager nm = new NotificationManager.AndroidNotificationManager(context);
        for (int i = 0; i< notificationsList.length; i++) {
            long id_dashboard = notifications.getJSONObject(i).getJSONObject("activity").getLong("id_dashboard");
            String dashboardName = null;
            StoreObject[] dashboardsData = ProducteevDataService.getInstance().getDashboards();
            ProducteevDashboard dashboard = null;
            if (dashboardsData != null) {
                for (int j=0; i<dashboardsData.length;i++) {
                    long id = dashboardsData[j].getValue(ProducteevDashboard.REMOTE_ID);
                    if (id == id_dashboard) {
                        dashboardName = dashboardsData[j].getValue(ProducteevDashboard.NAME);
                        dashboard = new ProducteevDashboard(id, dashboardName, null);
                        break;
                    }
                }
            }
            // it seems dashboard is null if we get a notification about an unknown dashboard, just filter it.
            if (dashboard != null) {
                // initialize notification
                int icon = R.drawable.ic_producteev_notification;
                long when = System.currentTimeMillis();
                Notification notification = new Notification(icon, null, when);
                CharSequence contentTitle = context.getString(R.string.producteev_notification_title)+": "+dashboard.getName();

                Filter filter = ProducteevFilterExposer.filterFromList(context, dashboard, userId);
                Intent notificationIntent = ShortcutActivity.createIntent(filter);

                // filter the tags from the message
                String message = notificationsList[i].replaceAll("<[^>]+>", "");
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                notification.setLatestEventInfo(context, contentTitle, message, contentIntent);

                nm.notify(Constants.NOTIFICATION_PRODUCTEEV_NOTIFICATIONS-i, notification);
            }
        }

        // store lastIds in Preferences
        Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_NOTIFICATION, lastNotificationId);
        Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_ACTIVITY, lastActivityId);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper methods
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==116?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }
}
