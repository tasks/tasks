package com.timsu.astrid.sync;

import java.util.Date;

import com.timsu.astrid.data.enums.Importance;
import com.timsu.astrid.data.enums.RepeatInterval;
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

    String          name                = null;
    String          notes               = null;

    Importance      importance          = null;
    Integer         progressPercentage  = null;

    Date            creationDate        = null;
    Date            completionDate      = null;

    Date            definiteDueDate     = null;
    Date            preferredDueDate    = null;
    Date            hiddenUntil         = null;

    String[]        tags                = null;

    Integer         estimatedSeconds    = null;
    Integer         elapsedSeconds      = null;
    Integer         repeatEveryNSeconds = null;

    // --- internal state

    /** id of the synchronization service */
    private int     syncServiceId;

    /** id of this particular remote task */
    private String  syncTaskId;

    /** was the task deleted on the remote server */
    private boolean isDeleted           = false;

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
        hiddenUntil = task.getHiddenUntil();
        estimatedSeconds = task.getEstimatedSeconds();
        elapsedSeconds = task.getElapsedSeconds();
        RepeatInfo repeatInfo = task.getRepeat();
        if(repeatInfo != null) {
            repeatEveryNSeconds = (int)(repeatInfo.shiftDate(new Date(0)).getTime()/1000);
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
        if(definiteDueDate != null)
            task.setDefiniteDueDate(definiteDueDate);
        if(preferredDueDate != null)
            task.setPreferredDueDate(preferredDueDate);
        if(hiddenUntil != null)
            task.setHiddenUntil(hiddenUntil);

        // TODO tags

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
