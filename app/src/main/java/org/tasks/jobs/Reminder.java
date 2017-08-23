package org.tasks.jobs;

import org.tasks.notifications.Notification;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

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

    @Override
    public Notification toNotification() {
        Notification notification = new Notification();
        notification.taskId = taskId;
        notification.type = type;
        notification.timestamp = currentTimeMillis();
        return notification;
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
