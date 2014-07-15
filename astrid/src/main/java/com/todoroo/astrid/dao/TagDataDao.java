/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property;
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
        return getFirst(Query.select(properties).where(RemoteModel.UUID_PROPERTY.eq(uuid)));
    }

    public TagData getTagByName(String name, Property<?>... properties) {
        return getFirst(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(name)));
    }
}

