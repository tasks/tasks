package org.tasks.data;

public class SubtaskInfo {
  public boolean hasSubtasks;
  public boolean hasGoogleSubtasks;

  public boolean usesSubtasks() {
    return hasSubtasks || hasGoogleSubtasks;
  }
}
