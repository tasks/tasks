package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import org.tasks.backup.XmlReader;
import org.tasks.backup.XmlWriter;

@Entity(tableName = "alarms")
public class Alarm {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "task")
    private Long task;

    @ColumnInfo(name = "time")
    private Long time;

    public Alarm() {

    }

    @Ignore
    public Alarm(XmlReader xml) {
        xml.readLong("task", this::setTask);
        xml.readLong("time", this::setTime);
    }

    public void writeToXml(XmlWriter writer) {
        writer.writeLong("task", task);
        writer.writeLong("time", time);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTask() {
        return task;
    }

    public void setTask(Long task) {
        this.task = task;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        if (id != null ? !id.equals(alarm.id) : alarm.id != null) return false;
        if (task != null ? !task.equals(alarm.task) : alarm.task != null) return false;
        return time != null ? time.equals(alarm.time) : alarm.time == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (task != null ? task.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "id=" + id +
                ", task=" + task +
                ", time=" + time +
                '}';
    }
}
