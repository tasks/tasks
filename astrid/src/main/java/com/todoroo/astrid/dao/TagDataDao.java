/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

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
}

