package com.todoroo.astrid.service;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.calendars.CalendarEventProvider;

import javax.inject.Inject;

public class TaskDeleter {

    private final TaskDao taskDao;
    private final CalendarEventProvider calendarEventProvider;

    @Inject
    public TaskDeleter(TaskDao taskDao, CalendarEventProvider calendarEventProvider) {
        this.taskDao = taskDao;
        this.calendarEventProvider = calendarEventProvider;
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    public void deleteTasksWithEmptyTitles(Long suppress) {
        Query query = Query.select(Task.ID).where(TaskDao.TaskCriteria.hasNoTitle());
        taskDao.forEach(query, task -> {
            long id = task.getId();
            if (suppress == null || suppress != id) {
                taskDao.delete(id);
            }
        });
    }

    public void delete(Task item) {
        if(!item.isSaved()) {
            return;
        }

        if(item.containsValue(Task.TITLE) && item.getTitle().length() == 0) {
            taskDao.delete(item.getId());
            item.setId(Task.NO_ID);
        } else {
            long id = item.getId();
            item.clear();
            item.setId(id);
            calendarEventProvider.deleteEvent(item);
            item.setDeletionDate(DateUtilities.now());
            taskDao.save(item);
        }
    }
}
