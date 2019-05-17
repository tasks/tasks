package org.tasks.data;

import androidx.room.Embedded;
import com.todoroo.astrid.data.Task;

public class TaskContainer {
  @Embedded public Task task;
  @Embedded public GoogleTask googletask;
  public String tags;
  public String caldav;
  public int children;
  public int siblings;
  public long primarySort;
  public long secondarySort;
  @Deprecated public int indent;

  public String getTagsString() {
    return tags;
  }

  public String getGoogleTaskList() {
    return googletask == null ? null : googletask.getListId();
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

  public long getPrimarySort() {
    return primarySort;
  }

  public long getSecondarySort() {
    return secondarySort;
  }

  public int getIndent() {
    return indent;
  }

  public void setIndent(int indent) {
    this.indent = indent;
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

    if (children != that.children) {
      return false;
    }
    if (siblings != that.siblings) {
      return false;
    }
    if (primarySort != that.primarySort) {
      return false;
    }
    if (secondarySort != that.secondarySort) {
      return false;
    }
    if (task != null ? !task.equals(that.task) : that.task != null) {
      return false;
    }
    if (googletask != null ? !googletask.equals(that.googletask) : that.googletask != null) {
      return false;
    }
    if (tags != null ? !tags.equals(that.tags) : that.tags != null) {
      return false;
    }
    return caldav != null ? caldav.equals(that.caldav) : that.caldav == null;
  }

  @Override
  public int hashCode() {
    int result = task != null ? task.hashCode() : 0;
    result = 31 * result + (googletask != null ? googletask.hashCode() : 0);
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    result = 31 * result + (caldav != null ? caldav.hashCode() : 0);
    result = 31 * result + children;
    result = 31 * result + siblings;
    result = 31 * result + (int) (primarySort ^ (primarySort >>> 32));
    result = 31 * result + (int) (secondarySort ^ (secondarySort >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "TaskContainer{"
        + "task="
        + task
        + ", googletask="
        + googletask
        + ", tags='"
        + tags
        + '\''
        + ", caldav='"
        + caldav
        + '\''
        + ", children="
        + children
        + ", siblings="
        + siblings
        + ", primarySort="
        + primarySort
        + ", secondarySort="
        + secondarySort
        + ", indent="
        + indent
        + '}';
  }

  public String getUuid() {
    return task.getUuid();
  }

  public long getParent() {
    return googletask == null ? 0 : googletask.getParent();
  }

  public void setParent(long parent) {
    googletask.setParent(parent);
  }

  public boolean hasParent() {
    return getParent() > 0;
  }

  public boolean hasChildren() {
    return children > 0;
  }

  public boolean isLastSubtask() {
    return secondarySort == siblings - 1;
  }

  public GoogleTask getGoogleTask() {
    return googletask;
  }
}
