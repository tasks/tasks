package com.todoroo.astrid.service;

import com.thoughtworks.sql.Query;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.Database;
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
    private Database database;

    @Autowired
    private TaskDao taskDao;

    public TaskService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- property list

    /**
     * @return property list containing just task id's
     */
    public static Property<?>[] idProperties() {
        return new Property<?>[] { Task.ID };
    }

    // --- service layer

    /**
     *
     * @param properties
     * @param id id
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(Property<?>[] properties,
            long id) {
        return taskDao.fetch(database, properties, id);
    }

    /**
     * Mark the given action item as completed and save it.
     *
     * @param item
     */
    public void setComplete(Task item, boolean completed) {
        if(completed)
            item.setValue(Task.COMPLETION_DATE, DateUtilities.now());
        else
            item.setValue(Task.COMPLETION_DATE, 0);

        taskDao.save(database, item, false);
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
        return taskDao.save(database, item, isDuringSync);
    }

    /**
     * Delete the given action item
     *
     * @param model
     */
    public void delete(long itemId) {
        taskDao.delete(database, itemId);
    }

    /**
     * Clean up tasks. Typically called on startup
     */
    public void cleanup() {
        TodorooCursor<Task> cursor = taskDao.query(database,
                Query.select(idProperties()).where(TaskCriteria.hasNoTitle()));
        try {
            if(cursor.getCount() == 0)
                return;

            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                taskDao.delete(database, id);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Invoke the sql in the filter for sqlforNewTasks
     *
     * @param filter
     * @param task
     */
    public void invokeSqlForNewTask(Filter filter, Task task) {
        String sql = filter.sqlForNewTasks.replace("$ID", //$NON-NLS-1$
                Long.toString(task.getId()));
        database.getDatabase().execSQL(sql);
    }
}
