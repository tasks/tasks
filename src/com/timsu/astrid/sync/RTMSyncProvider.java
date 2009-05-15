/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.sync;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.mdt.rtm.ApplicationInfo;
import com.mdt.rtm.ServiceException;
import com.mdt.rtm.ServiceImpl;
import com.mdt.rtm.ServiceInternalException;
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
import com.timsu.astrid.activities.TaskList;
import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.sync.SyncMapping;
import com.timsu.astrid.data.task.TaskModelForSync;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;

public class RTMSyncProvider extends SynchronizationProvider {

    private ServiceImpl rtmService = null;
    private String INBOX_LIST_NAME = "Inbox";
    Map<String, String> listNameToIdMap = new HashMap<String, String>();
    Map<String, String> listIdToNameMap = new HashMap<String, String>();

    public RTMSyncProvider(int id) {
        super(id);
    }

    // --- abstract methods

    @Override
    String getName() {
        return "RTM";
    }

    @Override
    protected void synchronize(final Context activity) {
    	// authenticate the user. this will automatically call the next step
        authenticate(activity);
    }

    @Override
    public void clearPersonalData(Context context) {
        Preferences.setSyncRTMToken(context, null);
        Preferences.setSyncRTMLastSync(context, null);
        synchronizer.getSyncController(context).deleteAllMappings(getId());
    }

    // --- authentication

    /** Perform authentication with RTM. Will open the SyncBrowser if necessary */
    private void authenticate(final Context context) {
        try {
            String apiKey = "bd9883b3384a21ead17501da38bb1e68";
            String sharedSecret = "a19b2a020345219b";
            String appName = null;
            String authToken = Preferences.getSyncRTMToken(context);

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
                        Preferences.setSyncRTMToken(context, token);
                        performSync(context);

                        return;
                    } catch (Exception e) {
                        // didn't work. do the process again.
                    }
                }

                // open up a dialog and have the user go to browser
                if(isBackgroundService())
                	return;

                rtmService = new ServiceImpl(new ApplicationInfo(
                        apiKey, sharedSecret, appName));
                final String url = rtmService.beginAuthorization(Perms.delete);
                progressDialog.dismiss();
                Resources r = context.getResources();
                DialogUtilities.okCancelDialog(context,
                        r.getString(R.string.sync_auth_request, "RTM"),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        TaskList.synchronizeNow = true;
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(url));
                        context.startActivity(intent);
                    }
                }, null);

            } else {
                performSync(context);
            }

        } catch (Exception e) {
            // IO Exception
            if(e instanceof ServiceInternalException &&
                    ((ServiceInternalException)e).getEnclosedException() instanceof
                    IOException) {
                showError(context, e, "Sync Connection Error! Check your " +
                		"Internet connection & try again...");
            } else
                showError(context, e, null);
        }
    }

    // --- synchronization!

    private void performSync(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                performSyncInNewThread(context);
            }
        }).start();
    }

    private void performSyncInNewThread(final Context context) {
        try {
            postUpdate(new ProgressLabelUpdater("Reading remote data"));
            postUpdate(new ProgressUpdater(0, 5));

            // get RTM timeline
            final String timeline = rtmService.timelines_create();
            postUpdate(new ProgressUpdater(1, 5));

            // push task if single task sync is requested
            if(getSingleTaskForSync() != null) {
                SyncMapping mapping = synchronizer.getSyncController(context).
                    getSyncMapping(getId(), getSingleTaskForSync());
                if(mapping == null) {
                    Log.w("astrid-rtm", "Couldn't find sync mapping for updated task");
                    return;
                }

                TaskProxy localTask = new TaskProxy(getId(), mapping.getRemoteId());
                TaskModelForSync task = synchronizer.getTaskController(context).
                    fetchTaskForSync(getSingleTaskForSync());
                localTask.readFromTaskModel(task);
                postUpdate(new ProgressLabelUpdater("Synchronizing repeating task"));
                pushLocalTask(timeline, localTask, null, mapping);
            }

            // load RTM lists
            RtmLists lists = rtmService.lists_getList();
            for(RtmList list : lists.getLists().values()) {
                listNameToIdMap.put(list.getName().toLowerCase(), list.getId());
                listIdToNameMap.put(list.getId(), list.getName());

                // read the name of the inbox with the correct case
                if(INBOX_LIST_NAME.equalsIgnoreCase(list.getName()))
                    INBOX_LIST_NAME = list.getName();
            }
            postUpdate(new ProgressUpdater(2, 5));

            // read all tasks
            LinkedList<TaskProxy> remoteChanges = new LinkedList<TaskProxy>();
            Date lastSyncDate = Preferences.getSyncRTMLastSync(context);
            boolean shouldSyncIndividualLists = false;
            String filter = null;
            if(lastSyncDate == null)
                filter = "status:incomplete"; // 1st time sync: get unfinished tasks

            // try the quick synchronization
            try {
                Thread.sleep(2000); // throttle
                postUpdate(new ProgressUpdater(3, 5));
                RtmTasks tasks = rtmService.tasks_getList(null, filter, lastSyncDate);
                postUpdate(new ProgressUpdater(5, 5));
                addTasksToList(tasks, remoteChanges);
            } catch (Exception e) {
                remoteChanges.clear();
                shouldSyncIndividualLists = true;
            }

            if(shouldSyncIndividualLists) {
                int progress = 0;
                for(final Entry<String, String> entry : listIdToNameMap.entrySet()) {
                	postUpdate(new ProgressLabelUpdater("Reading " +
                    		" list: " + entry.getValue()));
                	postUpdate(new ProgressUpdater(progress++,
                            listIdToNameMap.size()));
                    try {
                        Thread.sleep(1500);
                        RtmTasks tasks = rtmService.tasks_getList(entry.getKey(),
                                filter, lastSyncDate);
                        addTasksToList(tasks, remoteChanges);
                    } catch (Exception e) {
                    	postUpdate(new Runnable() {
                            public void run() {
                                DialogUtilities.okDialog(context,
                                        "List '" + entry.getValue() +
                                        "' import failed (too big?)", null);
                            }
                        });
                        continue;
                    }
                }
                postUpdate(new ProgressUpdater(1, 1));
            }

            synchronizeTasks(context, remoteChanges, new RtmSyncHelper(timeline));

            // add a bit of fudge time so we don't load tasks we just edited
            Date syncTime = new Date(System.currentTimeMillis() + 1000);
            Preferences.setSyncRTMLastSync(context, syncTime);

        } catch (Exception e) {
            showError(context, e, null);

        } finally {
            // on with the synchronization
            synchronizer.continueSynchronization(context);
        }
    }

    // --- helper methods

    /** Add the tasks read from RTM to the given list */
    private void addTasksToList(RtmTasks tasks, LinkedList<TaskProxy> list) {
        for(RtmTaskList taskList : tasks.getLists()) {
            for(RtmTaskSeries taskSeries : taskList.getSeries()) {
                TaskProxy remoteTask =
                    parseRemoteTask(taskList.getId(), taskSeries);
                list.add(remoteTask);
            }
        }
    }

    /** Get a task proxy with default RTM values */
    private TaskProxy getDefaultTaskProxy() {
        TaskProxy taskProxy = new TaskProxy(0, "");
        taskProxy.progressPercentage = 0;
        taskProxy.tags = new LinkedList<String>();
        taskProxy.notes = "";
        return taskProxy;
    }

    /** Send changes for the given TaskProxy across the wire */
    private void pushLocalTask(String timeline, TaskProxy task, TaskProxy remoteTask,
            SyncMapping mapping) throws ServiceException {
        RtmId id = new RtmId(mapping.getRemoteId());

        // fetch remote task for comparison (won't work if you renamed it)
        if(remoteTask == null) {
            RtmTaskSeries rtmTask = rtmService.tasks_getTask(id.taskSeriesId, task.name);
            if(rtmTask != null)
                remoteTask = parseRemoteTask(id.listId, rtmTask);
        }
        if(remoteTask == null)
            remoteTask = getDefaultTaskProxy();

        if(task.name != null && !task.name.equals(remoteTask.name))
            rtmService.tasks_setName(timeline, id.listId, id.taskSeriesId,
                    id.taskId, task.name);
        if(task.importance != null && !task.importance.equals(remoteTask.importance))
            rtmService.tasks_setPriority(timeline, id.listId, id.taskSeriesId,
                    id.taskId, Priority.values()[task.importance.ordinal()]);

        // due date
        Date dueDate = task.dueDate;
        if(dueDate == null)
        	dueDate = task.definiteDueDate;
        if(dueDate == null)
            dueDate = task.preferredDueDate;
        if(dueDate != remoteTask.dueDate &&
                !dueDate.equals(remoteTask.dueDate))
            rtmService.tasks_setDueDate(timeline, id.listId, id.taskSeriesId,
                id.taskId, dueDate, dueDate != null);

        // progress
        if(task.progressPercentage != null && !task.progressPercentage.equals(
                remoteTask.progressPercentage)) {
            if(task.progressPercentage == 100)
                rtmService.tasks_complete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
            else
                rtmService.tasks_uncomplete(timeline, id.listId, id.taskSeriesId,
                        id.taskId);
        }

        // notes
        if(task.notes != null && task.notes.length() > 0 &&
                !task.notes.equals(remoteTask.notes))
            rtmService.tasks_notes_add(timeline, id.listId, id.taskSeriesId,
                    id.taskId, "From Astrid", task.notes);

        // tags
        if(task.tags != null && !task.tags.equals(remoteTask.tags)) {
            String listName = listIdToNameMap.get(id.listId);
            if(task.tags.size() > 0 && listName.equals(task.tags.getFirst()))
                task.tags.remove(0);
            rtmService.tasks_setTags(timeline, id.listId, id.taskSeriesId,
                    id.taskId, task.tags.toArray(new String[task.tags.size()]));
        }

        // estimated time
        if(task.estimatedSeconds == 0 && remoteTask.estimatedSeconds != null ||
                task.estimatedSeconds > 0 && remoteTask.estimatedSeconds == null) {
            String estimation;
            int estimatedSeconds = task.estimatedSeconds;
            if(estimatedSeconds == 0)
                estimation = "";
            else if(estimatedSeconds < 3600)
                estimation = estimatedSeconds/60 + " minutes";
            else if(estimatedSeconds < 24*3600) {
                int hours = (estimatedSeconds/3600);
                estimation = hours+ " hours ";
                if(hours*3600 != estimatedSeconds)
                    estimation += estimatedSeconds - hours*3600 + " minutes";
            } else
                estimation = estimatedSeconds/3600/24 + " days";
            rtmService.tasks_setEstimate(timeline, id.listId, id.taskSeriesId,
                    id.taskId, estimation);
        }
    }

    /** Create a task proxy for the given RtmTaskSeries */
    private TaskProxy parseRemoteTask(String listId, RtmTaskSeries rtmTaskSeries) {
        TaskProxy task = new TaskProxy(getId(),
                new RtmId(listId, rtmTaskSeries).toString());

        task.name = rtmTaskSeries.getName();

        // notes
        StringBuilder sb = new StringBuilder();
        for(RtmTaskNote note: rtmTaskSeries.getNotes().getNotes()) {
            sb.append(note.getText() + "\n");
        }
        if(sb.length() > 0)
            task.notes = sb.toString().trim();

        // repeat
        if(rtmTaskSeries.hasRecurrence())
            task.syncOnComplete = true;

        // list / tags
        LinkedList<String> tagsList = rtmTaskSeries.getTags();
        String listName = listIdToNameMap.get(listId);
        if(listName != null && !listName.equals(INBOX_LIST_NAME)) {
            if(tagsList == null)
                tagsList = new LinkedList<String>();
            tagsList.addFirst(listName);
        }
        if(tagsList != null)
            task.tags = tagsList;

        RtmTask rtmTask = rtmTaskSeries.getTask();
        String estimate = rtmTask.getEstimate();
        if(estimate != null && estimate.length() > 0) {
            task.estimatedSeconds = parseEstimate(estimate);
        }
        task.creationDate = rtmTaskSeries.getCreated();
        task.completionDate = rtmTask.getCompleted();
        task.isDeleted = rtmTask.getDeleted() != null;
        if(rtmTask.getDue() != null) {
            Date due = rtmTask.getDue();

            // if no time is set, set it to midnight
            if(due.getHours() == 0 && due.getMinutes() == 0 && due.getSeconds() == 0) {
                due.setHours(23);
                due.setMinutes(59);
            }
            task.dueDate = due;
        }
        task.progressPercentage = (rtmTask.getCompleted() == null) ? 0 : 100;
        task.importance = Importance.values()[rtmTask.getPriority().ordinal()];

        return task;
    }

    /** Parse an estimated time of the format ## {s,m,h,d,w,mo} and return
     * the duration in seconds. Returns null on failure. */
    private Integer parseEstimate(String estimate) {
        try {
            float total = 0;
            int position = 0;
            while(position != -1) {
                for(; position < estimate.length(); position++) {
                    char c = estimate.charAt(position);
                    if(c != '.' && (c < '0' || c > '9'))
                        break;
                }
                float numberPortion = Float.parseFloat(estimate.substring(0, position));
                String stringPortion = estimate.substring(position).trim();
                position = stringPortion.indexOf(" ");
                if(position != -1)
                    estimate = stringPortion.substring(position+1).trim();

                if(stringPortion.startsWith("mo"))
                    total += numberPortion * 31 * 24 * 3600;
                else if(stringPortion.startsWith("w"))
                    total += numberPortion * 7 * 24 * 3600;
                else if(stringPortion.startsWith("d"))
                    total += numberPortion * 24 * 3600;
                else if(stringPortion.startsWith("h"))
                    total += numberPortion * 3600;
                else if(stringPortion.startsWith("m"))
                    total += numberPortion * 60;
                else if(stringPortion.startsWith("s"))
                    total += numberPortion;
            }
            return (int)total;
        } catch (Exception e) { /* */ }
        return null;
    }

    // --- helper classes

    /** SynchronizeHelper for remember the milk */
    class RtmSyncHelper implements SynchronizeHelper {
        private String timeline;
        private String lastCreatedTask = null;

        public RtmSyncHelper(String timeline) {
            this.timeline = timeline;
        }
        public String createTask(TaskModelForSync task) throws IOException {
            String listId = listNameToIdMap.get(INBOX_LIST_NAME.toLowerCase());
            RtmTaskSeries s = rtmService.tasks_add(timeline, listId,
                    task.getName());
            lastCreatedTask = new RtmId(listId, s).toString();
            return lastCreatedTask;
        }
        public void deleteTask(SyncMapping mapping) throws IOException {
            RtmId id = new RtmId(mapping.getRemoteId());
            rtmService.tasks_delete(timeline, id.listId, id.taskSeriesId,
                    id.taskId);
        }
        public void pushTask(TaskProxy task, TaskProxy remoteTask,
                SyncMapping mapping) throws IOException {

            // don't save the stuff that we already saved by creating the task
            if(task.getRemoteId().equals(lastCreatedTask))
                task.name = null;

            pushLocalTask(timeline, task, remoteTask, mapping);
        }
        public TaskProxy refetchTask(TaskProxy task) throws IOException {
            RtmId id = new RtmId(task.getRemoteId());
            RtmTaskSeries rtmTask = rtmService.tasks_getTask(id.taskSeriesId,
                    task.name);
            if(rtmTask == null)
                return task; // can't fetch
            return parseRemoteTask(id.listId, rtmTask);
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
}
