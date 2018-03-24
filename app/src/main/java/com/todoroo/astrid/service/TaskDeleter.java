package com.todoroo.astrid.service;

import static com.todoroo.andlib.sql.Criterion.all;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.notCompleted;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.calendars.CalendarEventProvider;
import org.tasks.data.AlarmDao;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;

public class TaskDeleter {

  private final TaskDao taskDao;
  private final CalendarEventProvider calendarEventProvider;
  private final AlarmDao alarmDao;
  private final LocationDao locationDao;
  private final TagDao tagDao;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;

  @Inject
  public TaskDeleter(TaskDao taskDao, CalendarEventProvider calendarEventProvider,
      AlarmDao alarmDao, LocationDao locationDao, TagDao tagDao,
      GoogleTaskDao googleTaskDao, CaldavDao caldavDao) {
    this.taskDao = taskDao;
    this.calendarEventProvider = calendarEventProvider;
    this.alarmDao = alarmDao;
    this.locationDao = locationDao;
    this.tagDao = tagDao;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
  }

  public int purgeDeleted() {
    List<Task> deleted = taskDao.getDeleted();
    for (Task task : deleted) {
      calendarEventProvider.deleteEvent(task);
      long id = task.getId();
      taskDao.deleteById(id);
      alarmDao.deleteByTaskId(id);
      locationDao.deleteByTaskId(id);
      tagDao.deleteByTaskId(id);
      googleTaskDao.deleteByTaskId(id);
      caldavDao.deleteById(id);
    }
    return deleted.size();
  }

  public void markDeleted(Task item) {
    if (!item.isSaved()) {
      return;
    }

    item.setDeletionDate(now());
    taskDao.save(item);
  }

  public List<Task> markDeleted(List<Long> taskIds) {
    List<Task> tasks = taskDao.fetch(taskIds);
    for (Task task : tasks) {
      markDeleted(task);
    }
    return tasks;
  }

  public int clearCompleted(Filter filter) {
    List<Task> completed = new ArrayList<>();
    String query = filter.getSqlQuery()
        .replace(isVisible().toString(), all.toString())
        .replace(notCompleted().toString(), all.toString());
    for (Task task : taskDao.fetchFiltered(query)) {
      if (task.isCompleted()) {
        completed.add(task);
      }
    }
    for (Task task : completed) {
      markDeleted(task);
    }
    return completed.size();
  }
}
