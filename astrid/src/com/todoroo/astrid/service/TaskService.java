package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
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

    @Autowired
    private MetadataDao metadataDao;

    public TaskService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<Task> query(Query query) {
        return taskDao.query(query);
    }

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
     * @param skipHooks
     *            Whether pre and post hooks should run. This should be set
     *            to true if tasks are created as part of synchronization
     */
    public boolean save(Task item, boolean runHooks) {
        return taskDao.save(item, runHooks);
    }

    /**
     * Clone the given task and all its metadata
     *
     * @param the old task
     * @return the new task
     */
    public Task clone(Task task) {
        Task newTask = fetchById(task.getId(), Task.PROPERTIES);
        newTask.clearValue(Task.ID);
        taskDao.createNew(newTask);
        TodorooCursor<Metadata> cursor = metadataDao.query(
                Query.select(Metadata.PROPERTIES).where(MetadataCriteria.byTask(task.getId())));
        try {
            if(cursor.getCount() > 0) {
                Metadata metadata = new Metadata();
                long newId = newTask.getId();
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    metadata.readFromCursor(cursor);
                    metadata.setValue(Metadata.TASK, newId);
                    metadata.clearValue(Metadata.ID);
                    metadataDao.createNew(metadata);
                }
            }
        } finally {
            cursor.close();
        }
        return newTask;
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
        else if(item.containsValue(Task.TITLE) && item.getValue(Task.TITLE).length() == 0) {
            taskDao.delete(item.getId());
        } else {
            long id = item.getId();
            item.clear();
            item.setId(id);
            item.setValue(Task.DELETION_DATE, DateUtilities.now());
            taskDao.save(item, false);
        }
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

    /**
     * Fetch tasks for the given filter
     * @param properties
     * @param filter
     * @return
     */
    public TodorooCursor<Task> fetchFiltered(Filter filter, Property<?>... properties) {
        if(filter == null || filter.sqlQuery == null)
            return taskDao.query(Query.select(properties));
        else
            return taskDao.query(Query.select(properties).withQueryTemplate(filter.sqlQuery));
    }

    /**
     * Return the default task ordering
     * @return
     */
    @SuppressWarnings("nls")
    public static Order defaultTaskOrder() {
        return Order.asc(Functions.caseStatement(Task.DUE_DATE.eq(0),
                DateUtilities.now() + DateUtilities.ONE_WEEK,
                Task.DUE_DATE) + " + 200000000 * " +
                Task.IMPORTANCE + " + " + Task.COMPLETION_DATE);
    }

    /**
     * @param query
     * @return how many tasks are matched by this query
     */
    public int count(Query query) {
        TodorooCursor<Task> cursor = taskDao.query(query);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }



}
