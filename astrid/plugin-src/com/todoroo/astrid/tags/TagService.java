package com.todoroo.astrid.tags;

import java.util.ArrayList;

import android.content.Context;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.MetadataService;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class TagService {

    /**
     * Metadata key for tag data
     */
    public static final String KEY = "tags-tag";

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private MetadataService metadataService;

    public TagService(@SuppressWarnings("unused") Context context) {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();

    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(Metadata.VALUE);
    public static final Order GROUPED_TAGS_BY_SIZE = Order.desc(COUNT);

    /**
     * Helper class for returning a tag/task count pair
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public final class Tag {
        public String tag;
        int count;

        @Override
        public String toString() {
            return tag;
        }

        /**
         * Return SQL selector query for getting tasks with a given tag
         *
         * @param tag
         * @return
         */
        public QueryTemplate queryTemplate() {
            return new QueryTemplate().join(Join.inner(Metadata.TABLE,
                    Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                            MetadataCriteria.withKey(KEY), Metadata.VALUE.eq(tag)));
        }
    }

    public QueryTemplate untaggedTemplate() {
        return new QueryTemplate().join(Join.inner(Metadata.TABLE,
                Task.ID.eq(Metadata.TASK))).where(Criterion.and(
                        TaskCriteria.isActive(), MetadataCriteria.withKey(KEY),
                        Metadata.VALUE.eq(null)));
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param taskId
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order) {
        TodorooCursor<Metadata> cursor = metadataService.fetchWithCount(
                COUNT, MetadataCriteria.withKey(KEY), order);
        try {
            Tag[] array = new Tag[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = new Tag();
                array[i].tag = cursor.get(Metadata.VALUE);
                array[i].count = cursor.get(COUNT);
            }
            return array;
        } finally {
            cursor.close();
        }
    }

    /**
     * Return tags on the given task
     *
     * @param taskId
     * @return empty array if no tags, otherwise array
     */
    public TodorooCursor<Metadata> getTags(long taskId) {
        Query query = Query.select(Metadata.VALUE).where(Criterion.and(MetadataCriteria.withKey(KEY),
                MetadataCriteria.byTask(taskId)));
        return metadataDao.query(query);
    }

    /**
     * Return tags as a comma-separated list of strings
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId) {
        StringBuilder tagBuilder = new StringBuilder();
        TodorooCursor<Metadata> tags = getTags(taskId);
        int length = tags.getCount();
        Metadata metadata = new Metadata();
        for (int i = 0; i < length; i++) {
            tags.moveToNext();
            metadata.readFromCursor(tags);
            tagBuilder.append(metadata.getValue(Metadata.VALUE));
            if (i < length - 1)
                tagBuilder.append(", ");
        }
        return tagBuilder.toString();
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public void synchronizeTags(long taskId, ArrayList<String> tags) {
        metadataDao.deleteWhere(Criterion.and(MetadataCriteria.byTask(taskId),
                MetadataCriteria.withKey(KEY)));

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, KEY);
        metadata.setValue(Metadata.TASK, taskId);
        for(String tag : tags) {
            metadata.setValue(Metadata.VALUE, tag.trim());
            metadataDao.createItem(metadata);
        }
    }
}
