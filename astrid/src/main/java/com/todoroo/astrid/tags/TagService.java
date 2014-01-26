/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

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
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.MetadataService;
import com.todoroo.astrid.service.TagDataService;

import org.tasks.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides operations for working with tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagService {

    // --- singleton

    private static TagService instance = null;

    private static int[] default_tag_images = new int[] {
        R.drawable.default_list_0,
        R.drawable.default_list_1,
        R.drawable.default_list_2,
        R.drawable.default_list_3
    };

    public static synchronized TagService getInstance() {
        if(instance == null) {
            instance = new TagService();
        }
        return instance;
    }

    // --- implementation details

    @Autowired MetadataDao metadataDao;

    @Autowired TagDataService tagDataService;

    @Autowired TagDataDao tagDataDao;

    public TagService() {
        DependencyInjectionService.getInstance().inject(this);
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
    public static final class Tag {
        public String tag;
        public int count;
        public String uuid;
        public String image;

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
            tag = tagData.getName();
            count = tagData.getTaskCount();
            uuid = tagData.getUUID();
            image = tagData.getPictureUrl(TagData.PICTURE, RemoteModel.PICTURE_THUMB);
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
            ArrayList<Tag> array = new ArrayList<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                Tag tag = Tag.tagFromUUID(cursor.get(TaskToTagMetadata.TAG_UUID));
                if (tag != null) {
                    array.add(tag);
                }
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
            createLink(task, tagData.getName(), tagData.getUUID());
        } finally {
            existingTag.close();
        }
    }

    public void createLink(Task task, String tagName, String tagUuid) {
        Metadata link = TaskToTagMetadata.newTagMetadata(task.getId(), task.getUuid(), tagName, tagUuid);
        if (metadataDao.update(Criterion.and(MetadataCriteria.byTaskAndwithKey(task.getId(), TaskToTagMetadata.KEY),
                    TaskToTagMetadata.TASK_UUID.eq(task.getUUID()), TaskToTagMetadata.TAG_UUID.eq(tagUuid)), link) <= 0) {
            metadataDao.createNew(link);
        }
    }

    /**
     * Delete all links between the specified task and the list of tags
     */
    public void deleteLinks(long taskId, String taskUuid, String[] tagUuids) {
        Metadata deleteTemplate = new Metadata();
        deleteTemplate.setValue(Metadata.TASK, taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setValue(Metadata.DELETION_DATE, DateUtilities.now());
        if (tagUuids != null) {
            for (String uuid : tagUuids) {
                // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
                // the outstanding entry correctly. If possible, this should be improved to a single query
                deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, uuid); // Need this for recording changes in outstanding table
                metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleteTemplate);
            }
        }
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
     * Return tags as a comma-separated list of strings
     * @return empty string if no tags, otherwise string
     */
    public String getTagsAsString(long taskId) {
        return getTagsAsString(taskId, ", ");
    }

    /**
     * Return tags as a list of strings separated by given separator
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
                if (i < length - 1) {
                    tagBuilder.append(separator);
                }
            }
        } finally {
            tags.close();
        }
        return tagBuilder.toString();
    }

    public Intent deleteOrLeaveTag(Context context, String tag, String uuid) {
        int deleted = deleteTagMetadata(uuid);
        TagData tagData = tagDataDao.fetch(uuid, TagData.ID, TagData.UUID, TagData.DELETION_DATE, TagData.MEMBER_COUNT, TagData.USER_ID);
        Intent tagDeleted = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED);
        if(tagData != null) {
            tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
            PluginServices.getTagDataService().save(tagData);
            tagDeleted.putExtra(TagViewFragment.EXTRA_TAG_UUID, tagData.getUuid());
        }
        Toast.makeText(context, context.getString(R.string.TEA_tags_deleted, tag, deleted), Toast.LENGTH_SHORT).show();

        context.sendBroadcast(tagDeleted);
        return tagDeleted;
    }

    /**
     * Return all tags (including metadata tags and TagData tags) in an array list
     */
    public ArrayList<Tag> getTagList() {
        ArrayList<Tag> tagList = new ArrayList<>();
        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(TagData.PROPERTIES).where(Criterion.and(TagData.DELETION_DATE.eq(0), Criterion.or(TagData.IS_FOLDER.isNull(),
                TagData.IS_FOLDER.neq(1)), TagData.NAME.isNotNull())).orderBy(Order.asc(Functions.upper(TagData.NAME))));
        try {
            TagData tagData = new TagData();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                tagData.readFromCursor(cursor);
                Tag tag = new Tag(tagData);
                if(TextUtils.isEmpty(tag.tag)) {
                    continue;
                }
                tagList.add(tag);
            }
        } finally {
            cursor.close();
        }
        return tagList;
    }

    /**
     * Save the given array of tags into the database
     */
    public void synchronizeTags(long taskId, String taskUuid, Set<String> tags) {
        HashSet<String> existingLinks = new HashSet<>();
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
            if (existingLinks.contains(tagData.getUUID())) {
                existingLinks.remove(tagData.getUUID());
            } else {
                Metadata newLink = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, tag, tagData.getUUID());
                metadataDao.createNew(newLink);
            }
        }

        // Mark as deleted links that don't exist anymore
        deleteLinks(taskId, taskUuid, existingLinks.toArray(new String[existingLinks.size()]));
    }

    /**
     * If a tag already exists in the database that case insensitively matches the
     * given tag, return that. Otherwise, return the argument
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
                        tagWithCase = new TagData(tagData).getName();
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
        TagData template = new TagData();
        template.setValue(TagData.NAME, newName);
        tagDataDao.update(TagData.UUID.eq(uuid), template);

        Metadata metadataTemplate = new Metadata();
        metadataTemplate.setValue(TaskToTagMetadata.TAG_NAME, newName);

        return metadataDao.update(Criterion.and(MetadataCriteria.withKey(TaskToTagMetadata.KEY), TaskToTagMetadata.TAG_UUID.eq(uuid)), metadataTemplate);
    }

    public static int getDefaultImageIDForTag(String nameOrUUID) {
        if (RemoteModel.NO_UUID.equals(nameOrUUID)) {
            int random = (int)(Math.random()*4);
            return default_tag_images[random];
        }
        return default_tag_images[(Math.abs(nameOrUUID.hashCode()))%4];
    }
}
