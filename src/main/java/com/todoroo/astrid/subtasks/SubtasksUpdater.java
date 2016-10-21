/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskListMetadata;

public abstract class SubtasksUpdater extends AstridOrderedListUpdater {

    public static final String ACTIVE_TASKS_ORDER = "active_tasks_order"; //$NON-NLS-1$
    public static final String TODAY_TASKS_ORDER = "today_tasks_order"; //$NON-NLS-1$

    SubtasksUpdater(TaskDao taskDao) {
        super(taskDao);
    }

    @Override
    public void initialize(TaskListMetadata list, Filter filter) {
        super.initialize(list, filter);
        applyToFilter(filter);
    }

    @Override
    protected void applyToFilter(Filter filter) {
        String query = filter.getSqlQuery();

        query = query.replaceAll("ORDER BY .*", "");
        query = query + String.format(" ORDER BY %s, %s, %s",
                Task.DELETION_DATE, getOrderString(), Task.CREATION_DATE);
        query = query.replace(TaskCriteria.isVisible().toString(),
                Criterion.all.toString());

        filter.setFilterQueryOverride(query);
    }

}


