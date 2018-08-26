package org.tasks.gtasks;

import com.todoroo.astrid.data.Task;

import java.util.Date;

import java.util.List;

public class GoogleTaskAdditionalMetadata {

    public enum Importance {
        MUST(Task.Priority.HIGH),
        HIGH(Task.Priority.MEDIUM),
        DEFAULT(Task.Priority.LOW),
        LOW(Task.Priority.NONE);

        private int taskImportance;

        Importance(int taskImportance) {
            this.taskImportance = taskImportance;
        }

        public int getTaskImportance() {
            return taskImportance;
        }

        public static Importance valueOf(int taskImportance) {
            Importance result = DEFAULT;
            for(Importance i: values()) {
                if (i.getTaskImportance()==taskImportance) {
                    result = i;
                }
            }
            return result;
        }
    }

    private List<String> tags;
    private Importance importance;
    private Date hideUntil;
    private Boolean notifyAtDeadline;
    private Boolean notifyAfterDeadline;
    private Boolean notifyModeNonstop;
    private Boolean notifyModeFive;
    private Date repeatUntil;
    private String recurrence;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    public Date getHideUntil() {
        return hideUntil;
    }

    public void setHideUntil(Date hideUntil) {
        this.hideUntil = hideUntil;
    }

    public Boolean isNotifyAtDeadline() {
        return notifyAtDeadline;
    }

    public void setNotifyAtDeadline(boolean notifyAtDeadline) {
        this.notifyAtDeadline = notifyAtDeadline;
    }

    public Boolean isNotifyAfterDeadline() {
        return notifyAfterDeadline;
    }

    public void setNotifyAfterDeadline(boolean notifyAfterDeadline) {
        this.notifyAfterDeadline = notifyAfterDeadline;
    }

    public Boolean isNotifyModeNonstop() {
        return notifyModeNonstop;
    }

    public void setNotifyModeNonstop(boolean notifyModeNonstop) {
        this.notifyModeNonstop = notifyModeNonstop;
    }

    public Boolean isNotifyModeFive() {
        return notifyModeFive;
    }

    public void setNotifyModeFive(boolean notifyModeFive) {
        this.notifyModeFive = notifyModeFive;
    }

    public Date getRepeatUntil() {
        return repeatUntil;
    }

    public void setRepeatUntil(Date repeatUntil) {
        this.repeatUntil = repeatUntil;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }
}
