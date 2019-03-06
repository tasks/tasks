package com.todoroo.astrid.adapter;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import java.util.ArrayList;
import java.util.Arrays;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;
import timber.log.Timber;

public final class GoogleTaskAdapter extends TaskAdapter {

  private final GoogleTaskList list;
  private final GtasksTaskListUpdater updater;

  public GoogleTaskAdapter(GoogleTaskList list, GtasksTaskListUpdater updater) {
    this.list = list;
    this.updater = updater;
  }

  @Override
  public int getIndent(Task task) {
    return task.getIndent();
  }

  @Override
  public boolean canIndent(int position, Task task) {
    Task parent = getTask(position - 1);
    return parent != null && getIndent(task) == 0;
  }

  @Override
  public boolean isManuallySorted() {
    return true;
  }

  @Override
  public void moved(int from, int to) {
    long targetTaskId = getTaskId(from);
    if (targetTaskId <= 0) {
      return; // This can happen with gestures on empty parts of the list (e.g. extra space below
      // tasks)
    }

    try {
      if (to >= getCount()) {
        updater.moveTo(list, targetTaskId, -1);
      } else {
        long destinationTaskId = getTaskId(to);
        updater.moveTo(list, targetTaskId, destinationTaskId);
      }
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public void indented(int which, int delta) {
    long targetTaskId = getTaskId(which);
    if (targetTaskId <= 0) {
      return; // This can happen with gestures on empty parts of the list (e.g. extra space below
      // tasks)
    }
    try {
      updater.indent(list, targetTaskId, delta);
    } catch (Exception e) {
      Timber.e(e);
    }
  }

  @Override
  public Property<?>[] getTaskProperties() {
    ArrayList<Property<?>> properties = new ArrayList<>(Arrays.asList(TaskAdapter.PROPERTIES));
    properties.add(GoogleTask.ORDER);
    properties.add(GoogleTask.INDENT);
    return properties.toArray(new Property<?>[properties.size()]);
  }
}
