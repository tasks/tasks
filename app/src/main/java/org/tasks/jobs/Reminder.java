package org.tasks.jobs;

public class Reminder implements JobQueueEntry {
    private final long taskId;
    private final long time;
    private final int type;

    public Reminder(long taskId, long time, int type) {
        this.taskId = taskId;
        this.time = time;
        this.type = type;
    }

    @Override
    public long getId() {
        return taskId;
    }

    @Override
    public long getTime() {
        return time;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reminder reminder = (Reminder) o;

        if (taskId != reminder.taskId) return false;
        if (time != reminder.time) return false;
        return type == reminder.type;

    }

    @Override
    public int hashCode() {
        int result = (int) (taskId ^ (taskId >>> 32));
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + type;
        return result;
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "taskId=" + taskId +
                ", time=" + time +
                ", type=" + type +
                '}';
    }
}
