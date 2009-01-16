package com.timsu.astrid.sync;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.mdt.rtm.ApplicationInfo;
import com.mdt.rtm.ServiceException;
import com.mdt.rtm.ServiceImpl;
import com.mdt.rtm.data.RtmList;
import com.mdt.rtm.data.RtmLists;
import com.mdt.rtm.data.RtmTask;
import com.mdt.rtm.data.RtmTaskList;
import com.mdt.rtm.data.RtmTaskNote;
import com.mdt.rtm.data.RtmTaskSeries;
import com.mdt.rtm.data.RtmTasks;
import com.mdt.rtm.data.RtmAuth.Perms;
import com.mdt.rtm.data.RtmTask.Priority;
import com.timsu.astrid.R;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;

public class RTMSyncService extends SynchronizationService {

    private ServiceImpl rtmService = null;
    private String INBOX_LIST_NAME = "Inbox";
    Map<String, String> listNameToIdMap = new HashMap<String, String>();
    Map<String, String> listIdToNameMap = new HashMap<String, String>();

    public RTMSyncService(int id) {
        super(id);
    }

    @Override
    String getName() {
        return "RTM";
    }

    @Override
    protected void synchronize(final Activity activity) {
        if(Preferences.shouldSyncRTM(activity) &&
                Preferences.getSyncRTMToken(activity) == null) {
            DialogUtilities.okCancelDialog(activity,
                    activity.getResources().getString(R.string.sync_rtm_notes),
                    new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    authenticate(activity);
                }
            }, null);
        } else
            authenticate(activity);
    }

    @Override
    public void clearPersonalData(Activity activity) {
        Preferences.setSyncRTMToken(activity, null);
        Preferences.setSyncRTMLastSync(activity, null);
        Synchronizer.getSyncController(activity).deleteAllMappings(getId());
    }

    /** Perform authentication with RTM. Will open the SyncBrowser if necessary */
    private void authenticate(final Activity activity) {
        try {
            String apiKey = "bd9883b3384a21ead17501da38bb1e68";
            String sharedSecret = "a19b2a020345219b";
            String appName = null;
            String authToken = Preferences.getSyncRTMToken(activity);

            // check if we have a token & it works
            if(authToken != null) {
                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName, authToken));
                if(!rtmService.isServiceAuthorized()) // re-do login
                    authToken = null;
            }

            if(authToken == null) {
                // try completing the authorization if it was partial
                if(rtmService != null) {
                    try {
                        String token = rtmService.completeAuthorization();
                        Log.w("astrid", "got RTM token: " + token);
                        Preferences.setSyncRTMToken(activity, token);
                        performSync(activity);

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName));
                final String url = rtmService.beginAuthorization(Perms.delete);
                progressDialog.dismiss();
                Resources r = activity.getResources();
                DialogUtilities.okCancelDialog(activity,
                        r.getString(R.string.sync_auth_request, "RTM"),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(url));
                        activity.startActivity(intent);
                    }
                }, null);

            } else {
                performSync(activity);
            }

        } catch (Exception e) {
            showError(activity, e);
        }
    }

    private void performSync(final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                performSyncInNewThread(activity);
            }
        }).start();
    }

    private void performSyncInNewThread(final Activity activity) {
        try {
            syncHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog.show();
                    progressDialog.setMessage("Reading Remote Information");
                    progressDialog.setProgress(0);
                }
            });

            // get RTM timeline
            final String timeline = rtmService.timelines_create();
            syncHandler.post(new ProgressUpdater(20, 100));

            // load RTM lists
            RtmLists lists = rtmService.lists_getList();
            for(RtmList list : lists.getLists().values()) {
                listNameToIdMap.put(list.getName().toLowerCase(), list.getId());
                listIdToNameMap.put(list.getId(), list.getName());

                // read the name of the inbox with the correct case
                if(INBOX_LIST_NAME.equalsIgnoreCase(list.getName()))
                    INBOX_LIST_NAME = list.getName();
            }
            syncHandler.post(new ProgressUpdater(40, 100));

            // read all tasks
            RtmTasks tasks = rtmService.tasks_getList(null, null,
                    Preferences.getSyncRTMLastSync(activity));
            syncHandler.post(new ProgressUpdater(100, 100));

            List<TaskProxy> remoteChanges = new LinkedList<TaskProxy>();
            for(RtmTaskList taskList : tasks.getLists()) {
                for(RtmTaskSeries taskSeries : taskList.getSeries()) {
                    TaskProxy remoteTask = parseRemoteTask(taskList.getId(), taskSeries);
                    remoteChanges.add(remoteTask);
                }
            }

            synchronizeTasks(activity, remoteChanges, new SynchronizeHelper() {
                @Override
                public String createTask(String listName) throws IOException {
                    if(listName == null)
                        listName = INBOX_LIST_NAME;
                    if(!listNameToIdMap.containsKey(listName.toLowerCase())) {
                        try {
                            String listId =
                                rtmService.lists_add(timeline, listName).getId();
                            listNameToIdMap.put(listName.toLowerCase(), listId);
                        } catch (Exception e) {
                            listName = INBOX_LIST_NAME;
                        }
                    }
                    String listId = listNameToIdMap.get(listName.toLowerCase());
                    RtmTaskSeries s = rtmService.tasks_add(timeline,
                            listId, "tmp");
                    return new RtmId(listId, s).toString();
                }
                @Override
                public void deleteTask(SyncMapping mapping) throws IOException {
                    RtmId id = new RtmId(mapping.getRemoteId());
                    rtmService.tasks_delete(timeline, id.listId, id.taskSeriesId,
                            id.taskId);
                }
                @Override
                public void pushTask(TaskProxy task, SyncMapping mapping) throws IOException {
                    pushLocalTask(timeline, task, mapping);
                }
                @Override
                public TaskProxy refetchTask(TaskProxy task) throws IOException {
                    RtmId id = new RtmId(task.getRemoteId());
                    RtmTaskSeries rtmTask = rtmService.tasks_getTask(id.taskSeriesId,
                            task.name);
                    if(rtmTask != null)
                        return task; // can't fetch
                    return parseRemoteTask(id.listId, rtmTask);
                }
            });

            // add a bit of fudge time so we don't load tasks we just edited
            Date syncTime = new Date(System.currentTimeMillis() + 1000);
            Preferences.setSyncRTMLastSync(activity, syncTime);

        } catch (Exception e) {
            showError(activity, e);
        }
    }

    /** Helper class for processing RTM id's into one field */
    private static class RtmId {
        String taskId;
        String taskSeriesId;
        String listId;

        public RtmId(String listId, RtmTaskSeries taskSeries) {
            this.taskId = taskSeries.getTask().getId();
            this.taskSeriesId = taskSeries.getId();
            this.listId = listId;
        }

        public RtmId(String id) {
            StringTokenizer strtok = new StringTokenizer(id, "|");
            taskId = strtok.nextToken();
            taskSeriesId = strtok.nextToken();
            listId = strtok.nextToken();
        }

        @Override
        public String toString() {
            return taskId + "|" + taskSeriesId + "|" + listId;
        }
    }

    /** Send changes for the given TaskProxy across the wire */
    private void pushLocalTask(String timeline, TaskProxy task, SyncMapping mapping)
            throws ServiceException {
        RtmId id = new RtmId(mapping.getRemoteId());

        if(task.name != null)
            rtmService.tasks_setName(timeline, id.listId, id.taskSeriesId,
                    id.taskId, task.name);
        if(task.importance != null)
            rtmService.tasks_setPriority(timeline, id.listId, id.taskSeriesId,
                    id.taskId, Priority.values()[task.importance.ordinal()]);
        Date dueDate = task.definiteDueDate;
        if(dueDate == null)
            dueDate = task.preferredDueDate;
        rtmService.tasks_setDueDate(timeline, id.listId, id.taskSeriesId,
                id.taskId, dueDate, dueDate != null);
        if(task.progressPercentage != null) {
            if(task.progressPercentage == 100)
                rtmService.tasks_complete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
            else
                rtmService.tasks_uncomplete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
        }
        if(task.notes != null && !task.notes.endsWith("\n")) {
            rtmService.tasks_notes_add(timeline, id.listId, id.taskSeriesId,
                    id.taskId, "From Astrid", task.notes);
        }
    }

    /** Create a task proxy for the given RtmTaskSeries */
    private TaskProxy parseRemoteTask(String listId, RtmTaskSeries rtmTaskSeries) {
        TaskProxy task = new TaskProxy(getId(),
                new RtmId(listId, rtmTaskSeries).toString(),
                rtmTaskSeries.getTask().getDeleted() != null);

        task.name = rtmTaskSeries.getName();
        StringBuilder sb = new StringBuilder();
        for(RtmTaskNote note: rtmTaskSeries.getNotes().getNotes()) {
            sb.append(note.getText() + "\n");
        }
        if(sb.length() > 0)
            task.notes = sb.toString();
        String listName = listIdToNameMap.get(listId);
        if(listName != null && !listName.equals(INBOX_LIST_NAME))
            task.tags = new String[] { listName };

        RtmTask rtmTask = rtmTaskSeries.getTask();
        task.creationDate = rtmTaskSeries.getCreated();
        task.completionDate = rtmTask.getCompleted();
        if(rtmTask.getDue() != null)
            task.definiteDueDate = rtmTask.getDue();
        task.progressPercentage = (rtmTask.getCompleted() == null) ? null : 100;

        task.importance = Importance.values()[rtmTask.getPriority().ordinal()];

        return task;
    }

}
