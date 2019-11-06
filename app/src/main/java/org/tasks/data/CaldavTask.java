package org.tasks.data;

import static com.todoroo.astrid.helper.UUIDHelper.newUUID;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.todoroo.andlib.data.Table;

@Entity(tableName = "caldav_tasks")
public class CaldavTask {

  public static final String KEY = "caldav";

  @Deprecated public static final Table TABLE = new Table("caldav_tasks");

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "cd_id")
  private long id;

  @ColumnInfo(name = "cd_task")
  private long task;

  @ColumnInfo(name = "cd_calendar")
  private String calendar;

  @ColumnInfo(name = "cd_object")
  private String object;

  @ColumnInfo(name = "cd_remote_id")
  private String remoteId;

  @ColumnInfo(name = "cd_etag")
  private String etag;

  @ColumnInfo(name = "cd_last_sync")
  private long lastSync;

  @ColumnInfo(name = "cd_deleted")
  private long deleted;

  @ColumnInfo(name = "cd_vtodo")
  private String vtodo;

  @ColumnInfo(name = "cd_parent")
  private long parent;

  @ColumnInfo(name = "cd_remote_parent")
  private String remoteParent;

  public CaldavTask() {}

  @Ignore
  public CaldavTask(long task, String calendar) {
    this.task = task;
    this.calendar = calendar;
    this.remoteId = newUUID();
    this.object = remoteId + ".ics";
  }

  @Ignore
  public CaldavTask(long task, String calendar, String remoteId, String object) {
    this.task = task;
    this.calendar = calendar;
    this.remoteId = remoteId;
    this.object = object;
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

  public String getCalendar() {
    return calendar;
  }

  public void setCalendar(String calendar) {
    this.calendar = calendar;
  }

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
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

  public String getVtodo() {
    return vtodo;
  }

  public void setVtodo(String vtodo) {
    this.vtodo = vtodo;
  }

  public long getParent() {
    return parent;
  }

  public void setParent(long parent) {
    this.parent = parent;
  }

  public String getRemoteParent() {
    return remoteParent;
  }

  public void setRemoteParent(String remoteParent) {
    this.remoteParent = remoteParent;
  }

  @Override
  public String toString() {
    return "CaldavTask{"
        + "id="
        + id
        + ", task="
        + task
        + ", calendar='"
        + calendar
        + '\''
        + ", object='"
        + object
        + '\''
        + ", remoteId='"
        + remoteId
        + '\''
        + ", etag='"
        + etag
        + '\''
        + ", lastSync="
        + lastSync
        + ", deleted="
        + deleted
        + ", vtodo='"
        + vtodo
        + '\''
        + ", parent='"
        + parent
        + '\''
        + ", remoteParent='"
        + remoteParent
        + '\''
        + '}';
  }
}
