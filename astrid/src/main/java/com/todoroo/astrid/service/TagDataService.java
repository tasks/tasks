/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.TagData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service layer for {@link TagData}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class TagDataService {

    private final TagDataDao tagDataDao;

    @Inject
    public TagDataService(TagDataDao tagDataDao) {
        this.tagDataDao = tagDataDao;
    }

    // --- service layer

    /**
     * Query underlying database
     */
    public TodorooCursor<TagData> query(Query query) {
        return tagDataDao.query(query);
    }

    /**
     * Save a single piece of metadata
     */
    public void save(TagData tagData) {
        tagDataDao.persist(tagData);
    }

    /**
     * @return item, or null if it doesn't exist
     */
    public TagData fetchById(long id, Property<?>... properties) {
        return tagDataDao.fetch(id, properties);
    }

    /**
     * Find a tag by name
     * @return null if doesn't exist
     */
    public TagData getTagByName(String name, Property<?>... properties) {
        TodorooCursor<TagData> cursor = tagDataDao.query(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(name)));
        try {
            if(cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return new TagData(cursor);
        } finally {
            cursor.close();
        }
    }
}
