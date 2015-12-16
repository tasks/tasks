/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TaskToTagMetadata;
import com.todoroo.astrid.utility.TitleParser;

import org.tasks.Broadcaster;
import org.tasks.filters.FilterCounter;
import org.tasks.scheduling.RefreshScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;


/**
 * Service layer for {@link Task}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class TaskService {

    public static final String TRANS_EDIT_SAVE = "task-edit-save"; //$NON-NLS-1$

    public static final String TRANS_REPEAT_COMPLETE = "repeat-complete"; //$NON-NLS-1$

    private final TagDataDao tagDataDao;
    private final TaskDao taskDao;
    private final Broadcaster broadcaster;
    private final FilterCounter filterCounter;
    private final RefreshScheduler refreshScheduler;
    private final TagService tagService;
    private final MetadataDao metadataDao;

    @Inject
    public TaskService(TagDataDao tagDataDao, TaskDao taskDao, Broadcaster broadcaster, FilterCounter filterCounter,
                       RefreshScheduler refreshScheduler, TagService tagService, MetadataDao metadataDao) {
        this.tagDataDao = tagDataDao;
        this.taskDao = taskDao;
        this.broadcaster = broadcaster;
        this.filterCounter = filterCounter;
        this.refreshScheduler = refreshScheduler;
        this.tagService = tagService;
        this.metadataDao = metadataDao;
    }

    // --- service layer

    /**
     * Query underlying database
     */
    public TodorooCursor<Task> query(Query query) {
        return taskDao.query(query);
    }

    /**
     * @return item, or null if it doesn't exist
     */
    public Task fetchById(long id, Property<?>... properties) {
        return taskDao.fetch(id, properties);
    }

    /**
     * Mark the given task as completed and save it.
     */
    public void setComplete(Task item, boolean completed) {
        if(completed) {
            item.setCompletionDate(DateUtilities.now());
        } else {
            item.setCompletionDate(0L);
        }

        save(item);
    }

    /**
     * Create or save the given action item
     */
    public void save(Task item) {
        taskDao.save(item);
        broadcastFilterListUpdated();
        refreshScheduler.scheduleRefresh(item);
    }

    private void saveWithoutPublishingFilterUpdate(Task item) {
        taskDao.save(item);
    }

    /**
     * Fetch tasks for the given filter
     * @param constraint text constraint, or null
     */
    public TodorooCursor<Task> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null) {
            whereConstraint = Functions.upper(Task.TITLE).like("%" +
                    constraint.toString().toUpperCase() + "%");
        }

        if(queryTemplate == null) {
            if(whereConstraint == null) {
                return taskDao.query(Query.selectDistinct(properties));
            } else {
                return taskDao.query(Query.selectDistinct(properties).where(whereConstraint));
            }
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE")) {
                sql = queryTemplate + " WHERE " + whereConstraint;
            } else {
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
            }
        } else {
            sql = queryTemplate;
        }

        sql = PermaSql.replacePlaceholders(sql);

        return taskDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    /**
     * @return how many tasks are matched by this query
     */
    public int count(Query query) {
        return taskDao.count(query);
    }

    /**
     * Update database based on selection and values
     */
    public int updateBySelection(String selection, String[] selectionArgs,
            Task taskValues) {
        TodorooCursor<Task> cursor = taskDao.rawQuery(selection, selectionArgs, Task.ID);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                taskValues.setID(cursor.get(Task.ID));
                save(taskValues);
            }
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Update all matching a clause to have the values set on template object.
     * <p>
     * Example (updates "joe" => "bob" in metadata value1):
     * {code}
     * Metadata item = new Metadata();
     * item.setVALUE1("bob");
     * update(item, Metadata.VALUE1.eq("joe"));
     * {code}
     * @param where sql criteria
     * @param template set fields on this object in order to set them in the db.
     * @return # of updated items
     */
    public int update(Criterion where, Task template) {
        return taskDao.update(where, template);
    }

    /**
     * Count tasks overall
     */
    public int countTasks() {
        TodorooCursor<Task> cursor = query(Query.select(Task.ID));
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }

    /**
     * Save task, parsing quick-add mark-up:
     * <ul>
     * <li>#tag - add the tag "tag"
     * <li>@context - add the tag "@context"
     * <li>!4 - set priority to !!!!
     */
    private void quickAdd(Task task, List<String> tags) {
        saveWithoutPublishingFilterUpdate(task);
        for(String tag : tags) {
            createLink(task, tag);
        }
        broadcastFilterListUpdated();
    }

    private void broadcastFilterListUpdated() {
        filterCounter.refreshFilterCounts(new Runnable() {
            @Override
            public void run() {
                broadcaster.refresh();
            }
        });
    }

    /**
     * Parse quick add markup for the given task
     * @param tags an empty array to apply tags to
     */
    void parseQuickAddMarkup(Task task, ArrayList<String> tags) {
        TitleParser.parse(tagService, task, tags);
    }

    /**
     * Create task from the given content values, saving it. This version
     * doesn't need to start with a base task model.
     */
    public Task createWithValues(ContentValues values, String title) {
        Task task = new Task();
        return createWithValues(task, values, title);
    }

    /**
     * Create task from the given content values, saving it.
     * @param task base task to start with
     */
    public Task createWithValues(Task task, ContentValues values, String title) {
        if (title != null) {
            task.setTitle(title);
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
            outer: for (Entry<String, Object> item : values.valueSet()) {
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

        quickAdd(task, tags);

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
}
