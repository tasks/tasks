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
import javax.inject.Singleton;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@Singleton
public class TaskListMetadataDao extends RemoteModelDao<TaskListMetadata> {

    @Inject
    public TaskListMetadataDao(Database database) {
        super(TaskListMetadata.class);
        setDatabase(database);
    }

    public TaskListMetadata fetchByTagId(String tagUuid, Property<?>... properties) {
        return getFirst(Query.select(properties).where(Criterion.or(TaskListMetadata.TAG_UUID.eq(tagUuid),
                TaskListMetadata.FILTER.eq(tagUuid))));
    }
}

