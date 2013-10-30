/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.database.Cursor;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.adapter.UpdateAdapter;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.UserActivity;
import com.todoroo.astrid.tags.TaskToTagMetadata;

/**
 * Service layer for {@link TagData}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataService {

    @Autowired TagDataDao tagDataDao;
    @Autowired UserActivityDao userActivityDao;

    public TagDataService() {
        DependencyInjectionService.getInstance().inject(this);
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

    private static Query queryForTagData(TagData tagData, Criterion extraCriterion, Property<?>[] activityProperties) {
        Criterion criteria;
        if (tagData == null) {
            criteria = UserActivity.DELETED_AT.eq(0);
        } else {
            criteria = Criterion.and(UserActivity.DELETED_AT.eq(0), Criterion.or(
                    Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TAG_COMMENT), UserActivity.TARGET_ID.eq(tagData.getUuid())),
                    Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT),
                            UserActivity.TARGET_ID.in(Query.select(TaskToTagMetadata.TASK_UUID)
                                    .from(Metadata.TABLE).where(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(tagData.getUuid())))))));
        }

        if (extraCriterion != null) {
            criteria = Criterion.and(criteria, extraCriterion);
        }

        Query result = Query.select(AndroidUtilities.addToArray(Property.class, activityProperties)).where(criteria);
        return result;
    }

    public Cursor getActivityForTagData(TagData tagData, Criterion extraCriterion) {
        Query activityQuery = queryForTagData(tagData, extraCriterion, UpdateAdapter.USER_ACTIVITY_PROPERTIES)
                .from(UserActivity.TABLE);

        Query resultQuery = activityQuery.orderBy(Order.desc("1")); //$NON-NLS-1$

        return userActivityDao.query(resultQuery);
    }
}
