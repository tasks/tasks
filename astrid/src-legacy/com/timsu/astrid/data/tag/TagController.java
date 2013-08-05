/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.tag;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.tag.AbstractTagModel.TagModelDatabaseHelper;
import com.timsu.astrid.data.tag.TagToTaskMapping.TagToTaskMappingDatabaseHelper;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.todoroo.astrid.provider.Astrid2TaskProvider;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Controller for Tag-related operations
 */

@Deprecated
public class TagController extends LegacyAbstractController {

    private SQLiteDatabase tagDatabase, tagToTaskMapDatabase;

    // --- tag batch operations

    /**
     * Get a list of all tags
     */
    public LinkedList<TagModelForView> getAllTags()
            throws SQLException {
        LinkedList<TagModelForView> list = new LinkedList<TagModelForView>();
        Cursor cursor = tagDatabase.query(tagsTable,
                TagModelForView.FIELD_LIST, null, null, null, null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            do {
                cursor.moveToNext();
                list.add(new TagModelForView(cursor));
            } while (!cursor.isLast());
        } finally {
            cursor.close();
        }

        return list;
    }

    // --- tag to task map batch operations

    /**
     * Get a list of all tags as an id => tag map
     */
    public HashMap<TagIdentifier, TagModelForView> getAllTagsAsMap() throws SQLException {
        HashMap<TagIdentifier, TagModelForView> map = new HashMap<TagIdentifier, TagModelForView>();
        for (TagModelForView tag : getAllTags()) {
            map.put(tag.getTagIdentifier(), tag);
        }
        return map;
    }

    /**
     * Get a list of tag identifiers for the given task
     */
    public LinkedList<TagIdentifier> getTaskTags(TaskIdentifier
                                                         taskId) throws SQLException {
        LinkedList<TagIdentifier> list = new LinkedList<TagIdentifier>();
        Cursor cursor = tagToTaskMapDatabase.query(tagTaskTable,
                TagToTaskMapping.FIELD_LIST, TagToTaskMapping.TASK + " = ?",
                new String[]{taskId.idAsString()}, null, null, null);

        try {
            if (cursor.getCount() == 0) {
                return list;
            }
            do {
                cursor.moveToNext();
                list.add(new TagToTaskMapping(cursor).getTag());
            } while (!cursor.isLast());
        } finally {
            cursor.close();
        }

        return list;
    }

    // --- single tag operations

    public TagIdentifier createTag(String name) throws SQLException {
        if (name == null) {
            throw new NullPointerException("Name can't be null");
        }

        TagModelForView newTag = new TagModelForView(name);
        long row = tagDatabase.insertOrThrow(tagsTable, AbstractTagModel.NAME,
                newTag.getMergedValues());
        return new TagIdentifier(row);
    }

    /**
     * Creates or saves the given tag
     */
    public boolean saveTag(AbstractTagModel tag) throws SQLException {
        boolean saveSucessful;

        if (tag.getTagIdentifier() == null) {
            long newRow = tagDatabase.insert(tagsTable, AbstractTagModel.NAME,
                    tag.getMergedValues());
            tag.setTagIdentifier(new TagIdentifier(newRow));

            saveSucessful = newRow >= 0;
        } else {
            long id = tag.getTagIdentifier().getId();
            saveSucessful = tagDatabase.update(tagsTable, tag.getSetValues(),
                    KEY_ROWID + "=" + id, null) > 0;
        }

        return saveSucessful;
    }

    /**
     * Deletes the tag and removes tag/task mappings
     */
    public boolean deleteTag(TagIdentifier tagId)
            throws SQLException {
        if (tagToTaskMapDatabase.delete(tagTaskTable,
                TagToTaskMapping.TAG + " = " + tagId.idAsString(), null) < 0) {
            return false;
        }

        int res = tagDatabase.delete(tagsTable,
                KEY_ROWID + " = " + tagId.idAsString(), null);

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return res > 0;
    }

    // --- single tag to task operations

    /**
     * Remove the given tag from the task
     */
    public boolean removeTag(TaskIdentifier taskId, TagIdentifier tagId)
            throws SQLException {

        int res = tagToTaskMapDatabase.delete(tagTaskTable,
                String.format("%s = ? AND %s = ?",
                        TagToTaskMapping.TAG, TagToTaskMapping.TASK),
                new String[]{tagId.idAsString(), taskId.idAsString()});

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return res > 0;
    }

    /**
     * Add the given tag to the task
     */
    public boolean addTag(TaskIdentifier taskId, TagIdentifier tagId)
            throws SQLException {
        ContentValues values = new ContentValues();
        values.put(TagToTaskMapping.TAG, tagId.getId());
        values.put(TagToTaskMapping.TASK, taskId.getId());

        long res = tagToTaskMapDatabase.insert(tagTaskTable, TagToTaskMapping.TAG,
                values);

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return res >= 0;
    }

    // --- boilerplate

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     */
    public TagController(Context context) {
        super(context);
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     *
     * @return this (self reference, allowing this to be chained in an
     * initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Override
    public synchronized void open() throws SQLException {
        tagToTaskMapDatabase = new TagToTaskMappingDatabaseHelper(context,
                tagTaskTable, tagTaskTable).getWritableDatabase();
        tagDatabase = new TagModelDatabaseHelper(context,
                tagsTable, tagsTable).getWritableDatabase();
    }

    /**
     * Closes database resource
     */
    @Override
    public void close() {
        if (tagDatabase != null) {
            tagDatabase.close();
        }
        if (tagToTaskMapDatabase != null) {
            tagToTaskMapDatabase.close();
        }
    }
}
