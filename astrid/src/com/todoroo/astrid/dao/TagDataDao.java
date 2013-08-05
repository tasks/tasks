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

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TagDataDao extends RemoteModelDao<TagData> {

    @Autowired
    Database database;

    public TagDataDao() {
        super(TagData.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    protected boolean shouldRecordOutstandingEntry(String columnName, Object value) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_TAGS, columnName);
    }
}

