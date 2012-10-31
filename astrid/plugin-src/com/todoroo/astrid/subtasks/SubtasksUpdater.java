/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;

public class SubtasksUpdater extends NewOrderedListUpdater<TagData> {

    @Autowired TagDataService tagDataService;
    @Autowired TaskService taskService;

    public static final String ACTIVE_TASKS_ORDER = "active_tasks_order"; //$NON-NLS-1$

    @Override
    protected void initialize(TagData list, Filter filter) {
        super.initialize(list, filter);
        applySubtasksToFilter(filter);
    }

    @SuppressWarnings("nls")
    public void applySubtasksToFilter(Filter filter) {
        String query = filter.getSqlQuery();

        query = query.replaceAll("ORDER BY .*", "");
        query = query + String.format(" ORDER BY %s, %s, %s, %s)",
                Task.DELETION_DATE, Task.COMPLETION_DATE,
                getOrderString(), Task.CREATION_DATE);
        query = query.replace(TaskCriteria.isVisible().toString(),
                Criterion.all.toString());

        filter.setFilterQueryOverride(query);
    }

    @Override
    protected String getSerializedTree(TagData list) {
        if (list == null) {
            String order = Preferences.getStringValue(ACTIVE_TASKS_ORDER);
            if (order == null)
                order = "{}"; //$NON-NLS-1$
            return order;
        }
        return list.getValue(TagData.NAME);
    }

    @Override
    protected void writeSerialization(TagData list, String serialized) {
        if (list == null)
            Preferences.setString(ACTIVE_TASKS_ORDER, serialized);
        else {
            list.setValue(TagData.NAME, serialized);
            tagDataService.save(list);
        }
    }

}


