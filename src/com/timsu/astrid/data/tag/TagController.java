package com.timsu.astrid.data.tag;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.tag.AbstractTagModel.TagModelDatabaseHelper;
import com.timsu.astrid.data.tag.TagToTaskMapping.TagToTaskMappingDatabaseHelper;
import com.timsu.astrid.data.task.TaskIdentifier;

public class TagController extends AbstractController {

    private SQLiteDatabase tagDatabase, tagToTaskMapDatabase;

    // --- tag batch operations

    /** Get a list of all tags */
    public List<TagModelForView> getAllTags()
            throws SQLException {
        List<TagModelForView> list = new LinkedList<TagModelForView>();
        Cursor cursor = tagDatabase.query(TAG_TABLE_NAME,
            TagModelForView.FIELD_LIST, null, null, null, null, null, null);
        activity.startManagingCursor(cursor);

        if(cursor.getCount() == 0)
            return list;
        do {
            cursor.moveToNext();
            list.add(new TagModelForView(cursor));
        } while(!cursor.isLast());

        return list;
    }

    // --- tag to task map batch operations

    /** Get a list of all tags as an id => tag map */
    public Map<TagIdentifier, TagModelForView> getAllTagsAsMap() throws SQLException {
        Map<TagIdentifier, TagModelForView> map = new HashMap<TagIdentifier, TagModelForView>();
        for(TagModelForView tag : getAllTags())
            map.put(tag.getTagIdentifier(), tag);
        return map;
    }

    /** Get a list of tag identifiers for the given task */
    public List<TagIdentifier> getTaskTags(TaskIdentifier
            taskId) throws SQLException {
        List<TagIdentifier> list = new LinkedList<TagIdentifier>();
        Cursor cursor = tagToTaskMapDatabase.query(TAG_TASK_MAP_NAME,
                TagToTaskMapping.FIELD_LIST, TagToTaskMapping.TASK + " = ?",
                new String[] { taskId.idAsString() }, null, null, null);
        activity.startManagingCursor(cursor);

        if(cursor.getCount() == 0)
            return list;
        do {
            cursor.moveToNext();
            list.add(new TagToTaskMapping(cursor).getTag());
        } while(!cursor.isLast());

        return list;
    }

    /** Get a list of task identifiers for the given tag */
    public List<TaskIdentifier> getTaggedTasks(TagIdentifier
            tagId) throws SQLException {
        List<TaskIdentifier> list = new LinkedList<TaskIdentifier>();
        Cursor cursor = tagToTaskMapDatabase.query(TAG_TASK_MAP_NAME,
                TagToTaskMapping.FIELD_LIST, TagToTaskMapping.TAG + " = ?",
                new String[] { tagId.idAsString() }, null, null, null);
        activity.startManagingCursor(cursor);

        if(cursor.getCount() == 0)
            return list;
        do {
            cursor.moveToNext();
            list.add(new TagToTaskMapping(cursor).getTask());
        } while(!cursor.isLast());

        return list;
    }

    // --- single tag operations

    public TagIdentifier createTag(String name) throws SQLException {
        if(name == null)
            throw new NullPointerException("Name can't be null");

        TagModelForView newTag = new TagModelForView(name);
        long row = tagDatabase.insertOrThrow(TAG_TABLE_NAME, AbstractTagModel.NAME,
                newTag.getMergedValues());
        return new TagIdentifier(row);
    }

    /** Creates or saves the given tag */
    public boolean saveTag(AbstractTagModel tag) throws SQLException {
        boolean saveSucessful;

        if(tag.getTagIdentifier() == null) {
            long newRow = tagDatabase.insert(TAG_TABLE_NAME, AbstractTagModel.NAME,
                    tag.getMergedValues());
            tag.setTagIdentifier(new TagIdentifier(newRow));

            saveSucessful = newRow >= 0;
        } else {
            long id = tag.getTagIdentifier().getId();
            saveSucessful = tagDatabase.update(TAG_TABLE_NAME, tag.getSetValues(),
                    KEY_ROWID + "=" + id, null) > 0;
        }

        return saveSucessful;
    }

    /** Returns a TaskModelForView corresponding to the given TaskIdentifier */
    public TagModelForView fetchTagForView(TagIdentifier tagId) throws SQLException {
        long id = tagId.getId();
        Cursor cursor = tagDatabase.query(true, TAG_TABLE_NAME,
                TagModelForView.FIELD_LIST,
                KEY_ROWID + "=" + id, null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            TagModelForView model = new TagModelForView(cursor);
            return model;
        }

        throw new SQLException("Returned empty set!");
    }

    /** Deletes the tag and removes tag/task mappings */
    public boolean deleteTag( TagIdentifier tagId)
            throws SQLException{
        if(tagToTaskMapDatabase.delete(TAG_TASK_MAP_NAME,
                TagToTaskMapping.TAG + " = " + tagId.idAsString(), null) < 0)
            return false;

        return tagDatabase.delete(TAG_TABLE_NAME,
                KEY_ROWID + " = " + tagId.toString(), null) > 0;
    }

    // --- single tag to task operations

    /** Remove the given tag from the task */
    public boolean removeTag(TaskIdentifier taskId, TagIdentifier tagId)
            throws SQLException{
        return tagToTaskMapDatabase.delete(TAG_TASK_MAP_NAME,
                String.format("%s = ? AND %s = ?",
                        TagToTaskMapping.TAG, TagToTaskMapping.TASK),
                new String[] { tagId.idAsString(), taskId.idAsString() }) > 0;
    }

    /** Add the given tag to the task */
    public boolean addTag(TaskIdentifier taskId, TagIdentifier tagId)
            throws SQLException {
        ContentValues values = new ContentValues();
        values.put(TagToTaskMapping.TAG, tagId.getId());
        values.put(TagToTaskMapping.TASK, taskId.getId());
        return tagToTaskMapDatabase.insert(TAG_TASK_MAP_NAME, TagToTaskMapping.TAG,
                values) >= 0;
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public TagController(Activity activity) {
        this.activity = activity;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public TagController open() throws SQLException {
        tagToTaskMapDatabase = new TagToTaskMappingDatabaseHelper(activity,
                TAG_TASK_MAP_NAME, TAG_TASK_MAP_NAME).getWritableDatabase();
        tagDatabase = new TagModelDatabaseHelper(activity,
                TAG_TABLE_NAME, TAG_TABLE_NAME).getWritableDatabase();
        return this;
    }

    /** Closes database resource */
    public void close() {
        tagDatabase.close();
        tagToTaskMapDatabase.close();
    }
}
