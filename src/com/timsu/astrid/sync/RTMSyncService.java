package com.timsu.astrid.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
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
    private static final String ASTRID_LIST_NAME = "Astrid";

    public RTMSyncService(int id) {
        super(id);
    }

    @Override
    String getName() {
        return "RTM";
    }

    @Override
    public void synchronize(Activity activity) {
        authenticate(activity);
    }

    @Override
    public void clearPersonalData(Activity activity) {
        Preferences.setSyncRTMToken(activity, null);
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

    private void performSync(Activity activity) {
        try {
            // get RTM timeline
            final String timeline = rtmService.timelines_create();

            // get / create astrid list
            RtmList astridList = null;
            RtmLists lists = rtmService.lists_getList();
            for(RtmList list : lists.getLists().values()) {
                if(ASTRID_LIST_NAME.equals(list.getName())) {
                    astridList = list;
                    break;
                }
            }
            if(astridList == null)
                astridList = rtmService.lists_add(timeline, ASTRID_LIST_NAME);
            final RtmList newTaskCreationList = astridList;

            RtmTasks tasks = rtmService.tasks_getList(null, null,
                    Preferences.getSyncRTMLastSync(activity));

            List<TaskProxy> remoteChanges = new LinkedList<TaskProxy>();
            for(RtmTaskList taskList : tasks.getLists()) {
                for(RtmTaskSeries taskSeries : taskList.getSeries()) {
                    TaskProxy remoteTask = parseRemoteTask(taskList.getId(), taskSeries);
                    remoteChanges.add(remoteTask);
                }
            }

            synchronizeTasks(activity, remoteChanges, new SynchronizeHelper() {
                @Override
                public String createTask() throws IOException {
                    RtmTaskSeries s = rtmService.tasks_add(timeline,
                            newTaskCreationList.getId(), "tmp");
                    return new RtmId(newTaskCreationList.getId(), s).toString();
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
                    RtmTaskSeries rtmTask = rtmService.tasks_getTask(task.getRemoteId(),
                            task.name);
                    return parseRemoteTask(id.listId, rtmTask);
                }
            });

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
    public void pushLocalTask(String timeline, TaskProxy task, SyncMapping mapping)
            throws ServiceException {
        RtmId id = new RtmId(mapping.getRemoteId());

        if(task.name != null)
            rtmService.tasks_setName(timeline, id.listId, id.taskSeriesId,
                    id.taskId, task.name);
        if(task.importance != null)
            rtmService.tasks_setPriority(timeline, id.listId, id.taskSeriesId,
                    id.taskId, Priority.values()[task.importance.ordinal()]);
        rtmService.tasks_setDueDate(timeline, id.listId, id.taskSeriesId,
                id.taskId, task.definiteDueDate, task.definiteDueDate != null);
        if(task.progressPercentage != null) {
            if(task.progressPercentage == 100)
                rtmService.tasks_complete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
            else
                rtmService.tasks_uncomplete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
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
        task.notes = sb.toString();

        RtmTask rtmTask = rtmTaskSeries.getTask();
        task.creationDate = rtmTaskSeries.getCreated();
        task.completionDate = rtmTask.getCompleted();
        if(rtmTask.getHasDueTime() > 0)
            task.definiteDueDate = rtmTask.getDue();
        task.progressPercentage = (rtmTask.getCompleted() == null) ? null : 100;

        task.importance = Importance.values()[rtmTask.getPriority().ordinal()];

        return task;
    }

}
