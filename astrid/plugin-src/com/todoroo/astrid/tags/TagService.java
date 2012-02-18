package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import android.text.TextUtils;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.CountProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
import com.todoroo.astrid.data.Update;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Flags;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class TagService {

    // --- public constants

    /** Metadata key for tag data */
    public static final String KEY = "tags-tag";

    /** Property for reading tag values */
    public static final StringProperty TAG = Metadata.VALUE1;

    /** Property for astrid.com remote id */
    public static final LongProperty REMOTE_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE2.name);

    // --- singleton

    private static TagService instance = null;

    private static int[] default_tag_images = new int[] {
        R.drawable.default_list_0,
        R.drawable.default_list_1,
        R.drawable.default_list_2,
        R.drawable.default_list_3
    };

    public static synchronized TagService getInstance() {
        if(instance == null)
            instance = new TagService();
        return instance;
    }

    // --- implementation details

    @Autowired MetadataDao metadataDao;

    @Autowired TaskService taskService;

    @Autowired TagDataService tagDataService;

    public TagService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();
    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(Functions.upper(TAG));
    public static final Order GROUPED_TAGS_BY_SIZE = Order.desc(COUNT);

    /**
     * Helper class for returning a tag/task count pair
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static final class Tag {
        public String tag;
        public int count;
        public long remoteId;
        public String image;
        public String updateText;

        public Tag(String tag, int count, long remoteId) {
            this.tag = tag;
            this.count = count;
            this.remoteId = remoteId;
        }

        public Tag(TagData tagData) {
            tag = tagData.getValue(TagData.NAME);
            count = tagData.getValue(TagData.TASK_COUNT);
            remoteId = tagData.getValue(TagData.REMOTE_ID);
            image = tagData.getValue(TagData.PICTURE);
        }

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
        public QueryTemplate queryTemplate(Criterion criterion) {
            return new QueryTemplate().join(Join.inner(Metadata.TABLE,
                    Task.ID.eq(Metadata.TASK))).where(tagEqIgnoreCase(tag, criterion));
        }

    }

    public static Criterion tagEq(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(KEY), TAG.eq(tag),
                additionalCriterion);
    }

    public static Criterion tagEqIgnoreCase(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(KEY), TAG.eqCaseInsensitive(tag),
                additionalCriterion);
    }

    public static QueryTemplate untaggedTemplate() {
        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(MetadataCriteria.withKey(KEY)))),
                TaskCriteria.isActive(),
                TaskApiDao.TaskCriteria.ownedByMe(),
                TaskCriteria.isVisible()));
    }

    /**
     * Return all tags ordered by given clause
     *
     * @param order ordering
     * @param activeStatus criterion for specifying completed or uncompleted
     * @return empty array if no tags, otherwise array
     */
    public Tag[] getGroupedTags(Order order, Criterion activeStatus) {
        Query query = Query.select(TAG, REMOTE_ID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(Criterion.and(activeStatus, MetadataCriteria.withKey(KEY))).
            orderBy(order).groupBy(TAG);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            Tag[] array = new Tag[cursor.getCount()];
            for (int i = 0; i < array.length; i++) {
                cursor.moveToNext();
                array[i] = new Tag(cursor.get(TAG), cursor.get(COUNT), cursor.get(REMOTE_ID));
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
        Query query = Query.select(TAG, REMOTE_ID).where(Criterion.and(MetadataCriteria.withKey(KEY),
                MetadataCriteria.byTask(taskId))).orderBy(Order.asc(Functions.upper(TAG)));
        return metadataDao.query(query);
    }

    /**
     * Return tags as a comma-separated list of strings
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId) {
        return getTagsAsString(taskId, ", ");
    }

    /**
     * Return tags as a list of strings separated by given separator
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId, String separator) {
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
                    tagBuilder.append(separator);
            }
        } finally {
            tags.close();
        }
        return tagBuilder.toString();
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     * @return
     */
    public ArrayList<Tag> getTagList() {
        HashMap<String, Tag> tags = new HashMap<String, Tag>();

        Tag[] tagsByAlpha = getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA,
                TaskCriteria.activeAndVisible());
        for(Tag tag : tagsByAlpha)
            if(!TextUtils.isEmpty(tag.tag))
                tags.put(tag.tag, tag);

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES));
        try {
            TagData tagData = new TagData();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                tagData.readFromCursor(cursor);
                String tagName = tagData.getValue(TagData.NAME).trim();
                Tag tag = new Tag(tagData);
                if(tagData.getValue(TagData.DELETION_DATE) > 0 && !tags.containsKey(tagName)) continue;
                if(TextUtils.isEmpty(tag.tag))
                    continue;
                tags.put(tagName, tag);

                Update update = tagDataService.getLatestUpdate(tagData);
                if(update != null)
                    tag.updateText = ActFmPreferenceService.updateToString(update);
            }
        } finally {
            cursor.close();
        }
        ArrayList<Tag> tagList = new ArrayList<Tag>(tags.values());
        Collections.sort(tagList,
                new Comparator<Tag>() {
            @Override
            public int compare(Tag object1, Tag object2) {
                return object1.tag.compareToIgnoreCase(object2.tag);
            }
        });
        return tagList;
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public boolean synchronizeTags(long taskId, LinkedHashSet<String> tags) {
        MetadataService service = PluginServices.getMetadataService();

        HashSet<String> addedTags = new HashSet<String>();
        ArrayList<Metadata> metadata = new ArrayList<Metadata>();
        for(String tag : tags) {
            String tagWithCase = getTagWithCase(tag); // Find if any tag exists that matches with case ignore
            if (addedTags.contains(tagWithCase)) // Prevent two identical tags from being added twice (e.g. don't add "Tag, tag" as "tag, tag")
                continue;
            addedTags.add(tagWithCase);
            Metadata item = new Metadata();
            item.setValue(Metadata.KEY, KEY);
            item.setValue(TAG, tagWithCase);
            TagData tagData = tagDataService.getTag(tagWithCase, TagData.REMOTE_ID);
            if(tagData != null)
                item.setValue(REMOTE_ID, tagData.getValue(TagData.REMOTE_ID));

            metadata.add(item);
        }

        return service.synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(KEY));
    }

    /**
     * If a tag already exists in the database that case insensitively matches the
     * given tag, return that. Otherwise, return the argument
     * @param tag
     * @return
     */
    public String getTagWithCase(String tag) {
        MetadataService service = PluginServices.getMetadataService();
        String tagWithCase = tag;
        TodorooCursor<Metadata> tagMetadata = service.query(Query.select(TAG).where(TagService.tagEqIgnoreCase(tag, Criterion.all)).limit(1));
        try {
            if (tagMetadata.getCount() > 0) {
                tagMetadata.moveToFirst();
                Metadata tagMatch = new Metadata(tagMetadata);
                tagWithCase = tagMatch.getValue(TagService.TAG);
            } else {
                TodorooCursor<TagData> tagData = tagDataService.query(Query.select(TagData.NAME).where(TagData.NAME.eqCaseInsensitive(tag)));
                try {
                    if (tagData.getCount() > 0) {
                        tagData.moveToFirst();
                        tagWithCase = new TagData(tagData).getValue(TagData.NAME);
                    }
                } finally {
                    tagData.close();
                }
            }
        } finally {
            tagMetadata.close();
        }
        return tagWithCase;
    }

    public int delete(String tag) {
        invalidateTaskCache(tag);
        return PluginServices.getMetadataService().deleteWhere(tagEqIgnoreCase(tag, Criterion.all));
    }

    public int rename(String oldTag, String newTag) {
        return renameHelper(oldTag, newTag, false);
    }

    public int renameCaseSensitive(String oldTag, String newTag) { // Need this for tag case migration process
        return renameHelper(oldTag, newTag, true);
    }

    private int renameHelper(String oldTag, String newTag, boolean caseSensitive) {
     // First remove newTag from all tasks that have both oldTag and newTag.
        MetadataService metadataService = PluginServices.getMetadataService();
        metadataService.deleteWhere(
                Criterion.and(
                        Metadata.VALUE1.eq(newTag),
                        Metadata.TASK.in(rowsWithTag(oldTag, Metadata.TASK))));

        // Then rename all instances of oldTag to newTag.
        Metadata metadata = new Metadata();
        metadata.setValue(TAG, newTag);
        int ret;
        if (caseSensitive)
            ret = metadataService.update(tagEq(oldTag, Criterion.all), metadata);
        else
            ret = metadataService.update(tagEqIgnoreCase(oldTag, Criterion.all), metadata);
        invalidateTaskCache(newTag);
        return ret;
    }


    private Query rowsWithTag(String tag, Field... projections) {
        return Query.select(projections).from(Metadata.TABLE).where(Metadata.VALUE1.eq(tag));
    }

    private void invalidateTaskCache(String tag) {
        taskService.clearDetails(Task.ID.in(rowsWithTag(tag, Task.ID)));
        Flags.set(Flags.REFRESH);
    }

    public static int getDefaultImageIDForTag(long remoteID) {
        if (remoteID <= 0) {
            int random = (int)(Math.random()*4);
            return default_tag_images[random];
        }
        return default_tag_images[((int)remoteID)%4];
    }
    public static int getDefaultImageIDForTag(String title) {
        return getDefaultImageIDForTag(Math.abs(title.hashCode()));
    }
}
