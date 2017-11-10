/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.TagData;

import org.tasks.injection.ApplicationScope;

import java.util.List;

import javax.inject.Inject;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 */
@ApplicationScope
public class TagDataDao {

    private final RemoteModelDao<TagData> dao;

    @Inject
    public TagDataDao(Database database) {
        dao = new RemoteModelDao<>(database, TagData.class);
    }

    public TagData getTagByName(String name) {
        return dao.getFirst(Query.select(TagData.PROPERTIES).where(TagData.NAME.eqCaseInsensitive(name)));
    }

    public List<TagData> allTags() {
        // TODO: does this need to be ordered?
        return dao.toList(Query.select(TagData.PROPERTIES)
                .where(TagData.DELETION_DATE.eq(0))
                .orderBy(Order.asc(TagData.ID)));
    }

    public TagData getByUuid(String uuid) {
        return dao.getFirst(Query.select(TagData.PROPERTIES).where(TagData.UUID.eq(uuid)));
    }

    public List<TagData> tagDataOrderedByName() {
        return dao.toList(Query.select(TagData.PROPERTIES).where(Criterion.and(
                TagData.DELETION_DATE.eq(0),
                TagData.NAME.isNotNull()))
                .orderBy(Order.asc(Functions.upper(TagData.NAME))));
    }

    public void persist(TagData tagData) {
        dao.persist(tagData);
    }

    public void update(Criterion where, TagData template) {
        dao.update(where, template);
    }

    public void delete(long id) {
        dao.delete(id);
    }

    public void createNew(TagData tag) {
        dao.createNew(tag);
    }
}

