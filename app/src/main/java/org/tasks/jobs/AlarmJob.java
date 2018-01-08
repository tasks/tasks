package org.tasks.jobs;

import com.todoroo.astrid.alarms.AlarmFields;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.reminders.ReminderService;

import org.tasks.notifications.Notification;

import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class AlarmJob implements JobQueueEntry {
    private final long alarmId;
    private final long taskId;
    private final long time;

    public AlarmJob(Metadata metadata) {
        this(metadata.getId(), metadata.getTask(), metadata.getValue(AlarmFields.TIME));
    }

    public AlarmJob(long alarmId, long taskId, Long time) {
        this.alarmId = alarmId;
        this.taskId = taskId;
        this.time = time;
    }

    @Override
    public long getId() {
        return alarmId;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public Notification toNotification() {
        Notification notification = new Notification();
        notification.taskId = taskId;
        notification.type = ReminderService.TYPE_ALARM;
        notification.timestamp = currentTimeMillis();
        return notification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlarmJob alarmJob = (AlarmJob) o;

        if (alarmId != alarmJob.alarmId) return false;
        if (taskId != alarmJob.taskId) return false;
        return time == alarmJob.time;
    }

    @Override
    public int hashCode() {
        int result = (int) (alarmId ^ (alarmId >>> 32));
        result = 31 * result + (int) (taskId ^ (taskId >>> 32));
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "AlarmJob{" +
                "alarmId=" + alarmId +
                ", taskId=" + taskId +
                ", time=" + time +
                '}';
    }
}
