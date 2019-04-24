package org.tasks.data;

import android.net.Uri;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.common.base.Strings;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.data.Task;
import java.io.File;

@Entity(tableName = "task_attachments")
public final class TaskAttachment {

  @Deprecated public static final Table TABLE = new Table("task_attachments");

  public static final String KEY = "attachment";

  /** default directory for files on external storage */
  public static final String FILES_DIRECTORY_DEFAULT = "attachments"; // $NON-NLS-1$

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "_id")
  private transient Long id;

  @ColumnInfo(name = "remoteId")
  private String remoteId = Task.NO_UUID;

  // -- Constants
  @ColumnInfo(name = "task_id")
  private String taskId = Task.NO_UUID;

  @ColumnInfo(name = "name")
  private String name = "";

  @ColumnInfo(name = "path")
  private String uri = "";

  @ColumnInfo(name = "content_type")
  private String contentType = "";

  public static TaskAttachment createNewAttachment(String taskUuid, Uri uri, String fileName) {
    TaskAttachment attachment = new TaskAttachment();
    attachment.setTaskId(taskUuid);
    attachment.setName(fileName);
    attachment.setUri(uri);
    return attachment;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(Uri uri) {
    setUri(uri == null ? null : uri.toString());
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void convertPathUri() {
    setUri(Uri.fromFile(new File(uri)).toString());
  }

  public Uri parseUri() {
    return Strings.isNullOrEmpty(uri) ? null : Uri.parse(uri);
  }
}
