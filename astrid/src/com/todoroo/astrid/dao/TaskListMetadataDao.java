/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TaskListMetadata;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskListMetadataDao extends RemoteModelDao<TaskListMetadata> {

    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TaskListMetadataDao() {
        super(TaskListMetadata.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    protected boolean shouldRecordOutstandingEntry(String columnName, Object value) {
        if (TaskListMetadata.FILTER.name.equals(columnName) || TaskListMetadata.TAG_UUID.name.equals(columnName))
            return !RemoteModel.isUuidEmpty(value.toString());

        if (TaskListMetadata.TASK_IDS.name.equals(columnName))
            return !TaskListMetadata.taskIdsIsEmpty(value.toString());

        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_TASK_LIST_METADATA, columnName);
    }

    public TaskListMetadata fetchByTagId(String tagUuid, Property<?>...properties) {
        TodorooCursor<TaskListMetadata> taskListMetadata = query(Query.select(properties).where(Criterion.or(TaskListMetadata.TAG_UUID.eq(tagUuid),
                TaskListMetadata.FILTER.eq(tagUuid))));
        taskListMetadata.moveToFirst();
        return returnFetchResult(taskListMetadata);
    }

}

