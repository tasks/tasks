/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TaskListMetadata;

import javax.inject.Inject;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TaskListMetadataDao {

    private final RemoteModelDao<TaskListMetadata> dao;

    @Inject
    public TaskListMetadataDao(Database database) {
        dao = new RemoteModelDao<>(database, TaskListMetadata.class);
    }

    public TaskListMetadata fetchByTagId(String tagUuid, Property<?>... properties) {
        return dao.getFirst(Query.select(properties).where(Criterion.or(TaskListMetadata.TAG_UUID.eq(tagUuid),
                TaskListMetadata.FILTER.eq(tagUuid))));
    }

    public void createNew(TaskListMetadata taskListMetadata) {
        dao.createNew(taskListMetadata);
    }

    public void saveExisting(TaskListMetadata list) {
        dao.saveExisting(list);
    }
}

