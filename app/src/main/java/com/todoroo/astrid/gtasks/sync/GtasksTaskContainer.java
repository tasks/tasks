/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import static org.tasks.gtasks.GoogleTaskSynchronizer.mergeDates;

import com.google.api.client.util.DateTime;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import java.util.ArrayList;
import org.tasks.data.GoogleTask;

public class GtasksTaskContainer {

  public final Task task;
  public final ArrayList<GoogleTask> metadata;
  public final GoogleTask gtaskMetadata;
  private final long updateTime;

  public GtasksTaskContainer(
      com.google.api.services.tasks.model.Task remoteTask,
      Task localTask,
      String listId,
      GoogleTask metadata) {
    task = localTask;
    this.metadata = new ArrayList<>();
    this.gtaskMetadata = metadata;

    task.setTitle(remoteTask.getTitle());
    task.setCreationDate(DateUtilities.now());
    task.setCompletionDate(
        GtasksApiUtilities.gtasksCompletedTimeToUnixTime(remoteTask.getCompleted()));

    long dueDate = GtasksApiUtilities.gtasksDueTimeToUnixTime(remoteTask.getDue());
    mergeDates(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate), task);
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
