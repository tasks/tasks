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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.enums.RepeatInterval;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForSync;
import com.timsu.astrid.data.task.AbstractTaskModel.RepeatInfo;

/** Representation of a task on a remote server. Your synchronization
 * service should instantiate these, filling out every field (use null
 * where the field does not exist).
 *
 * @author timsu
 *
 */
public class TaskProxy {

    TaskProxy(int syncServiceId, String syncTaskId, boolean isDeleted) {
        this.syncServiceId = syncServiceId;
        this.syncTaskId = syncTaskId;
        this.isDeleted = isDeleted;
    }

    // --- fill these out

    String             name                = null;
    String             notes               = null;

    Importance         importance          = null;
    Integer            progressPercentage  = null;

    Date               creationDate        = null;
    Date               completionDate      = null;

    Date               dueDate             = null;
    Date               definiteDueDate     = null;
    Date               preferredDueDate    = null;
    Date               hiddenUntil         = null;

    LinkedList<String> tags                = null;

    Integer            estimatedSeconds    = null;
    Integer            elapsedSeconds      = null;
    Integer            repeatEveryNSeconds = null;

    // --- internal state

    /** id of the synchronization service */
    private int        syncServiceId;

    /** id of this particular remote task */
    private String     syncTaskId;

    /** was the task deleted on the remote server */
    private boolean    isDeleted           = false;

    public int getSyncServiceId() {
        return syncServiceId;
    }

    public String getRemoteId() {
        return syncTaskId;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    // --- helper methods

    /** Merge with another TaskProxy. Fields in this taskProxy will be overwritten! */
    public void mergeWithOther(TaskProxy other) {
        if(other == null)
            return;

        if(other.name != null)
            name = other.name;
        if(other.notes != null)
            notes = other.notes;
        if(other.importance != null)
            importance = other.importance;
        if(other.progressPercentage != null)
            progressPercentage = other.progressPercentage;
        if(other.creationDate != null)
            creationDate = other.creationDate;
        if(other.completionDate != null)
            completionDate = other.completionDate;
        if(other.dueDate != null)
        	dueDate = other.dueDate;
        if(other.definiteDueDate != null)
            definiteDueDate = other.definiteDueDate;
        if(other.preferredDueDate != null)
            preferredDueDate = other.preferredDueDate;
        if(other.hiddenUntil != null)
            hiddenUntil = other.hiddenUntil;
        if(other.tags != null)
            tags = other.tags;
        if(other.estimatedSeconds != null)
            estimatedSeconds = other.estimatedSeconds;
        if(other.elapsedSeconds != null)
            elapsedSeconds = other.elapsedSeconds;
        if(other.repeatEveryNSeconds != null)
            repeatEveryNSeconds = other.repeatEveryNSeconds;
    }

    /** Read from the given task model */
    public void readFromTaskModel(TaskModelForSync task) {
        name = task.getName();
        notes = task.getNotes();
        importance = task.getImportance();
        progressPercentage = task.getProgressPercentage();
        creationDate = task.getCreationDate();
        completionDate = task.getCompletionDate();
        definiteDueDate = task.getDefiniteDueDate();
        preferredDueDate = task.getPreferredDueDate();
        dueDate = definiteDueDate != null ? definiteDueDate : preferredDueDate;
        hiddenUntil = task.getHiddenUntil();
        estimatedSeconds = task.getEstimatedSeconds();
        elapsedSeconds = task.getElapsedSeconds();
        RepeatInfo repeatInfo = task.getRepeat();
        if(repeatInfo != null) {
            repeatEveryNSeconds = (int)(repeatInfo.shiftDate(new Date(0)).getTime()/1000);
        }
    }

    /** Read tags from the given tag controller */
    public void readTagsFromController(TaskIdentifier taskId,
            TagController tagController, HashMap<TagIdentifier, TagModelForView>
            tagList) {
        LinkedList<TagIdentifier> tagIds = tagController.getTaskTags(taskId);
        tags = new LinkedList<String>();
        for(TagIdentifier tagId : tagIds) {
            tags.add(tagList.get(tagId).getName());
        }
    }

    /** Write to the given task model */
    public void writeToTaskModel(TaskModelForSync task) {
        if(name != null)
            task.setName(name);
        if(notes != null)
            task.setNotes(notes);
        if(importance != null)
            task.setImportance(importance);
        if(progressPercentage != null)
            task.setProgressPercentage(progressPercentage);
        if(creationDate != null)
            task.setCreationDate(creationDate);
        if(completionDate != null)
            task.setCompletionDate(completionDate);

        // date handling: if sync service only supports one type of due date,
        // we have to figure out which field to write to based on what
        // already has data

        if(dueDate != null) {
        	if(task.getDefiniteDueDate() != null)
        		task.setDefiniteDueDate(dueDate);
        	else if(task.getPreferredDueDate() != null)
        		task.setPreferredDueDate(dueDate);
        	else
        		task.setDefiniteDueDate(dueDate);
        } else {
	        if(definiteDueDate != null)
	            task.setDefiniteDueDate(definiteDueDate);
	        if(preferredDueDate != null)
	            task.setPreferredDueDate(preferredDueDate);
        }

        if(hiddenUntil != null)
            task.setHiddenUntil(hiddenUntil);
        if(estimatedSeconds != null)
            task.setEstimatedSeconds(estimatedSeconds);
        if(elapsedSeconds != null)
            task.setElapsedSeconds(elapsedSeconds);

        // this is inaccurate. =/
        if(repeatEveryNSeconds != null) {
            RepeatInterval repeatInterval;
            int repeatValue;
            if(repeatEveryNSeconds < 7 * 24 * 3600) {
                repeatInterval = RepeatInterval.DAYS;
                repeatValue = repeatEveryNSeconds / (24 * 3600);
            } else if(repeatEveryNSeconds < 30 * 24 * 3600) {
                repeatInterval = RepeatInterval.WEEKS;
                repeatValue = repeatEveryNSeconds / (7 * 24 * 3600);
            } else {
                repeatInterval = RepeatInterval.MONTHS;
                repeatValue = repeatEveryNSeconds / (30 * 24 * 3600);
            }
            task.setRepeat(new RepeatInfo(repeatInterval, repeatValue));
        }
    }
}
