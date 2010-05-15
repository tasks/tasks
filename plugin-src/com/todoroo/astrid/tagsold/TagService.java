/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.astrid.tagsold;

import android.database.SQLException;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.sql.Join;
import com.todoroo.andlib.data.sql.Query;
import com.todoroo.astrid.model.Task;

/**
 * Service layer for tags plugin
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagService {

    private GenericDao<Tag> tagDao;
    private GenericDao<TagToTaskMapping> tagToTaskDao;

    public TagService() {
        TagsDatabase tagDatabase = new TagsDatabase();
        tagDao = new GenericDao<Tag>(Tag.class, tagDatabase);
        tagToTaskDao = new GenericDao<TagToTaskMapping>(TagToTaskMapping.class,
                tagDatabase);
    }

    // --- tag batch operations

    /** Get a list of all tags */
    public TodorooCursor<Tag> getAllTags(Property<?>... properties) {
        return tagDao.query(Query.select(properties));
    }

    /** Get a list of tag identifiers for the given task */
    public TodorooCursor<Tag> getTaskTags(Task task, Property<?>... properties) throws SQLException {
        Query query = Query.select(properties).join(Join.inner(TagToTaskMapping.TABLE,
                Tag.ID.eq(TagToTaskMapping.TAG))).where(TagToTaskMapping.TASK.eq(task.getId()));
        return tagDao.query(query);
    }

}
