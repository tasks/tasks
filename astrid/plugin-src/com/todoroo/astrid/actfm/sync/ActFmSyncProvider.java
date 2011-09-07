/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm.sync;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.timsu.astrid.C2DMReceiver;
import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.ActFmBackgroundService;
import com.todoroo.astrid.actfm.ActFmLoginActivity;
import com.todoroo.astrid.actfm.ActFmPreferences;
import com.todoroo.astrid.actfm.sync.ActFmSyncService.JsonHelper;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.notes.NoteMetadata;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.sync.SyncProvider;
import com.todoroo.astrid.sync.SyncProviderUtilities;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class ActFmSyncProvider extends SyncProvider<ActFmTaskContainer> {

    private ActFmInvoker invoker = null;

    @Autowired ActFmDataService actFmDataService;
    @Autowired ActFmSyncService actFmSyncService;
    @Autowired ActFmPreferenceService actFmPreferenceService;

    static {
        AstridDependencyInjector.initialize();
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------ utility methods
    // ----------------------------------------------------------------------

    @Override
    protected SyncProviderUtilities getUtilities() {
        return actFmPreferenceService;
    }

    /**
     * Sign out of service, deleting all synchronization metadata
     */
    public void signOut() {
        actFmPreferenceService.setToken(null);
        actFmPreferenceService.clearLastSyncDate();
        C2DMReceiver.unregister();
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
            C2DMReceiver.register();
            String authToken = actFmPreferenceService.getToken();
            invoker = new ActFmInvoker(authToken);

            // check if we have a token & it works
            if(authToken != null) {
                performSync();
            }
        } catch (IllegalStateException e) {
            // occurs when application was closed
        } catch (Exception e) {
            handleException("actfm-authenticate", e, true);
        } finally {
            actFmPreferenceService.stopOngoing();
        }
    }

    /**
     * If user isn't already signed in, show sign in dialog. Else perform sync.
     */
    @Override
    protected void initiateManual(Activity activity) {
        String authToken = actFmPreferenceService.getToken();
        actFmPreferenceService.stopOngoing();

        // check if we have a token & it works
        if(authToken == null) {
            // display login-activity
            Intent intent = new Intent(activity, ActFmLoginActivity.class);
            activity.startActivityForResult(intent, 0);
        } else {
            activity.startService(new Intent(null, null,
                    activity, ActFmBackgroundService.class));
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------- synchronization!
    // ----------------------------------------------------------------------

    protected void performSync() {
        actFmPreferenceService.recordSyncStart();
        boolean syncSuccess = false;

        try {
            int serverTime = Preferences.getInt(ActFmPreferenceService.PREF_SERVER_TIME, 0);
            ArrayList<ActFmTaskContainer> remoteTasks = new ArrayList<ActFmTaskContainer>();

            int newServerTime = fetchRemoteTasks(serverTime, remoteTasks);
            fetchRemoteTagData(serverTime);

            SyncData<ActFmTaskContainer> syncData = populateSyncData(remoteTasks);

            try {
                synchronizeTasks(syncData);
            } finally {
                syncData.localCreated.close();
                syncData.localUpdated.close();
            }

            Preferences.setInt(ActFmPreferenceService.PREF_SERVER_TIME, newServerTime);
            actFmPreferenceService.recordSuccessfulSync();

            syncSuccess = true;
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH);
            ContextManager.getContext().sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);

        } catch (IllegalStateException e) {
        	// occurs when application was closed
        } catch (Exception e) {
            handleException("actfm-sync", e, true); //$NON-NLS-1$
        } finally {
            StatisticsService.reportEvent(StatisticsConstants.ACTFM_SYNC_FINISHED,
                    "success", Boolean.toString(syncSuccess)); //$NON-NLS-1$
        }
    }

    /**
     * Read remote tag data and merge with local
     * @param serverTime last sync time
     */
    private void fetchRemoteTagData(int serverTime) throws ActFmServiceException, IOException, JSONException {
        actFmSyncService.fetchTags(serverTime);
    }

    /**
     * Read remote task data into remote task array
     * @param serverTime last sync time
     */
    private int fetchRemoteTasks(int serverTime,
            ArrayList<ActFmTaskContainer> remoteTasks) throws IOException,
            ActFmServiceException, JSONException {
        JSONObject result;
        if(serverTime == 0)
            result = invoker.invoke("task_list", "active", 1);
        else
            result = invoker.invoke("task_list", "modified_after", serverTime);

        JSONArray taskList = result.getJSONArray("list");
        for(int i = 0; i < taskList.length(); i++) {
            ActFmTaskContainer remote = parseRemoteTask(taskList.getJSONObject(i));

            // update reminder flags for incoming remote tasks to prevent annoying
            if(remote.task.hasDueDate() && remote.task.getValue(Task.DUE_DATE) < DateUtilities.now())
                remote.task.setFlag(Task.REMINDER_FLAGS, Task.NOTIFY_AFTER_DEADLINE, false);

            actFmDataService.findLocalMatch(remote);

            remoteTasks.add(remote);
        }
        return result.optInt("time", 0);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------------------ sync data
    // ----------------------------------------------------------------------

    /**
     * Populate SyncData data structure
     * @throws JSONException
     */
    private SyncData<ActFmTaskContainer> populateSyncData(ArrayList<ActFmTaskContainer> remoteTasks) throws JSONException {
        // fetch locally created tasks
        TodorooCursor<Task> localCreated = actFmDataService.getLocallyCreated(Task.PROPERTIES);

        // fetch locally updated tasks
        TodorooCursor<Task> localUpdated = actFmDataService.getLocallyUpdated(Task.PROPERTIES);

        return new SyncData<ActFmTaskContainer>(remoteTasks, localCreated, localUpdated);
    }

    // ----------------------------------------------------------------------
    // ------------------------------------------------- create / push / pull
    // ----------------------------------------------------------------------

    @Override
    protected ActFmTaskContainer create(ActFmTaskContainer local) throws IOException {
        return push(local, null);
    }

    /** Create a task container for the given remote task
     * @throws JSONException */
    private ActFmTaskContainer parseRemoteTask(JSONObject remoteTask) throws JSONException {
        Task task = new Task();

        ArrayList<Metadata> metadata = new ArrayList<Metadata>();

        JsonHelper.taskFromJson(remoteTask, task, metadata);
        ActFmTaskContainer container = new ActFmTaskContainer(task, metadata, remoteTask);

        return container;
    }

    @Override
    protected ActFmTaskContainer pull(ActFmTaskContainer task) throws IOException {
        if(task.task.getValue(Task.REMOTE_ID) == 0)
            throw new ActFmServiceException("Tried to read an invalid task"); //$NON-NLS-1$

        JSONObject remote = invoker.invoke("task_show", "id", task.task.getValue(Task.REMOTE_ID));
        try {
            return parseRemoteTask(remote);
        } catch (JSONException e) {
            throw new ActFmServiceException(e);
        }
    }

    /**
     * Send changes for the given Task across the wire.
     */
    @Override
    protected ActFmTaskContainer push(ActFmTaskContainer local, ActFmTaskContainer remote) throws IOException {
        long id = local.task.getValue(Task.REMOTE_ID);

        actFmSyncService.pushTaskOnSave(local.task, local.task.getDatabaseValues());

        // push unsaved comments
        for(Metadata item : local.metadata) {
            if(NoteMetadata.METADATA_KEY.equals(item.getValue(Metadata.KEY)))
                if(TextUtils.isEmpty(item.getValue(NoteMetadata.EXT_ID))) {
                    JSONObject comment = invoker.invoke("comment_add",
                            "task_id", id,
                            "message", item.getValue(NoteMetadata.BODY));
                    item.setValue(NoteMetadata.EXT_ID, comment.optString("id"));
                }
        }

        return local;
    }

    @Override
    protected void readRemotelyUpdated(SyncData<ActFmTaskContainer> data) throws IOException {
        int serverTime = Preferences.getInt(ActFmPreferenceService.PREF_SERVER_TIME, 0);
        ArrayList<ActFmTaskContainer> remoteTasks = new ArrayList<ActFmTaskContainer>();

        try {
            fetchRemoteTasks(serverTime, remoteTasks);
            data.remoteUpdated = remoteTasks;
        } catch (JSONException e) {
            // Ingnored
        }
        super.readRemotelyUpdated(data);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- read / write
    // ----------------------------------------------------------------------

    @Override
    protected ActFmTaskContainer read(TodorooCursor<Task> cursor) throws IOException {
        return actFmDataService.readTaskAndMetadata(cursor);
    }

    @Override
    protected void write(ActFmTaskContainer task) throws IOException {
        if(task.task.isSaved()) {
            Task local = PluginServices.getTaskService().fetchById(task.task.getId(), Task.COMPLETION_DATE);
            if(task.task.isCompleted() && !local.isCompleted())
                StatisticsService.reportEvent(StatisticsConstants.ACTFM_TASK_COMPLETED);
        } else { // Set default reminders for remotely created tasks
            TaskDao.setDefaultReminders(task.task);
        }
        actFmDataService.saveTaskAndMetadata(task);
    }

    // ----------------------------------------------------------------------
    // --------------------------------------------------------- misc helpers
    // ----------------------------------------------------------------------

    @Override
    protected int matchTask(ArrayList<ActFmTaskContainer> tasks, ActFmTaskContainer target) {
        int length = tasks.size();
        for(int i = 0; i < length; i++) {
            ActFmTaskContainer task = tasks.get(i);
            if (task.task.getValue(Task.REMOTE_ID) == target.task.getValue(Task.REMOTE_ID))
                return i;
        }
        return -1;
    }

    @Override
    protected int updateNotification(Context context, Notification notification) {
        String notificationTitle = context.getString(R.string.actfm_notification_title);
        Intent intent = new Intent(context, ActFmPreferences.class);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0,
                intent, 0);
        notification.setLatestEventInfo(context,
                notificationTitle, context.getString(R.string.SyP_progress),
                notificationIntent);
        return Constants.NOTIFICATION_SYNC;
    }

    @Override
    protected void transferIdentifiers(ActFmTaskContainer source,
            ActFmTaskContainer destination) {
        destination.task.setValue(Task.REMOTE_ID, source.task.getValue(Task.REMOTE_ID));
    }

}
