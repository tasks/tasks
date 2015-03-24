/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.text.TextUtils;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
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
     * Return all tags ordered by given clause
     *
     * @param order ordering
     * @param activeStatus criterion for specifying completed or uncompleted
     * @return empty array if no tags, otherwise array
     */
    public TagData[] getGroupedTags(Order order, Criterion activeStatus) {
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID, COUNT).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(
                        activeStatus,
                        MetadataCriteria.withKey(TaskToTagMetadata.KEY))).
                orderBy(order).groupBy(TaskToTagMetadata.TAG_NAME);
        final List<TagData> array = new ArrayList<>();
        metadataDao.query(query, new Callback<Metadata>() {
            @Override
            public void apply(Metadata metadata) {
                TagData tag = tagFromUUID(metadata.getValue(TaskToTagMetadata.TAG_UUID));
                if (tag != null) {
                    array.add(tag);
                }
            }
        });
        return array.toArray(new TagData[array.size()]);
    }

    private TagData tagFromUUID(String uuid) {
        return tagDataDao.getByUuid(uuid, TagData.PROPERTIES);
    }

    public List<String> getTagNames(long taskId) {
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID).where(
                Criterion.and(
                    MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                    Metadata.DELETION_DATE.eq(0),
                    MetadataCriteria.byTask(taskId)))
                .orderBy(Order.asc(Functions.upper(TaskToTagMetadata.TAG_NAME)));
        final List<String> tagNames = new ArrayList<>();
        metadataDao.query(query,  new Callback<Metadata>() {
            @Override
            public void apply(Metadata entry) {
                tagNames.add(entry.getValue(TaskToTagMetadata.TAG_NAME));
            }
        });
        return tagNames;
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     */
    public List<TagData> getTagList() {
        final List<TagData> tagList = new ArrayList<>();
        tagDataDao.tagDataOrderedByName(new Callback<TagData>() {
            @Override
            public void apply(TagData tagData) {
                if (!TextUtils.isEmpty(tagData.getName())) {
                    tagList.add(tagData);
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
        Metadata tagMetadata = metadataDao.getFirst(Query.select(TaskToTagMetadata.TAG_NAME).where(tagEqIgnoreCase(tag, Criterion.all)).limit(1));
        if (tagMetadata != null) {
            return tagMetadata.getValue(TaskToTagMetadata.TAG_NAME);
        }

        TagData tagData = tagDataDao.getTagByName(tag, TagData.NAME);
        if (tagData != null) {
            return tagData.getName();
        }
        return tag;
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
