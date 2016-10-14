/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.injection.ApplicationScope;

import javax.inject.Inject;


/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@ApplicationScope
public class TaskService {

    private final TaskDao taskDao;

    @Inject
    public TaskService(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    // --- service layer

    /**
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(long id, Property<?>... properties) {
        return taskDao.fetch(id, properties);
    }

    /**
     * Mark the given task as completed and save it.
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setCompletionDate(DateUtilities.now());
        } else {
            item.setCompletionDate(0L);
        }

        save(item);
    }

    /**
     * Create or save the given action item
     */
    public void save(Task item) {
        taskDao.save(item);
    }

    /**
     * Fetch tasks for the given filter
     * @param constraint text constraint, or null
     */
    public TodorooCursor<Task> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null) {
            whereConstraint = Functions.upper(Task.TITLE).like("%" +
                    constraint.toString().toUpperCase() + "%");
        }

        if(queryTemplate == null) {
            if(whereConstraint == null) {
                return taskDao.query(Query.selectDistinct(properties));
            } else {
                return taskDao.query(Query.selectDistinct(properties).where(whereConstraint));
            }
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE")) {
                sql = queryTemplate + " WHERE " + whereConstraint;
            } else {
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
            }
        } else {
            sql = queryTemplate;
        }

        sql = PermaSql.replacePlaceholders(sql);

        return taskDao.query(Query.select(properties).withQueryTemplate(sql));
    }
}
