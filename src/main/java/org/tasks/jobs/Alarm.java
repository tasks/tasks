package org.tasks.jobs;

import com.todoroo.astrid.alarms.AlarmFields;
import com.todoroo.astrid.data.Metadata;

public class Alarm implements JobQueueEntry {
    private final long alarmId;
    private final long taskId;
    private final long time;

    public Alarm(Metadata metadata) {
        this(metadata.getId(), metadata.getTask(), metadata.getValue(AlarmFields.TIME));
    }

    public Alarm(long alarmId, long taskId, Long time) {
        this.alarmId = alarmId;
        this.taskId = taskId;
        this.time = time;
    }

    @Override
    public long getId() {
        return alarmId;
    }

    public long getTaskId() {
        return taskId;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        return alarmId == alarm.alarmId;
    }

    @Override
    public int hashCode() {
        return (int) (alarmId ^ (alarmId >>> 32));
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "alarmId=" + alarmId +
                ", taskId=" + taskId +
                ", time=" + time +
                '}';
    }
}
