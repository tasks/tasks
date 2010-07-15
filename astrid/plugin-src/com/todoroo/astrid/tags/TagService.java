package com.todoroo.astrid.tags;

import java.util.ArrayList;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class TagService {

    // --- public constants

    /** Metadata key for tag data */
    public static final String KEY = "tags";

    /** Property for reading tag values */
    public static final StringProperty TAG = Metadata.VALUE1;

    // --- implementation details

    @Autowired
    private MetadataDao metadataDao;

    public TagService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();
    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(TAG);
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
                            MetadataCriteria.withKey(KEY), TAG.eq(tag),
                            TaskCriteria.isActive()));
        }
    }

    public QueryTemplate untaggedTemplate() {
        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(MetadataCriteria.withKey(KEY)))),
                TaskCriteria.isActive(),
                TaskCriteria.isVisible(DateUtilities.now())));
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param taskId
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order) {
        Query query = Query.select(TAG, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(Criterion.and(TaskCriteria.isActive(), MetadataCriteria.withKey(KEY))).
            orderBy(order).groupBy(TAG);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            Tag[] array = new Tag[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = new Tag();
                array[i].tag = cursor.get(TAG);
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
     * @return cursor. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getTags(long taskId) {
        Query query = Query.select(TAG).where(Criterion.and(MetadataCriteria.withKey(KEY),
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
        try {
            int length = tags.getCount();
            Metadata metadata = new Metadata();
            for (int i = 0; i < length; i++) {
                tags.moveToNext();
                metadata.readFromCursor(tags);
                tagBuilder.append(metadata.getValue(TAG));
                if (i < length - 1)
                    tagBuilder.append(", ");
            }
        } finally {
            tags.close();
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
            metadata.clearValue(Metadata.ID);
            metadata.setValue(TAG, tag.trim());
            metadataDao.createNew(metadata);
        }
    }
}
