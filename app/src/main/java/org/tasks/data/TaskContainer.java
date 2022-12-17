package org.tasks.data;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import com.todoroo.astrid.data.Task;
import java.util.Objects;

public class TaskContainer {
  @Embedded public Task task;
  @Embedded public SubsetCaldav caldavTask;
  @Embedded public Location location;
  public boolean isGoogleTask;
  public boolean parentComplete;
  public String tags;
  public int children;
  public Long sortGroup;
  public long primarySort;
  public long secondarySort;
  public int indent;
  private int targetIndent;

  public String getTagsString() {
    return tags;
  }

  public @Nullable String getCaldav() {
    return caldavTask.getCd_calendar();
  }

  public boolean isCaldavTask() {
    return caldavTask != null;
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

  public long getStartDate() {
    return task.getHideUntil();
  }

  public boolean isCompleted() {
    return task.isCompleted();
  }

  public boolean hasDueDate() {
    return task.hasDueDate();
  }

  public boolean hasDueTime() {
    return task.hasDueTime();
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

  public long getCreationDate() {
    return task.getCreationDate();
  }

  public void setIndent(int indent) {
    this.indent = indent;
    targetIndent = indent;
  }

  public boolean isReadOnly() {
    return task.getReadOnly();
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
        && primarySort == that.primarySort
        && secondarySort == that.secondarySort
        && indent == that.indent
        && targetIndent == that.targetIndent
        && Objects.equals(task, that.task)
        && Objects.equals(caldavTask, that.caldavTask)
        && Objects.equals(location, that.location)
        && Objects.equals(tags, that.tags)
        && Objects.equals(sortGroup, that.sortGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        task,
        caldavTask,
        location,
        tags,
        children,
        sortGroup,
        primarySort,
        secondarySort,
        indent,
        targetIndent);
  }

  @Override
  public String toString() {
    return "TaskContainer{"
        + "task="
        + task
        + ", caldavTask="
        + caldavTask
        + ", location="
        + location
        + ", tags='"
        + tags
        + '\''
        + ", children="
        + children
        + ", sortGroup="
        + sortGroup
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
    if (isGoogleTask) {
      return caldavTask.getGt_parent();
    } else {
      return task.getParent();
    }
  }

  public void setParent(long parent) {
    if (isGoogleTask) {
      caldavTask.setGt_parent(parent);
    } else {
      task.setParent(parent);
    }
  }

  public boolean hasParent() {
    return getParent() > 0;
  }

  public boolean hasChildren() {
    return children > 0;
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

  public long getCaldavSortOrder() {
    return indent == 0 ? primarySort : secondarySort;
  }

  public int getPriority() {
    return task.getPriority();
  }
}
