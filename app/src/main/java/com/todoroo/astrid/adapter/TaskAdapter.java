/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Longs.asList;

import android.arch.paging.AsyncPagedListDiffer;
import com.google.common.collect.ObjectArrays;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.data.Task;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.tasks.data.TaskAttachment;

/**
 * Adapter for displaying a user's tasks as a list
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TaskAdapter {

  private static final StringProperty TAGS =
      new StringProperty(
              null,
              "group_concat(nullif("
                  + TaskListFragment.TAGS_METADATA_JOIN
                  + ".tag_uid, '')"
                  + ", ',')")
          .as("tags");
  private static final StringProperty GTASK =
      new StringProperty(null, "nullif(" + TaskListFragment.GTASK_METADATA_JOIN + ".list_id, '')")
          .as("googletask");
  private static final StringProperty CALDAV =
      new StringProperty(null, "nullif(" + TaskListFragment.CALDAV_METADATA_JOIN + ".calendar, '')")
          .as("caldav");

  private static final LongProperty FILE_ID_PROPERTY =
      TaskAttachment.ID.cloneAs(TaskListFragment.FILE_METADATA_JOIN, "fileId");
  public static final Property<?>[] PROPERTIES =
      ObjectArrays.concat(
          Task.PROPERTIES,
          new Property<?>[] {
            TAGS, // Concatenated list of tags
            GTASK,
            CALDAV,
            FILE_ID_PROPERTY // File id
          },
          Property.class);
  private final Set<Long> selected = new HashSet<>();
  private AsyncPagedListDiffer<Task> helper;
  private OnCompletedTaskListener onCompletedTaskListener = null;

  public int getCount() {
    return helper.getItemCount();
  }

  public void setHelper(AsyncPagedListDiffer<Task> helper) {
    this.helper = helper;
  }

  public List<Long> getSelected() {
    return newArrayList(selected);
  }

  public void setSelected(long... ids) {
    selected.clear();
    selected.addAll(asList(ids));
  }

  public void clearSelections() {
    selected.clear();
  }

  public int getIndent(Task task) {
    return 0;
  }

  public boolean canIndent(int position, Task task) {
    return false;
  }

  public boolean isSelected(Task task) {
    return selected.contains(task.getId());
  }

  public void toggleSelection(Task task) {
    long id = task.getId();
    if (selected.contains(id)) {
      selected.remove(id);
    } else {
      selected.add(id);
    }
  }

  public boolean isManuallySorted() {
    return false;
  }

  public void moved(int from, int to) {}

  public void indented(int position, int delta) {}

  public long getTaskId(int position) {
    return getTask(position).getId();
  }

  public Task getTask(int position) {
    return helper.getItem(position);
  }

  protected String getItemUuid(int position) {
    return getTask(position).getUuid();
  }

  public void onCompletedTask(Task task, boolean newState) {
    if (onCompletedTaskListener != null) {
      onCompletedTaskListener.onCompletedTask(task, newState);
    }
  }

  public void setOnCompletedTaskListener(final OnCompletedTaskListener newListener) {
    this.onCompletedTaskListener = newListener;
  }

  public interface OnCompletedTaskListener {

    void onCompletedTask(Task item, boolean newState);
  }
}
