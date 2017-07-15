package com.todoroo.astrid.service;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

public class TaskCreator {

    private final GCalHelper gcalHelper;
    private final Preferences preferences;
    private final MetadataDao metadataDao;
    private final TagDataDao tagDataDao;
    private final TaskDao taskDao;
    private final TagService tagService;

    @Inject
    public TaskCreator(GCalHelper gcalHelper, Preferences preferences, MetadataDao metadataDao,
                       TagDataDao tagDataDao, TaskDao taskDao, TagService tagService) {
        this.gcalHelper = gcalHelper;
        this.preferences = preferences;
        this.metadataDao = metadataDao;
        this.tagDataDao = tagDataDao;
        this.taskDao = taskDao;
        this.tagService = tagService;
    }

    public Task basicQuickAddTask(String title) {
        if (TextUtils.isEmpty(title)) {
            return null;
        }

        title = title.trim();

        Task task = createWithValues(null, title);
        addToCalendar(task);

        return task;
    }

    public void addToCalendar(Task task) {
        boolean gcalCreateEventEnabled = preferences.isDefaultCalendarSet() && task.hasDueDate(); //$NON-NLS-1$
        if (!TextUtils.isEmpty(task.getTitle()) && gcalCreateEventEnabled && TextUtils.isEmpty(task.getCalendarURI())) {
            Uri calendarUri = gcalHelper.createTaskEvent(task, new ContentValues());
            task.setCalendarUri(calendarUri.toString());
            task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true);
            taskDao.save(task);
        }
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public Task createWithValues(ContentValues values, String title) {
        return createWithValues(new Task(), values, title);
    }

    Task createWithValues(Task task, ContentValues values, String title) {
        if (title != null) {
            task.setTitle(title.trim());
        }

        ArrayList<String> tags = new ArrayList<>();
        try {
            parseQuickAddMarkup(task, tags);
        } catch (Throwable e) {
            Timber.e(e, e.getMessage());
        }

        ContentValues forMetadata = null;
        if (values != null && values.size() > 0) {
            ContentValues forTask = new ContentValues();
            forMetadata = new ContentValues();
            outer: for (Map.Entry<String, Object> item : values.valueSet()) {
                String key = item.getKey();
                Object value = item.getValue();
                if (value instanceof String) {
                    value = PermaSql.replacePlaceholders((String) value);
                }

                for (Property<?> property : Metadata.PROPERTIES) {
                    if (property.name.equals(key)) {
                        AndroidUtilities.putInto(forMetadata, key, value);
                        continue outer;
                    }
                }

                AndroidUtilities.putInto(forTask, key, value);
            }
            task.mergeWithoutReplacement(forTask);
        }

        saveWithoutPublishingFilterUpdate(task);
        for(String tag : tags) {
            createLink(task, tag);
        }

        if (forMetadata != null && forMetadata.size() > 0) {
            Metadata metadata = new Metadata();
            metadata.setTask(task.getId());
            metadata.mergeWith(forMetadata);
            if (TaskToTagMetadata.KEY.equals(metadata.getKey())) {
                if (metadata.containsNonNullValue(TaskToTagMetadata.TAG_UUID) && !RemoteModel.NO_UUID.equals(metadata.getValue(TaskToTagMetadata.TAG_UUID))) {
                    // This is more efficient
                    createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME), metadata.getValue(TaskToTagMetadata.TAG_UUID));
                } else {
                    // This is necessary for backwards compatibility
                    createLink(task, metadata.getValue(TaskToTagMetadata.TAG_NAME));
                }
            } else {
                metadataDao.persist(metadata);
            }
        }

        return task;
    }

    private void saveWithoutPublishingFilterUpdate(Task item) {
        taskDao.save(item);
    }

    private void createLink(Task task, String tagName) {
        TagData tagData = tagDataDao.getTagByName(tagName, TagData.NAME, TagData.UUID);
        if (tagData == null) {
            tagData = new TagData();
            tagData.setName(tagName);
            tagDataDao.persist(tagData);
        }
        createLink(task, tagData.getName(), tagData.getUUID());
    }

    private void createLink(Task task, String tagName, String tagUuid) {
        Metadata link = TaskToTagMetadata.newTagMetadata(task.getId(), task.getUuid(), tagName, tagUuid);
        if (metadataDao.update(Criterion.and(MetadataDao.MetadataCriteria.byTaskAndwithKey(task.getId(), TaskToTagMetadata.KEY),
                TaskToTagMetadata.TASK_UUID.eq(task.getUUID()), TaskToTagMetadata.TAG_UUID.eq(tagUuid)), link) <= 0) {
            metadataDao.createNew(link);
        }
    }

    /**
     * Parse quick add markup for the given task
     * @param tags an empty array to apply tags to
     */
    void parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        TitleParser.parse(tagService, task, tags);
    }
}
