/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.CountProperty;
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
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskApiDao;
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

    public static final String TOKEN_TAG_SQL = "tagSql"; //$NON-NLS-1$
    public static final String SHOW_ACTIVE_TASKS = "show_main_task_view"; //$NON-NLS-1$

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
    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(Functions.upper(TagMetadata.TAG_NAME));
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
        public long id;
        public long remoteId;
        public String image;
        public long userId;
        public long memberCount;

        public static Tag tagFromRemoteId(long remoteId) {
            TodorooCursor<TagData> tagData = PluginServices.getTagDataService().query(Query.select(TagData.PROPERTIES).where(TagData.REMOTE_ID.eq(remoteId)));
            try {
                if (tagData.getCount() > 0) {
                    tagData.moveToFirst();
                    return new Tag(new TagData(tagData));
                } else {
                    return null;
                }
            } finally {
                tagData.close();
            }

        }

        public Tag(TagData tagData) {
            id = tagData.getId();
            tag = tagData.getValue(TagData.NAME);
            count = tagData.getValue(TagData.TASK_COUNT);
            remoteId = tagData.getValue(TagData.REMOTE_ID);
            image = tagData.getValue(TagData.PICTURE);
            userId = tagData.getValue(TagData.USER_ID);
            memberCount = tagData.getValue(TagData.MEMBER_COUNT);
        }

        @Override
        public String toString() {
            return tag;
        }

        /**
         * Return SQL selector query for getting tasks with a given tagData
         *
         * @param tagData
         * @return
         */
        public QueryTemplate queryTemplate(Criterion criterion) {
            Criterion fullCriterion = Criterion.and(
                    Field.field("mtags." + Metadata.KEY.name).eq(TagMetadata.KEY),
                    Field.field("mtags." + TagMetadata.TAG_UUID.name).eq(remoteId),
                    Field.field("mtags." + Metadata.DELETION_DATE.name).eq(0),
                    criterion);
            return new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"), Task.REMOTE_ID.eq(Field.field("mtags." + TagMetadata.TASK_UUID.name))))
                    .where(fullCriterion);
        }

    }

    public static Criterion memberOfTagData(long tagDataRemoteId) {
        return Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(
                Criterion.and(Metadata.KEY.eq(TagMetadata.KEY), TagMetadata.TAG_UUID.eq(tagDataRemoteId))));
    }

    public static Criterion tagEq(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(TagMetadata.KEY), TagMetadata.TAG_NAME.eq(tag),
                additionalCriterion);
    }

    public static Criterion tagEqIgnoreCase(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(TagMetadata.KEY), TagMetadata.TAG_NAME.eqCaseInsensitive(tag),
                additionalCriterion);
    }

    public QueryTemplate untaggedTemplate() {
        Long[] emergentTags = getEmergentTagIds();

        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.REMOTE_ID.in(Query.select(TagMetadata.TASK_UUID).from(Metadata.TABLE)
                        .where(Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY), Criterion.not(TagMetadata.TAG_UUID.in(emergentTags)))))),
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
    @Deprecated
    public Tag[] getGroupedTags(Order order, Criterion activeStatus, boolean includeEmergent) {
        Criterion criterion;
        if (includeEmergent)
            criterion = Criterion.and(activeStatus, MetadataCriteria.withKey(TagMetadata.KEY));
        else
            criterion = Criterion.and(activeStatus, MetadataCriteria.withKey(TagMetadata.KEY), Criterion.not(TagMetadata.TAG_UUID.in(getEmergentTagIds())));
        Query query = Query.select(TagMetadata.TAG_NAME, TagMetadata.TAG_UUID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(criterion).
            orderBy(order).groupBy(TagMetadata.TAG_NAME);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            ArrayList<Tag> array = new ArrayList<Tag>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                Tag tag = Tag.tagFromRemoteId(cursor.get(TagMetadata.TAG_UUID));
                if (tag != null)
                    array.add(tag);
            }
            return array.toArray(new Tag[array.size()]);
        } finally {
            cursor.close();
        }
    }

    public Long[] getEmergentTagIds() {
        TodorooCursor<TagData> emergent = tagDataService.query(Query.select(TagData.ID)
                .where(Functions.bitwiseAnd(TagData.FLAGS, TagData.FLAG_EMERGENT).gt(0)));
        try {
            Long[] tags = new Long[emergent.getCount()];
            TagData data = new TagData();
            for (int i = 0; i < emergent.getCount(); i++) {
                emergent.moveToPosition(i);
                data.readFromCursor(emergent);
                tags[i] = data.getId();
            }
            return tags;
        } finally {
            emergent.close();
        }
    }

    public void createLink(Task task, String tagName, long tagUuid) {
        TodorooCursor<Metadata> existing = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY),
                        TagMetadata.TASK_UUID.eq(task.getValue(Task.REMOTE_ID)),
                        TagMetadata.TAG_UUID.eq(tagUuid),
                        Metadata.DELETION_DATE.eq(0))));
        try {
            if (existing.getCount() == 0) {
                Metadata link = TagMetadata.newTagMetadata(task, tagName, tagUuid);
                metadataDao.createNew(link);
            }
        } finally {
            existing.close();
        }
    }

    /**
     * Return tags on the given task
     *
     * @param taskId
     * @return cursor. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getTags(long taskId, boolean includeEmergent) {
        Criterion criterion;
        if (includeEmergent)
            criterion = Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY),
                    Metadata.DELETION_DATE.eq(0),
                    MetadataCriteria.byTask(taskId));
        else
            criterion = Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY),
                    Metadata.DELETION_DATE.eq(0),
                    MetadataCriteria.byTask(taskId), Criterion.not(TagMetadata.TAG_UUID.in(getEmergentTagIds())));
        Query query = Query.select(TagMetadata.TAG_NAME, TagMetadata.TAG_UUID).where(criterion).orderBy(Order.asc(Functions.upper(TagMetadata.TAG_NAME)));
        return metadataDao.query(query);
    }

    public TodorooCursor<TagData> getTagDataForTask(long taskId, boolean includeEmergent, Property<?>... properties) {
        Criterion criterion = TagData.REMOTE_ID.in(Query.select(TagMetadata.TAG_UUID)
                .from(Metadata.TABLE)
                .where(Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY),
                        Metadata.DELETION_DATE.eq(0),
                        Metadata.TASK.eq(taskId))));
        if (!includeEmergent)
            criterion = Criterion.and(Criterion.not(TagData.REMOTE_ID.in(getEmergentTagIds())), criterion);

        return tagDataService.query(Query.select(properties).where(criterion));
    }

    /**
     * Return tags as a comma-separated list of strings
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId, boolean includeEmergent) {
        return getTagsAsString(taskId, ", ", includeEmergent);
    }

    /**
     * Return tags as a list of strings separated by given separator
     *
     * @param taskId
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId, String separator, boolean includeEmergent) {
        StringBuilder tagBuilder = new StringBuilder();
        TodorooCursor<Metadata> tags = getTags(taskId, includeEmergent);
        try {
            int length = tags.getCount();
            Metadata metadata = new Metadata();
            for (int i = 0; i < length; i++) {
                tags.moveToNext();
                metadata.readFromCursor(tags);
                tagBuilder.append(metadata.getValue(TagMetadata.TAG_NAME));
                if (i < length - 1)
                    tagBuilder.append(separator);
            }
        } finally {
            tags.close();
        }
        return tagBuilder.toString();
    }

    public boolean deleteOrLeaveTag(Context context, String tag, String sql) {
        int deleted = deleteTagMetadata(tag);
        TagData tagData = PluginServices.getTagDataService().getTag(tag, TagData.ID, TagData.DELETION_DATE, TagData.MEMBER_COUNT, TagData.USER_ID);
        boolean shared = false;
        if(tagData != null) {
            tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
            PluginServices.getTagDataService().save(tagData);
            shared = tagData.getValue(TagData.MEMBER_COUNT) > 0 && tagData.getValue(TagData.USER_ID) != 0; // Was I a list member and NOT owner?
        }
        Toast.makeText(context, context.getString(shared ? R.string.TEA_tags_left : R.string.TEA_tags_deleted, tag, deleted),
                Toast.LENGTH_SHORT).show();

        Intent tagDeleted = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED);
        tagDeleted.putExtra(TagViewFragment.EXTRA_TAG_NAME, tag);
        tagDeleted.putExtra(TOKEN_TAG_SQL, sql);
        context.sendBroadcast(tagDeleted);
        return true;
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     * @return
     */
    public ArrayList<Tag> getTagList() {
        ArrayList<Tag> tagList = new ArrayList<Tag>();
        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).orderBy(Order.asc(TagData.NAME)));
        try {
            TagData tagData = new TagData();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                tagData.readFromCursor(cursor);
                Tag tag = new Tag(tagData);
                if(tagData.getValue(TagData.DELETION_DATE) > 0 || tagData.getFlag(TagData.FLAGS, TagData.FLAG_EMERGENT) || tagData.getFlag(TagData.FLAGS, TagData.FLAG_FEATURED)) {
                    continue;
                }
                if(TextUtils.isEmpty(tag.tag))
                    continue;
                tagList.add(tag);
            }
        } finally {
            cursor.close();
        }
        return tagList;
    }

    public ArrayList<Tag> getFeaturedLists() {
        HashMap<String, Tag> tags = new HashMap<String, Tag>();

        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES)
                .where(Functions.bitwiseAnd(TagData.FLAGS, TagData.FLAG_FEATURED).gt(0)));
        try {
            TagData tagData = new TagData();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                tagData.readFromCursor(cursor);
                if (tagData.getValue(TagData.DELETION_DATE) > 0)
                    continue;
                String tagName = tagData.getValue(TagData.NAME).trim();
                Tag tag = new Tag(tagData);
                if(TextUtils.isEmpty(tag.tag))
                    continue;
                tags.put(tagName, tag);
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
    public boolean synchronizeTags(long taskId, long taskUuid, Set<String> tags) {
        HashSet<Long> existingLinks = new HashSet<Long>();
        TodorooCursor<Metadata> links = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(TagMetadata.TASK_UUID.eq(taskUuid), Metadata.DELETION_DATE.eq(0))));
        try {
            for (links.moveToFirst(); !links.isAfterLast(); links.moveToNext()) {
                Metadata link = new Metadata(links);
                existingLinks.add(link.getValue(TagMetadata.TAG_UUID));
            }
        } finally {
            links.close();
        }

        for (String tag : tags) {
            TagData tagData = getTagDataWithCase(tag, TagData.NAME, TagData.REMOTE_ID);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setValue(TagData.NAME, tag);
                tagDataService.save(tagData);
            }
            if (existingLinks.contains(tagData.getValue(TagData.REMOTE_ID))) {
                existingLinks.remove(tagData.getValue(TagData.REMOTE_ID));
            } else {
                Metadata newLink = TagMetadata.newTagMetadata(taskId, taskUuid, tag, tagData.getValue(TagData.REMOTE_ID));
                metadataDao.createNew(newLink);
            }
        }

        // Mark as deleted links that don't exist anymore
        Metadata deletedLinkTemplate = new Metadata();
        deletedLinkTemplate.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        metadataDao.update(Criterion.and(MetadataCriteria.withKey(TagMetadata.KEY), TagMetadata.TASK_UUID.eq(taskUuid),
                TagMetadata.TAG_UUID.in(existingLinks.toArray(new Long[existingLinks.size()]))), deletedLinkTemplate);

        return true;
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
        TodorooCursor<Metadata> tagMetadata = service.query(Query.select(TagMetadata.TAG_NAME).where(TagService.tagEqIgnoreCase(tag, Criterion.all)).limit(1));
        try {
            if (tagMetadata.getCount() > 0) {
                tagMetadata.moveToFirst();
                Metadata tagMatch = new Metadata(tagMetadata);
                tagWithCase = tagMatch.getValue(TagMetadata.TAG_NAME);
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

    public TagData getTagDataWithCase(String tag, Property<?>... properties) {
        TodorooCursor<TagData> tagData = tagDataService.query(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(tag)));
        try {
            if (tagData.getCount() > 0) {
                tagData.moveToFirst();
                return new TagData(tagData);
            }
        } finally {
            tagData.close();
        }
        return null;
    }

    private int deleteTagMetadata(String tag) {
        invalidateTaskCache(tag);
        Metadata deleted = new Metadata();
        deleted.setValue(Metadata.DELETION_DATE, DateUtilities.now());

        return metadataDao.update(tagEqIgnoreCase(tag, Criterion.all), deleted);
    }

    public int rename(String oldTag, String newTag) {
        return renameHelper(oldTag, newTag, false);
    }

    public int renameCaseSensitive(String oldTag, String newTag) { // Need this for tag case migration process
        return renameHelper(oldTag, newTag, true);
    }

    @Deprecated
    private int renameHelper(String oldTag, String newTag, boolean caseSensitive) {
     // First remove newTag from all tasks that have both oldTag and newTag.
        MetadataService metadataService = PluginServices.getMetadataService();
        metadataService.deleteWhere(
                Criterion.and(
                        Metadata.VALUE1.eq(newTag),
                        Metadata.TASK.in(rowsWithTag(oldTag, Metadata.TASK))));

        // Then rename all instances of oldTag to newTag.
        Metadata metadata = new Metadata();
        metadata.setValue(TagMetadata.TAG_NAME, newTag);
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
