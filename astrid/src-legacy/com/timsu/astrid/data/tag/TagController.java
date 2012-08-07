/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.tag.AbstractTagModel.TagModelDatabaseHelper;
import com.timsu.astrid.data.tag.TagToTaskMapping.TagToTaskMappingDatabaseHelper;
import com.timsu.astrid.data.task.AbstractTaskModel.TaskModelDatabaseHelper;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.todoroo.astrid.provider.Astrid2TaskProvider;

/** Controller for Tag-related operations */
@SuppressWarnings("nls")
@Deprecated
public class TagController extends LegacyAbstractController {

    private SQLiteDatabase tagDatabase, tagToTaskMapDatabase;

    // --- tag batch operations

    /** Get a list of all tags */
    public LinkedList<TagModelForView> getAllTags()
            throws SQLException {
        LinkedList<TagModelForView> list = new LinkedList<TagModelForView>();
        Cursor cursor = tagDatabase.query(tagsTable,
            TagModelForView.FIELD_LIST, null, null, null, null, null, null);

        try {
	        if(cursor.getCount() == 0)
	            return list;
	        do {
	            cursor.moveToNext();
	            list.add(new TagModelForView(cursor));
	        } while(!cursor.isLast());
        } finally {
        	cursor.close();
        }

        return list;
    }

    // --- tag to task map batch operations

    /** Get a list of all tags as an id => tag map */
    public HashMap<TagIdentifier, TagModelForView> getAllTagsAsMap() throws SQLException {
        HashMap<TagIdentifier, TagModelForView> map = new HashMap<TagIdentifier, TagModelForView>();
        for(TagModelForView tag : getAllTags())
            map.put(tag.getTagIdentifier(), tag);
        return map;
    }

    /** Get a list of tag identifiers for the given task */
    public LinkedList<TagIdentifier> getTaskTags(TaskIdentifier
            taskId) throws SQLException {
        LinkedList<TagIdentifier> list = new LinkedList<TagIdentifier>();
        Cursor cursor = tagToTaskMapDatabase.query(tagTaskTable,
                TagToTaskMapping.FIELD_LIST, TagToTaskMapping.TASK + " = ?",
                new String[] { taskId.idAsString() }, null, null, null);

        try {
	        if(cursor.getCount() == 0)
	            return list;
	        do {
	            cursor.moveToNext();
	            list.add(new TagToTaskMapping(cursor).getTag());
	        } while(!cursor.isLast());
        } finally {
        	cursor.close();
        }

        return list;
    }

    /** Get a list of task identifiers for the given tag.
     * This searches for TAGGED tasks only.
     * Use getUntaggedTasks() to get a list of UNTAGGED tasks **/
    public LinkedList<TaskIdentifier> getTaggedTasks(TagIdentifier tagId)
    		throws SQLException {
        LinkedList<TaskIdentifier> list = new LinkedList<TaskIdentifier>();
        Cursor cursor = tagToTaskMapDatabase.query(tagTaskTable,
                TagToTaskMapping.FIELD_LIST, TagToTaskMapping.TAG + " = ?",
                new String[] { tagId.idAsString() }, null, null, null);

        try {
	        if(cursor.getCount() == 0)
	            return list;
	        do {
	            cursor.moveToNext();
	            list.add(new TagToTaskMapping(cursor).getTask());
	        } while(!cursor.isLast());
        } finally {
        	cursor.close();
        }

        return list;
    }

    /** Returns a list of task identifiers in the provided set that are UNtagged.
     *
     * The calling SubActivity must provide the set of tasks, since
     * TagController cannot access the appropriate instance of TaskController.
     *
     * The current implementation is not very efficient, because queries
     * the TagToTask map once for each active task.
     **/
    public LinkedList<TaskIdentifier> getUntaggedTasks() throws SQLException {
    	HashSet<Long> ids = new HashSet<Long>();

    	String[] tagMapColumns = new String[] { TagToTaskMapping.TASK };
    	Cursor tagMapCursor = tagToTaskMapDatabase.query(tagTaskTable,
    			tagMapColumns, null, null, TagToTaskMapping.TASK, null,
    			TagToTaskMapping.TASK + " ASC");

    	SQLiteDatabase taskDatabase = new TaskModelDatabaseHelper(context,
    			tasksTable, tasksTable).getReadableDatabase();
    	String[] taskColumns = new String[] { KEY_ROWID };
    	Cursor taskCursor = taskDatabase.query(tasksTable, taskColumns,
    			null, null, null, null, KEY_ROWID + " ASC");

    	LinkedList<TaskIdentifier> list = new LinkedList<TaskIdentifier>();
        try {
        	if(taskCursor.getCount() == 0)
        		return list;

        	do {
        		taskCursor.moveToNext();
                ids.add(taskCursor.getLong(0));
            } while(!taskCursor.isLast());

        	if(tagMapCursor.getCount() > 0) {
        		do {
        			tagMapCursor.moveToNext();
                    ids.remove(tagMapCursor.getLong(0));
                } while(!tagMapCursor.isLast());
        	}
        } finally {
        	taskCursor.close();
        	tagMapCursor.close();
        	taskDatabase.close();
        }

        for(Long id : ids)
        	list.add(new TaskIdentifier(id));
    	return list;
    }


    // --- single tag operations

    public TagIdentifier createTag(String name) throws SQLException {
        if(name == null)
            throw new NullPointerException("Name can't be null");

        TagModelForView newTag = new TagModelForView(name);
        long row = tagDatabase.insertOrThrow(tagsTable, AbstractTagModel.NAME,
                newTag.getMergedValues());
        return new TagIdentifier(row);
    }

    /** Creates or saves the given tag */
    public boolean saveTag(AbstractTagModel tag) throws SQLException {
        boolean saveSucessful;

        if(tag.getTagIdentifier() == null) {
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

    /** Returns a TaskModelForView corresponding to the given Tag Name */
    public TagModelForView fetchTagFromName(String name) throws SQLException {
        Cursor cursor = tagDatabase.query(true, tagsTable,
                TagModelForView.FIELD_LIST,
                AbstractTagModel.NAME + " = ?", new String[] {name}, null, null, null, null);

        try {
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                TagModelForView model = new TagModelForView(cursor);
                return model;
            }
            return null;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /** Returns a TaskModelForView corresponding to the given TagIdentifier */
    public TagModelForView fetchTagForView(TagIdentifier tagId) throws SQLException {
        long id = tagId.getId();
        Cursor cursor = tagDatabase.query(true, tagsTable,
                TagModelForView.FIELD_LIST,
                KEY_ROWID + "=" + id, null, null, null, null, null);

        try {
            if (cursor != null) {
                cursor.moveToFirst();
                TagModelForView model = new TagModelForView(cursor);
                return model;
            }

            throw new SQLException("Returned empty set!");
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    /** Deletes the tag and removes tag/task mappings */
    public boolean deleteTag( TagIdentifier tagId)
            throws SQLException{
        if(tagToTaskMapDatabase.delete(tagTaskTable,
                TagToTaskMapping.TAG + " = " + tagId.idAsString(), null) < 0)
            return false;

        int res = tagDatabase.delete(tagsTable,
                KEY_ROWID + " = " + tagId.idAsString(), null);

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

        return res > 0;
    }

    // --- single tag to task operations

    /** Remove the given tag from the task */
    public boolean removeTag(TaskIdentifier taskId, TagIdentifier tagId)
            throws SQLException{

    	int res = tagToTaskMapDatabase.delete(tagTaskTable,
                String.format("%s = ? AND %s = ?",
                        TagToTaskMapping.TAG, TagToTaskMapping.TASK),
                new String[] { tagId.idAsString(), taskId.idAsString() });

        // notify modification
        Astrid2TaskProvider.notifyDatabaseModification();

    	return res > 0;
    }

    /** Add the given tag to the task */
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
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    @Override
    public synchronized void open() throws SQLException {
        tagToTaskMapDatabase = new TagToTaskMappingDatabaseHelper(context,
                tagTaskTable, tagTaskTable).getWritableDatabase();
        tagDatabase = new TagModelDatabaseHelper(context,
                tagsTable, tagsTable).getWritableDatabase();
    }

    /** Closes database resource */
    @Override
    public void close() {
        if(tagDatabase != null)
            tagDatabase.close();
        if(tagToTaskMapDatabase != null)
            tagToTaskMapDatabase.close();
    }
}
