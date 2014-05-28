package com.todoroo.astrid.service;

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
