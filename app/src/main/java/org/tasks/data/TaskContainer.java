package org.tasks.data;

import androidx.room.Embedded;
import com.todoroo.astrid.data.Task;
import java.util.Objects;

public class TaskContainer {
  @Embedded public Task task;
  @Embedded public SubsetGoogleTask googletask;
  @Embedded public SubsetCaldav caldavTask;
  @Embedded public Location location;
  public String tags;
  public int children;
  public int siblings;
  public long primarySort;
  public long secondarySort;
  public int indent;
  private int targetIndent;

  public String getTagsString() {
    return tags;
  }

  public String getGoogleTaskList() {
    return googletask == null ? null : googletask.getListId();
  }

  public String getCaldav() {
    return caldavTask == null ? null : caldavTask.getCalendar();
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
    targetIndent = indent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TaskContainer)) {
      return false;
    }
    TaskContainer that = (TaskContainer) o;
    return children == that.children
        && siblings == that.siblings
        && primarySort == that.primarySort
        && secondarySort == that.secondarySort
        && indent == that.indent
        && targetIndent == that.targetIndent
        && Objects.equals(task, that.task)
        && Objects.equals(googletask, that.googletask)
        && Objects.equals(caldavTask, that.caldavTask)
        && Objects.equals(location, that.location)
        && Objects.equals(tags, that.tags);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(task, googletask, caldavTask, location, tags, children, siblings, primarySort,
            secondarySort, indent, targetIndent);
  }

  @Override
  public String toString() {
    return "TaskContainer{"
        + "task="
        + task
        + ", googletask="
        + googletask
        + ", caldavTask="
        + caldavTask
        + ", location="
        + location
        + ", tags='"
        + tags
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
        + ", targetIndent="
        + targetIndent
        + '}';
  }

  public String getUuid() {
    return task.getUuid();
  }

  public long getParent() {
    if (googletask != null) {
      return googletask.getParent();
    } else {
      return task.getParent();
    }
  }

  public void setParent(long parent) {
    if (googletask != null) {
      task.setParent(0);
      googletask.setParent(parent);
    } else {
      task.setParent(parent);
    }
    task.setParentUuid(null);
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

  public SubsetGoogleTask getGoogleTask() {
    return googletask;
  }

  public SubsetCaldav getCaldavTask() {
    return caldavTask;
  }

  public int getTargetIndent() {
    return targetIndent;
  }

  public void setTargetIndent(int indent) {
    targetIndent = indent;
  }

  public boolean hasLocation() {
    return location != null;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isCollapsed() {
    return task.isCollapsed();
  }
}
