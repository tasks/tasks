package org.tasks.data;

import androidx.room.Embedded;
import com.todoroo.astrid.data.Task;

public class TaskContainer {
  @Embedded public Task task;
  public String tags;
  public String googletask;
  public String caldav;
  public int order;
  public int indent;

  public int getIndent() {
    return indent;
  }

  public void setIndent(int indent) {
    this.indent = indent;
  }

  public String getTagsString() {
    return tags;
  }

  public String getGoogleTaskList() {
    return googletask;
  }

  public String getCaldav() {
    return caldav;
  }

  public String getNotes() {
    return task.getNotes();
  }

  public boolean hasNotes() {
    return task.hasNotes();
  }

  public String getTitle() {
    return task.getTitle();
  }

  public boolean isHidden() {
    return task.isHidden();
  }

  public boolean isCompleted() {
    return task.isCompleted();
  }

  public int getPriority() {
    return task.getPriority();
  }

  public String getRecurrence() {
    return task.getRecurrence();
  }

  public boolean hasDueDate() {
    return task.hasDueDate();
  }

  public boolean isOverdue() {
    return task.isOverdue();
  }

  public long getDueDate() {
    return task.getDueDate();
  }

  public Task getTask() {
    return task;
  }

  public long getId() {
    return task.getId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskContainer that = (TaskContainer) o;

    if (indent != that.indent) {
      return false;
    }
    if (task != null ? !task.equals(that.task) : that.task != null) {
      return false;
    }
    if (tags != null ? !tags.equals(that.tags) : that.tags != null) {
      return false;
    }
    if (googletask != null ? !googletask.equals(that.googletask) : that.googletask != null) {
      return false;
    }
    return caldav != null ? caldav.equals(that.caldav) : that.caldav == null;
  }

  @Override
  public int hashCode() {
    int result = task != null ? task.hashCode() : 0;
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    result = 31 * result + (googletask != null ? googletask.hashCode() : 0);
    result = 31 * result + (caldav != null ? caldav.hashCode() : 0);
    result = 31 * result + indent;
    return result;
  }

  @Override
  public String toString() {
    return "TaskContainer{"
        + "task="
        + task
        + ", tags='"
        + tags
        + '\''
        + ", googletask='"
        + googletask
        + '\''
        + ", caldav='"
        + caldav
        + '\''
        + ", indent="
        + indent
        + '}';
  }

  public String getUuid() {
    return task.getUuid();
  }
}
