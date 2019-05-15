/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks.sync;

import static org.tasks.gtasks.GoogleTaskSynchronizer.mergeDates;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;
import java.util.ArrayList;
import org.tasks.data.GoogleTask;

public class GtasksTaskContainer {

  public final Task task;
  public final ArrayList<GoogleTask> metadata;
  public final GoogleTask gtaskMetadata;

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
  }

  public void prepareForSaving() {
    metadata.add(gtaskMetadata);
  }
}
