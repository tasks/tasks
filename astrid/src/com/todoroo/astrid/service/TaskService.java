package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Task;

/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskService {

    @Autowired
    private TaskDao taskDao;

    public TaskService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     *
     * @param properties
     * @param id id
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(long id, Property<?>... properties) {
        return taskDao.fetch(id, properties);
    }

    /**
     * Mark the given task as completed and save it.
     *
     * @param item
     */
    public void setComplete(Task item, boolean completed) {
        if(completed)
            item.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        else
            item.setValue(Task.COMPLETION_DATE, 0L);

        taskDao.save(item, false);
    }

    /**
     * Create or save the given action item
     *
     * @param item
     * @param isDuringSync
     *            Whether this operation is invoked from synchronizer. This
     *            determines which pre and post save hooks get run
     */
    public boolean save(Task item, boolean isDuringSync) {
        return taskDao.save(item, isDuringSync);
    }

    /**
     * Delete the given task. Instead of deleting from the database, we set
     * the deleted flag.
     *
     * @param model
     */
    public void delete(Task item) {
        if(!item.isSaved())
            return;
        item.clear();
        item.setValue(Task.DELETION_DATE, DateUtilities.now());
        taskDao.save(item, false);
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Task> cursor = taskDao.query(
                Query.select(Task.ID).where(TaskCriteria.hasNoTitle()));
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                taskDao.delete(id);
            }
        } finally {
            cursor.close();
        }
    }

    public TodorooCursor<Task> fetchFiltered(Property<?>[] properties,
            Filter filter) {
        if(filter == null || filter.sqlQuery == null)
            return taskDao.query(Query.select(properties));
        else
            return taskDao.query(Query.select(properties).withQueryTemplate(filter.sqlQuery));
    }

}
