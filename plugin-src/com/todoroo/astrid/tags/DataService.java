package com.todoroo.astrid.tags;

import java.util.ArrayList;

import android.content.Context;
import android.database.DatabaseUtils;

import com.thoughtworks.sql.Criterion;
import com.thoughtworks.sql.Order;
import com.thoughtworks.sql.Query;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridContentProvider.AstridTask;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.service.MetadataService;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class DataService {

    /**
     * Metadata key for tag data
     */
    public static final String KEY = "tags-tag";

    @Autowired
    private Database database;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private MetadataService metadataService;

    public DataService(@SuppressWarnings("unused") Context context) {
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
    public class Tag {
        String tag;
        int count;

        @Override
        public String toString() {
            return tag;
        }
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param taskId
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order) {
        TodorooCursor<Metadata> cursor = metadataService.fetchWithCount(
                COUNT, MetadataCriteria.withKey(KEY), order, true);
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
            metadata.readFromCursor(tags, PROP)
            tagBuilder.append(]);
            if (i < tags.length - 1)
                tagBuilder.append(", ");
        }
        return tagBuilder.toString();
    }

    private static final String query = String.format("INNER JOIN %s ON %s = "
            + "%s WHERE %s = 0 AND %s = '%s' AND %s = ",
            AstridApiConstants.METADATA_TABLE,
            AstridTask.ID, Metadata.TASK,
            AstridTask.COMPLETION_DATE,
            Metadata.KEY, KEY,
            Metadata.VALUE);

    /**
     * Return SQL selector query for getting tasks with a given tag
     *
     * @param tag
     * @return
     */
    public String getQuery(String tag) {
        return query + String.format("%s", DatabaseUtils.sqlEscapeString(tag));
    }

    private static final String newTaskSql = String.format(
            "INSERT INTO %s (%s, %s, %s) " + "VALUES ($ID,'%s',",
            AstridApiConstants.METADATA_TABLE,
            Metadata.TASK.name,
            Metadata.KEY.name,
            Metadata.VALUE.name,
            KEY);

    /**
     * Return SQL new task creator query
     * @param tag
     */
    public String getNewTaskSql(String tag) {
        return newTaskSql + String.format("%s)", DatabaseUtils.sqlEscapeString(tag));
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public void synchronizeTags(long taskId, ArrayList<String> tags) {
        metadataDao.deleteWhere(database, MetadataSql.byTask(taskId) + " AND " +
                MetadataSql.withKey(KEY));

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, KEY);
        metadata.setValue(Metadata.TASK, taskId);
        for(String tag : tags) {
            metadata.setValue(Metadata.VALUE, tag.trim());
            metadataDao.save(database, metadata);
            metadata.clearValue(Metadata.ID);
        }
    }
}
