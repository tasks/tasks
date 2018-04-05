package org.tasks.backup;

import static java.util.Collections.emptyList;

import com.todoroo.astrid.data.Task;
import java.util.List;
import org.tasks.data.Alarm;
import org.tasks.data.CaldavCalendar;
import org.tasks.data.CaldavTask;
import org.tasks.data.Filter;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskList;
import org.tasks.data.Location;
import org.tasks.data.Tag;
import org.tasks.data.TagData;
import org.tasks.data.TaskAttachment;
import org.tasks.data.UserActivity;

class BackupContainer {

  final List<TaskBackup> tasks;
  final List<TagData> tags;
  final List<Filter> filters;
  final List<GoogleTaskList> googleTaskLists;
  private final List<CaldavCalendar> caldavCalendars;

  BackupContainer(
      List<TaskBackup> tasks,
      List<TagData> tags,
      List<Filter> filters,
      List<GoogleTaskList> googleTaskLists,
      List<CaldavCalendar> caldavCalendars) {
    this.tasks = tasks;
    this.tags = tags;
    this.filters = filters;
    this.googleTaskLists = googleTaskLists;
    this.caldavCalendars = caldavCalendars;
  }

  public List<CaldavCalendar> getCaldavCalendars() {
    return caldavCalendars == null ? emptyList() : caldavCalendars;
  }

  static class TaskBackup {

    final Task task;
    final List<Alarm> alarms;
    final List<Location> locations;
    final List<Tag> tags;
    final List<GoogleTask> google;
    final List<UserActivity> comments;
    private final List<TaskAttachment> attachments;
    private final List<CaldavTask> caldavTasks;

    TaskBackup(
        Task task,
        List<Alarm> alarms,
        List<Location> locations,
        List<Tag> tags,
        List<GoogleTask> google,
        List<UserActivity> comments,
        List<TaskAttachment> attachments,
        List<CaldavTask> caldavTasks) {
      this.task = task;
      this.alarms = alarms;
      this.locations = locations;
      this.tags = tags;
      this.google = google;
      this.comments = comments;
      this.attachments = attachments;
      this.caldavTasks = caldavTasks;
    }

    List<TaskAttachment> getAttachments() {
      return attachments == null ? emptyList() : attachments;
    }

    List<CaldavTask> getCaldavTasks() {
      return caldavTasks == null ? emptyList() : caldavTasks;
    }
  }
}
