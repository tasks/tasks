package com.todoroo.astrid.service;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.calendars.CalendarEventProvider;
import org.tasks.data.AlarmDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.LocationDao;
import org.tasks.data.TagDao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.sql.Criterion.all;
import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.notCompleted;

public class TaskDeleter {

    private final TaskDao taskDao;
    private final CalendarEventProvider calendarEventProvider;
    private final AlarmDao alarmDao;
    private final LocationDao locationDao;
    private final TagDao tagDao;
    private final GoogleTaskDao googleTaskDao;

    @Inject
    public TaskDeleter(TaskDao taskDao, CalendarEventProvider calendarEventProvider,
                       AlarmDao alarmDao, LocationDao locationDao, TagDao tagDao,
                       GoogleTaskDao googleTaskDao) {
        this.taskDao = taskDao;
        this.calendarEventProvider = calendarEventProvider;
        this.alarmDao = alarmDao;
        this.locationDao = locationDao;
        this.tagDao = tagDao;
        this.googleTaskDao = googleTaskDao;
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
        }
        return deleted.size();
    }

    public void markDeleted(Task item) {
        if(!item.isSaved()) {
            return;
        }

        item.setDeletionDate(now());
        taskDao.save(item);
    }

    public int markDeleted(List<Task> tasks) {
        for (Task task : tasks) {
            markDeleted(task);
        }
        return tasks.size();
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
        return markDeleted(completed);
    }
}
