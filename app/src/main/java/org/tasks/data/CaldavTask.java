package org.tasks.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import com.todoroo.andlib.data.Table;

@Entity(tableName = "caldav_tasks")
public class CaldavTask {

    public static final String KEY = "caldav";

    @Deprecated
    public static final Table TABLE = new Table("caldav_tasks");

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "task")
    private long task;

    @ColumnInfo(name = "account")
    private String account;

    @ColumnInfo(name = "remote_id")
    private String remoteId;

    @ColumnInfo(name = "etag")
    private String etag;

    @ColumnInfo(name = "last_sync")
    private long lastSync;

    @ColumnInfo(name = "deleted")
    private long deleted;

    public CaldavTask() {

    }

    @Ignore
    public CaldavTask(long task, String account) {
        this.task = task;
        this.account = account;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTask() {
        return task;
    }

    public void setTask(long task) {
        this.task = task;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getLastSync() {
        return lastSync;
    }

    public void setLastSync(long lastSync) {
        this.lastSync = lastSync;
    }

    public long getDeleted() {
        return deleted;
    }

    public void setDeleted(long deleted) {
        this.deleted = deleted;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
