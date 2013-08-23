/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;

public abstract class SubtasksUpdater<T> extends AstridOrderedListUpdater<T> {

    @Autowired TaskListMetadataDao taskListMetadataDao;
    @Autowired TaskService taskService;
    @Autowired ActFmSyncService actFmSyncService;

    public static final String ACTIVE_TASKS_ORDER = "active_tasks_order"; //$NON-NLS-1$
    public static final String TODAY_TASKS_ORDER = "today_tasks_order"; //$NON-NLS-1$

    @Override
    public void initialize(T list, Filter filter) {
        super.initialize(list, filter);
        applyToFilter(filter);
    }

    @Override
    @SuppressWarnings("nls")
    public void applyToFilter(Filter filter) {
        String query = filter.getSqlQuery();

        query = query.replaceAll("ORDER BY .*", "");
        query = query + String.format(" ORDER BY %s, %s, %s, %s",
                Task.DELETION_DATE, Task.COMPLETION_DATE,
                getOrderString(), Task.CREATION_DATE);
        query = query.replace(TaskCriteria.isVisible().toString(),
                Criterion.all.toString());

        filter.setFilterQueryOverride(query);
    }

}


