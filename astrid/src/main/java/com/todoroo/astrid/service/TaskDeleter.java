package com.todoroo.astrid.service;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;

import javax.inject.Inject;

public class TaskDeleter {

    private final TaskService taskService;
    private final GCalHelper gcalHelper;
    private final TaskDao taskDao;

    @Inject
    public TaskDeleter(TaskService taskService, GCalHelper gcalHelper, TaskDao taskDao) {
        this.taskService = taskService;
        this.gcalHelper = gcalHelper;
        this.taskDao = taskDao;
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    public void deleteTasksWithEmptyTitles() {
        TodorooCursor<Task> cursor = taskDao.query(
                Query.select(Task.ID).where(TaskDao.TaskCriteria.hasNoTitle()));
        try {
            if(cursor.getCount() == 0) {
                return;
            }

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                taskDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    public int purgeDeletedTasks() {
        return deleteWhere(Task.DELETION_DATE.gt(0));
    }

    /**
     * Delete all tasks matching a given criterion
     */
    public int deleteWhere(Criterion criteria) {
        return taskDao.deleteWhere(criteria);
    }

    /**
     * Permanently delete the given task.
     */
    public void purge(long taskId) {
        taskDao.delete(taskId);
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
            gcalHelper.deleteTaskEvent(item);
            item.setDeletionDate(DateUtilities.now());
            taskService.save(item);
        }
    }
}
