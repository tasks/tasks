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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.common.SyncProvider;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.StoreObject;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.producteev.ProducteevLoginActivity;
import com.todoroo.astrid.producteev.ProducteevPreferences;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.producteev.api.ApiResponseParseException;
import com.todoroo.astrid.producteev.api.ApiServiceException;
import com.todoroo.astrid.producteev.api.ApiUtilities;
import com.todoroo.astrid.producteev.api.ProducteevInvoker;
import com.todoroo.astrid.rmilk.data.MilkNote;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.utility.Preferences;

@SuppressWarnings("nls")
public class ProducteevSyncProvider extends SyncProvider<ProducteevTaskContainer> {

    private static final long TASK_ID_UNSYNCED = 1L;
    private ProducteevDataService dataService = null;
    private ProducteevInvoker invoker = null;
    private final ProducteevUtilities preferences = ProducteevUtilities.INSTANCE;

    /** map of producteev dashboard id + label name  to id's */
    private final HashMap<String, Long> labelMap = new HashMap<String, Long>();

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
        preferences.setLastError(e.toString());

        // occurs when application was closed
        if(e instanceof IllegalStateException) {
            exceptionService.reportError(tag + "-caught", e); //$NON-NLS-1$

        // occurs when network error
        } else if(!(e instanceof ApiServiceException) && e instanceof IOException) {
            exceptionService.reportError(tag + "-ioexception", e); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, e, context.getString(R.string.producteev_ioerror));
            }
        } else {
            exceptionService.reportError(tag + "-unhandled", e); //$NON-NLS-1$
            if(showError) {
                Context context = ContextManager.getContext();
                showError(context, e, null);
            }
        }
    }

    @Override
    protected void initiate(Context context) {
        dataService = ProducteevDataService.getInstance();

        // authenticate the user. this will automatically call the next step
        authenticate();
    }

    /**
     * Perform authentication with RTM. Will open the SyncBrowser if necessary
     */
    private void authenticate() {
        FlurryAgent.onEvent("producteev-started");

        preferences.recordSyncStart();

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
                    // display login-activity
                    final Context context = ContextManager.getContext();
                    Intent intent = new Intent(context, ProducteevLoginActivity.class);
                    if(context instanceof Activity)
                        ((Activity)context).startActivityForResult(intent, 0);
                    else {
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        ProducteevUtilities.INSTANCE.stopOngoing();
                    }
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

    public static ProducteevInvoker getInvoker() {
        String z = stripslashes(0, "71o3346pr40o5o4nt4n7t6n287t4op28","2");
        String v = stripslashes(2, "9641n76n9s1736q1578q1o1337q19233","4ae");
        return new ProducteevInvoker(z, v);
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        try {
            // load user information
            JSONObject user = invoker.usersView(null).getJSONObject("user");
            saveUserData(user);
            long userId = user.getLong("id_user");

            String lastServerSync = Preferences.getStringValue(ProducteevUtilities.PREF_SERVER_LAST_SYNC);

            // read dashboards
            JSONArray dashboards = invoker.dashboardsShowList(null);
            dataService.updateDashboards(dashboards);

            // read labels and tasks for each dashboard
            ArrayList<ProducteevTaskContainer> remoteTasks = new ArrayList<ProducteevTaskContainer>();
            for(StoreObject dashboard : dataService.getDashboards()) {
                long dashboardId = dashboard.getValue(ProducteevDashboard.REMOTE_ID);
                JSONArray labels = invoker.labelsShowList(dashboardId, null);
                readLabels(labels);

                JSONArray tasks = invoker.tasksShowList(dashboardId, lastServerSync);
                for(int i = 0; i < tasks.length(); i++) {
                    ProducteevTaskContainer remote = parseRemoteTask(tasks.getJSONObject(i));
                    boolean foundLocal = dataService.findLocalMatch(remote);

                    // if creator & responsible != current user, skip / delete it
                    if(userId != remote.pdvTask.getValue(ProducteevTask.CREATOR_ID) &&
                            userId != remote.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID)) {
                        if(foundLocal)
                            remote.task.setValue(Task.DELETION_DATE, DateUtilities.now());
                        else
                            continue;
                    }

                    remoteTasks.add(remote);
                }
            }

            SyncData<ProducteevTaskContainer> syncData = populateSyncData(remoteTasks);
            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            Preferences.setString(ProducteevUtilities.PREF_SERVER_LAST_SYNC, invoker.time());
            preferences.recordSuccessfulSync();

            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

            FlurryAgent.onEvent("pdv-sync-finished"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("pdv-sync", e, true); //$NON-NLS-1$
        }
    }


    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    private void saveUserData(JSONObject user) throws JSONException {
        long defaultDashboard = user.getLong("default_dashboard");
        long userId = user.getLong("id_user");
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

    /** Create a task container for the given RtmTaskSeries
     * @throws JSONException */
    private ProducteevTaskContainer parseRemoteTask(JSONObject remoteTask) throws JSONException {
        Task task = new Task();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        if(remoteTask.has("task"))
            remoteTask = remoteTask.getJSONObject("task");

        task.setValue(Task.TITLE, ApiUtilities.decode(remoteTask.getString("title")));
        task.setValue(Task.CREATION_DATE, ApiUtilities.producteevToUnixTime(remoteTask.getString("time_created"), 0));
        task.setValue(Task.COMPLETION_DATE, remoteTask.getInt("status") == 2 ? DateUtilities.now() : 0);
        task.setValue(Task.DELETION_DATE, remoteTask.getInt("deleted") == 1 ? DateUtilities.now() : 0);

        long dueDate = ApiUtilities.producteevToUnixTime(remoteTask.getString("deadline"), 0);
        task.setValue(Task.DUE_DATE, task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueDate));
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

        JSONArray notes = remoteTask.getJSONArray("notes");
        for(int i = notes.length() - 1; i >= 0; i--) {
            JSONObject note = notes.getJSONObject(i).getJSONObject("note");
            metadata.add(ProducteevNote.create(note));
        }

        ProducteevTaskContainer container = new ProducteevTaskContainer(task, metadata, remoteTask);

        return container;
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
    protected void push(ProducteevTaskContainer local, ProducteevTaskContainer remote) throws IOException {
        long idTask = local.pdvTask.getValue(ProducteevTask.ID);
        long idDashboard = local.pdvTask.getValue(ProducteevTask.DASHBOARD_ID);
        long idResponsible = local.pdvTask.getValue(ProducteevTask.RESPONSIBLE_ID);

        // if local is marked do not sync, handle accordingly
        if(idDashboard == ProducteevUtilities.DASHBOARD_NO_SYNC) {
            if(idTask != 1)
                invoker.tasksDelete(idTask);
            return;
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
        if(shouldTransmit(local, Task.DUE_DATE, remote))
            invoker.tasksSetDeadline(idTask, createDeadline(local.task));
        if(shouldTransmit(local, Task.COMPLETION_DATE, remote))
            invoker.tasksSetStatus(idTask, local.task.isCompleted() ? 2 : 1);


        try {
            // tags
            transmitTags(local, remote, idTask, idDashboard);

            // notes
            if(!TextUtils.isEmpty(local.task.getValue(Task.NOTES))) {
                String note = local.task.getValue(Task.NOTES);
                JSONObject result = invoker.tasksNoteCreate(idTask, note);
                local.metadata.add(ProducteevNote.create(result.getJSONObject("note")));
                local.task.setValue(Task.NOTES, "");
            }

            // milk note => producteev note
            if(local.findMetadata(MilkNote.METADATA_KEY) != null && (remote == null ||
                    (remote.findMetadata(ProducteevNote.METADATA_KEY) == null))) {
                for(Metadata item : local.metadata) {
                    if(MilkNote.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                        String message = MilkNote.toTaskDetail(item);
                        JSONObject result = invoker.tasksNoteCreate(idTask, message);
                        local.metadata.add(ProducteevNote.create(result.getJSONObject("note")));
                    }
                }
            }
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
            HashSet<String> toAdd = new HashSet<String>(localTags);
            toAdd.removeAll(remoteTags);
            HashSet<String> toRemove = remoteTags;
            toRemove.removeAll(localTags);

            if(toAdd.size() > 0) {
                for(String label : toAdd) {
                    String pdvLabel = idDashboard + label;
                    if(!labelMap.containsKey(pdvLabel)) {
                        JSONObject result = invoker.labelsCreate(idDashboard, label).getJSONObject("label");
                        long id = putLabelIntoCache(result);
                        invoker.tasksSetLabel(idTask, id);
                    } else
                        invoker.tasksSetLabel(idTask, labelMap.get(pdvLabel));
                }
            }

            if(toRemove.size() > 0) {
                for(String label : toRemove) {
                    String pdvLabel = idDashboard + label;
                    if(!labelMap.containsKey(pdvLabel))
                        continue;
                    invoker.tasksUnsetLabel(idTask, labelMap.get(pdvLabel));
                }
            }
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
            if(AndroidUtilities.equals(task.pdvTask, target.pdvTask))
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
            return null;
        if(!task.hasDueTime())
            return ApiUtilities.unixDateToProducteev(task.getValue(Task.DUE_DATE));
        String time = ApiUtilities.unixTimeToProducteev(task.getValue(Task.DUE_DATE));
        return time.substring(0, time.lastIndexOf(' '));
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

    @Override
    protected void updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.producteev_notification_title);
        Intent intent = new Intent(context, ProducteevPreferences.class);
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

    // ----------------------------------------------------------------------
    // ------------------------------------------------------- helper methods
    // ----------------------------------------------------------------------

    private static final String stripslashes(int ____,String __,String ___) {
        int _=__.charAt(____/92);_=_==116?_-1:_;_=((_>=97)&&(_<=123)?
        ((_-83)%27+97):_);return TextUtils.htmlEncode(____==31?___:
        stripslashes(____+1,__.substring(1),___+((char)_)));
    }
}
