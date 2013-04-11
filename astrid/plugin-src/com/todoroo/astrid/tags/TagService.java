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
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.TaskService;

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

    @Autowired TagDataDao tagDataDao;

    public TagService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Property for retrieving count of aggregated rows
     */
    private static final CountProperty COUNT = new CountProperty();
    public static final Order GROUPED_TAGS_BY_ALPHA = Order.asc(Functions.upper(TaskToTagMetadata.TAG_NAME));
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
        public String uuid;
        public String image;
        public String userId;
        public long memberCount;

        public static Tag tagFromUUID(String uuid) {
            TodorooCursor<TagData> tagData = PluginServices.getTagDataService().query(Query.select(TagData.PROPERTIES).where(TagData.UUID.eq(uuid)));
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
            uuid = tagData.getValue(TagData.UUID);
            image = tagData.getPictureUrl(TagData.PICTURE, RemoteModel.PICTURE_THUMB);
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
                    Field.field("mtags." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY),
                    Field.field("mtags." + TaskToTagMetadata.TAG_UUID.name).eq(uuid),
                    Field.field("mtags." + Metadata.DELETION_DATE.name).eq(0),
                    criterion);
            return new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"), Task.UUID.eq(Field.field("mtags." + TaskToTagMetadata.TASK_UUID.name))))
                    .where(fullCriterion);
        }

    }

    @Deprecated
    private static Criterion tagEq(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_NAME.eq(tag),
                additionalCriterion);
    }

    public static Criterion tagEqIgnoreCase(String tag, Criterion additionalCriterion) {
        return Criterion.and(
                MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_NAME.eqCaseInsensitive(tag),
                additionalCriterion);
    }

    public QueryTemplate untaggedTemplate() {
        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.UUID.in(Query.select(TaskToTagMetadata.TASK_UUID).from(Metadata.TABLE)
                        .where(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0))))),
                TaskCriteria.isActive(),
                TaskCriteria.ownedByMe(),
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
        Criterion criterion = Criterion.and(activeStatus, MetadataCriteria.withKey(TaskToTagMetadata.KEY));
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID, COUNT).
            join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
            where(criterion).
            orderBy(order).groupBy(TaskToTagMetadata.TAG_NAME);
        TodorooCursor<Metadata> cursor = metadataDao.query(query);
        try {
            ArrayList<Tag> array = new ArrayList<Tag>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                Tag tag = Tag.tagFromUUID(cursor.get(TaskToTagMetadata.TAG_UUID));
                if (tag != null)
                    array.add(tag);
            }
            return array.toArray(new Tag[array.size()]);
        } finally {
            cursor.close();
        }
    }

    public void createLink(Task task, String tagName) {
        TodorooCursor<TagData> existingTag = tagDataService.query(Query.select(TagData.NAME, TagData.UUID)
                .where(TagData.NAME.eqCaseInsensitive(tagName)));
        try {
            TagData tagData;
            if (existingTag.getCount() == 0) {
                tagData = new TagData();
                tagData.setValue(TagData.NAME, tagName);
                tagDataService.save(tagData);
            } else {
                existingTag.moveToFirst();
                tagData = new TagData(existingTag);
            }
            createLink(task, tagData.getValue(TagData.NAME), tagData.getValue(TagData.UUID));
        } finally {
            existingTag.close();
        }
    }

    public void createLink(Task task, String tagName, String tagUuid) {
        Metadata link = TaskToTagMetadata.newTagMetadata(task.getId(), task.getUuid(), tagName, tagUuid);
        if (metadataDao.update(Criterion.and(MetadataCriteria.byTaskAndwithKey(task.getId(), TaskToTagMetadata.KEY),
                    TaskToTagMetadata.TASK_UUID.eq(task.getValue(Task.UUID)), TaskToTagMetadata.TAG_UUID.eq(tagUuid)), link) <= 0) {
            metadataDao.createNew(link);
        }
    }

    /**
     * Creates a link for a nameless tag. We expect the server to fill in the tag name with a MakeChanges message later
     * @param taskId
     * @param taskUuid
     * @param tagUuid
     */
    public void createLink(long taskId, String taskUuid, String tagUuid, boolean suppressOutstanding) {
        TodorooCursor<TagData> existingTag = tagDataService.query(Query.select(TagData.NAME, TagData.UUID).where(TagData.UUID.eq(tagUuid)));
        try {
            TagData tagData;
            String name = "";
            if (existingTag.getCount() > 0) {
                existingTag.moveToFirst();
                tagData = new TagData(existingTag);
                name = tagData.getValue(TagData.NAME);
            }

            Metadata link = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, name, tagUuid);
            if (suppressOutstanding)
                link.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
            if (metadataDao.update(Criterion.and(MetadataCriteria.byTaskAndwithKey(taskId, TaskToTagMetadata.KEY),
                    TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(tagUuid)), link) <= 0) {
                if (suppressOutstanding)
                    link.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                metadataDao.createNew(link);
            }

        } finally {
            existingTag.close();
        }
    }

    /**
     * Delete a single task to tag link
     * @param taskUuid
     * @param tagUuid
     */
    public void deleteLink(long taskId, String taskUuid, String tagUuid, boolean suppressOutstanding) {
        Metadata deleteTemplate = new Metadata();
        if (suppressOutstanding)
            deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        deleteTemplate.setValue(Metadata.TASK, taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, tagUuid); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(tagUuid)), deleteTemplate);
    }

    /**
     * Delete all links between the specified task and the list of tags
     * @param taskUuid
     * @param tagUuids
     */
    public void deleteLinks(long taskId, String taskUuid, String[] tagUuids, boolean suppressOutstanding) {
        Metadata deleteTemplate = new Metadata();
        deleteTemplate.setValue(Metadata.TASK, taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        if (tagUuids != null) {
            for (String uuid : tagUuids) {
                // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
                // the outstanding entry correctly. If possible, this should be improved to a single query
                deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, uuid); // Need this for recording changes in outstanding table
                if (suppressOutstanding)
                    deleteTemplate.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
                metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleteTemplate);
            }
        }
    }

    /**
     * Return tags on the given task
     *
     * @param taskId
     * @return cursor. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getTags(long taskId) {
        Criterion criterion = Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                    Metadata.DELETION_DATE.eq(0),
                    MetadataCriteria.byTask(taskId));
        Query query = Query.select(TaskToTagMetadata.TAG_NAME, TaskToTagMetadata.TAG_UUID).where(criterion).orderBy(Order.asc(Functions.upper(TaskToTagMetadata.TAG_NAME)));
        return metadataDao.query(query);
    }

    public TodorooCursor<TagData> getTagDataForTask(long taskId, Property<?>... properties) {
        Criterion criterion = TagData.UUID.in(Query.select(TaskToTagMetadata.TAG_UUID)
                .from(Metadata.TABLE)
                .where(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY),
                        Metadata.DELETION_DATE.eq(0),
                        Metadata.TASK.eq(taskId))));

        return tagDataService.query(Query.select(properties).where(criterion));
    }

    public TodorooCursor<TagData> getTagDataForTask(long taskId, Criterion additionalCriterion, Property<?>... properties) {
        Criterion criterion = TagData.UUID.in(Query.select(TaskToTagMetadata.TAG_UUID).from(Metadata.TABLE).where(
                Criterion.and(Metadata.DELETION_DATE.eq(0),
                        MetadataCriteria.byTaskAndwithKey(taskId, TaskToTagMetadata.KEY))));
        return tagDataService.query(Query.select(properties).where(Criterion.and(criterion, additionalCriterion)));
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
                tagBuilder.append(metadata.getValue(TaskToTagMetadata.TAG_NAME));
                if (i < length - 1)
                    tagBuilder.append(separator);
            }
        } finally {
            tags.close();
        }
        return tagBuilder.toString();
    }

    public Intent deleteOrLeaveTag(Context context, String tag, String uuid) {
        int deleted = deleteTagMetadata(uuid);
        TagData tagData = tagDataDao.fetch(uuid, TagData.ID, TagData.UUID, TagData.DELETION_DATE, TagData.MEMBER_COUNT, TagData.USER_ID);
        boolean shared = false;
        Intent tagDeleted = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED);
        if(tagData != null) {
            tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
            PluginServices.getTagDataService().save(tagData);
            tagDeleted.putExtra(TagViewFragment.EXTRA_TAG_UUID, tagData.getUuid());
            shared = tagData.getValue(TagData.MEMBER_COUNT) > 0 && !Task.USER_ID_SELF.equals(tagData.getValue(TagData.USER_ID)); // Was I a list member and NOT owner?
        }
        Toast.makeText(context, context.getString(shared ? R.string.TEA_tags_left : R.string.TEA_tags_deleted, tag, deleted),
                Toast.LENGTH_SHORT).show();

        context.sendBroadcast(tagDeleted);
        return tagDeleted;
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     * @return
     */
    public ArrayList<Tag> getTagList() {
        ArrayList<Tag> tagList = new ArrayList<Tag>();
        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.and(TagData.DELETION_DATE.eq(0), Criterion.or(TagData.IS_FOLDER.isNull(),
                TagData.IS_FOLDER.neq(1)), TagData.NAME.isNotNull())).orderBy(Order.asc(Functions.upper(TagData.NAME))));
        try {
            TagData tagData = new TagData();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                tagData.readFromCursor(cursor);
                if(tagData.getFlag(TagData.FLAGS, TagData.FLAG_FEATURED)) {
                    continue;
                }
                Tag tag = new Tag(tagData);
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
    public boolean synchronizeTags(long taskId, String taskUuid, Set<String> tags) {
        HashSet<String> existingLinks = new HashSet<String>();
        TodorooCursor<Metadata> links = metadataDao.query(Query.select(Metadata.PROPERTIES)
                .where(Criterion.and(TaskToTagMetadata.TASK_UUID.eq(taskUuid), Metadata.DELETION_DATE.eq(0))));
        try {
            for (links.moveToFirst(); !links.isAfterLast(); links.moveToNext()) {
                Metadata link = new Metadata(links);
                existingLinks.add(link.getValue(TaskToTagMetadata.TAG_UUID));
            }
        } finally {
            links.close();
        }

        for (String tag : tags) {
            TagData tagData = getTagDataWithCase(tag, TagData.NAME, TagData.UUID);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setValue(TagData.NAME, tag);
                tagDataService.save(tagData);
            }
            if (existingLinks.contains(tagData.getValue(TagData.UUID))) {
                existingLinks.remove(tagData.getValue(TagData.UUID));
            } else {
                Metadata newLink = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, tag, tagData.getValue(TagData.UUID));
                metadataDao.createNew(newLink);
            }
        }

        // Mark as deleted links that don't exist anymore
        deleteLinks(taskId, taskUuid, existingLinks.toArray(new String[existingLinks.size()]), false);

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
        TodorooCursor<Metadata> tagMetadata = service.query(Query.select(TaskToTagMetadata.TAG_NAME).where(TagService.tagEqIgnoreCase(tag, Criterion.all)).limit(1));
        try {
            if (tagMetadata.getCount() > 0) {
                tagMetadata.moveToFirst();
                Metadata tagMatch = new Metadata(tagMetadata);
                tagWithCase = tagMatch.getValue(TaskToTagMetadata.TAG_NAME);
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

    public int deleteTagMetadata(String uuid) {
        Metadata deleted = new Metadata();
        deleted.setValue(Metadata.DELETION_DATE, DateUtilities.now());

        return metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleted);
    }

    public int rename(String uuid, String newName) {
        return rename(uuid, newName, false);
    }

    public int rename(String uuid, String newName, boolean suppressSync) {
        TagData template = new TagData();
        template.setValue(TagData.NAME, newName);
        if (suppressSync)
            template.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
        int result = tagDataDao.update(TagData.UUID.eq(uuid), template);

        boolean tagRenamed = result > 0;

        Metadata metadataTemplate = new Metadata();
        metadataTemplate.setValue(TaskToTagMetadata.TAG_NAME, newName);
        result = metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)), metadataTemplate);
        tagRenamed = tagRenamed || result > 0;

        return result;
    }

    public static int getDefaultImageIDForTag(String nameOrUUID) {
        if (RemoteModel.NO_UUID.equals(nameOrUUID)) {
            int random = (int)(Math.random()*4);
            return default_tag_images[random];
        }
        return default_tag_images[((int)Math.abs(nameOrUUID.hashCode()))%4];
    }
}
