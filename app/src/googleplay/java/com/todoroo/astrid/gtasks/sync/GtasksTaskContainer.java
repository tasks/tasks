/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import com.google.api.client.util.DateTime;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;

import org.tasks.data.GoogleTask;

import java.util.ArrayList;

public class GtasksTaskContainer {

    public final Task task;
    public final ArrayList<GoogleTask> metadata;

    public GoogleTask gtaskMetadata;
    private final long updateTime;

    public GtasksTaskContainer(com.google.api.services.tasks.model.Task remoteTask, String listId, GoogleTask metadata) {
        this.task = new Task();
        this.metadata = new ArrayList<>();
        this.gtaskMetadata = metadata;

        task.setTitle(remoteTask.getTitle());
        task.setCreationDate(DateUtilities.now());
        task.setCompletionDate(GtasksApiUtilities.gtasksCompletedTimeToUnixTime(remoteTask.getCompleted()));
        if (remoteTask.getDeleted() == null || !remoteTask.getDeleted()) {
            task.setDeletionDate(0L);
        } else {
            task.setDeletionDate(DateUtilities.now());
        }
        if (remoteTask.getHidden() != null && remoteTask.getHidden()) {
            task.setDeletionDate(DateUtilities.now());
        }

        long dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue());
        long createdDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate);
        task.setDueDate(createdDate);
        task.setNotes(remoteTask.getNotes());

        gtaskMetadata.setRemoteId(remoteTask.getId());
        gtaskMetadata.setListId(listId);

        DateTime updated = remoteTask.getUpdated();
        updateTime = updated == null ? 0 : updated.getValue();
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void prepareForSaving() {
        metadata.add(gtaskMetadata);
    }
}
