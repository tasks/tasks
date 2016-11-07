package com.todoroo.astrid.service;

import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.todoroo.andlib.sql.Criterion.all;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.isVisible;
import static com.todoroo.astrid.dao.TaskDao.TaskCriteria.notCompleted;

public class TaskDeleter {

    private final TaskDao taskDao;

    @Inject
    public TaskDeleter(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    void deleteTasksWithEmptyTitles(Long suppress) {
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
            Task template = new Task();
            template.setId(item.getId());
            template.setDeletionDate(DateUtilities.now());
            taskDao.save(template);
        }
    }

    public int delete(List<Task> tasks) {
        return markDeleted(tasks);
    }

    public void undelete(List<Task> tasks) {
        for (Task task : tasks) {
            Task template = new Task();
            template.setId(task.getId());
            template.setDeletionDate(0L);
            taskDao.save(template);
        }
    }

    private int markDeleted(List<Task> tasks) {
        for (Task task : tasks) {
            delete(task);
        }
        return tasks.size();
    }

    public int clearCompleted(Filter filter) {
        List<Task> completed = new ArrayList<>();
        String query = filter.getSqlQuery()
                .replace(isVisible().toString(), all.toString())
                .replace(notCompleted().toString(), all.toString());
        taskDao.fetchFiltered(query, Task.ID, Task.COMPLETION_DATE).forEach(task -> {
            if (task.isCompleted()) {
                completed.add(task);
            }
        });
        return markDeleted(completed);
    }
}
