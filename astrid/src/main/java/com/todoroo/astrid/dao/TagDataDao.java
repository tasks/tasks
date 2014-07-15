/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@Singleton
public class TagDataDao extends RemoteModelDao<TagData> {

    @Inject
    public TagDataDao(Database database) {
        super(TagData.class);
        setDatabase(database);
    }

    /**
     * Fetch a model object by UUID
     */
    public TagData fetch(String uuid, Property<?>... properties) {
        TodorooCursor<TagData> cursor = fetchItem(uuid, properties);
        return returnFetchResult(cursor);
    }

    /**
     * Returns cursor to object corresponding to the given identifier
     *
     * @param properties
     *            properties to read
     */
    private TodorooCursor<TagData> fetchItem(String uuid, Property<?>... properties) {
        TodorooCursor<TagData> cursor = query(
                Query.select(properties).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
        cursor.moveToFirst();
        return new TodorooCursor<>(cursor, properties);
    }
}

