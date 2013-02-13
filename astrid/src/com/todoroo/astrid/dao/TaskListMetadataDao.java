/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
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
    protected boolean shouldRecordOutstandingEntry(String columnName) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_TASK_LIST_METADATA, columnName);
    }

}

