/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.text.TextUtils;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public final class TagService {

    private final MetadataDao metadataDao;
    private final TagDataDao tagDataDao;

    @Inject
    public TagService(MetadataDao metadataDao, TagDataDao tagDataDao) {
        this.metadataDao = metadataDao;
        this.tagDataDao = tagDataDao;
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();
    public static final Order GROUPED_TAGS_BY_SIZE = Order.desc(COUNT);

    /**
     * Helper class for returning a tag/task count pair
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    // TODO: get rid of this
    public static final class Tag {
        public String tag;
        public String uuid;

        public Tag(TagData tagData) {
            tag = tagData.getName();
            uuid = tagData.getUUID();
        }

        @Override
        public String toString() {
            return tag;
        }

        /**
         * Return SQL selector query for getting tasks with a given tagData
         */
        public QueryTemplate queryTemplate(Criterion criterion) {
            Criterion fullCriterion = Criterion.and(
                    Field.field("mtags." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY),
                    Field.field("mtags." + TaskToTagMetadata.TAG_UUID.name).eq(uuid),
                    Field.field("mtags." + Metadata.DELETION_DATE.name).eq(0),
                    criterion);
            return new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"), Task.UUID.eq(Field.field("mtags." + TaskToTagMetadata.TASK_UUID.name))))
                    .where(fullCriterion);
        }
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param order ordering
     * @param activeStatus criterion for specifying completed or uncompleted
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order, Criterion activeStatus) {
        Criterion criterion = Criterion.and(activeStatus, MetadataCriteria.withKey(TaskToTagMetadata.KEY));
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(criterion).
            orderBy(order).groupBy(TaskToTagMetadata.TAG_NAME);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            ArrayList<Tag> array = new ArrayList<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                Tag tag = tagFromUUID(cursor.get(TaskToTagMetadata.TAG_UUID));
                if (tag != null) {
                    array.add(tag);
                }
            }
            return array.toArray(new Tag[array.size()]);
        } finally {
            cursor.close();
        }
    }

    private Tag tagFromUUID(String uuid) {
        TagData tagData = tagDataDao.getByUuid(uuid, TagData.PROPERTIES);
        return tagData == null ? null : new Tag(tagData);
    }

    /**
     * Return tags on the given task
     * @return cursor. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getTags(long taskId) {
        Criterion criterion = Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                    Metadata.DELETION_DATE.eq(0),
                    MetadataCriteria.byTask(taskId));
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID).where(criterion).orderBy(Order.asc(Functions.upper(TaskToTagMetadata.TAG_NAME)));
        return metadataDao.query(query);
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     */
    public List<Tag> getTagList() {
        final List<Tag> tagList = new ArrayList<>();
        tagDataDao.tagDataOrderedByName(new Callback<TagData>() {
            @Override
            public void apply(TagData tagData) {
                if (!TextUtils.isEmpty(tagData.getName())) {
                    tagList.add(new Tag(tagData));
                }
            }
        });
        return tagList;
    }

    /**
     * If a tag already exists in the database that case insensitively matches the
     * given tag, return that. Otherwise, return the argument
     */
    public String getTagWithCase(String tag) {
        String tagWithCase = tag;
        TodorooCursor<Metadata> tagMetadata = metadataDao.query(Query.select(TaskToTagMetadata.TAG_NAME).where(TagService.tagEqIgnoreCase(tag, Criterion.all)).limit(1));
        try {
            if (tagMetadata.getCount() > 0) {
                tagMetadata.moveToFirst();
                Metadata tagMatch = new Metadata(tagMetadata);
                tagWithCase = tagMatch.getValue(TaskToTagMetadata.TAG_NAME);
            } else {
                TagData tagData = tagDataDao.getTagByName(tag, TagData.NAME);
                if (tagData != null) {
                    tagWithCase = tagData.getName();
                }
            }
        } finally {
            tagMetadata.close();
        }
        return tagWithCase;
    }

    private static Criterion tagEqIgnoreCase(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_NAME.eqCaseInsensitive(tag),
                additionalCriterion);
    }

    public int rename(String uuid, String newName) {
        TagData template = new TagData();
        template.setName(newName);
        tagDataDao.update(TagData.UUID.eq(uuid), template);

        Metadata metadataTemplate = new Metadata();
        metadataTemplate.setValue(TaskToTagMetadata.TAG_NAME, newName);

        return metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)), metadataTemplate);
    }
}
