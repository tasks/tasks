/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.google.common.base.Joiner;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.injection.InjectingContentProvider;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import dagger.Lazy;
import timber.log.Timber;

/**
 * This is the legacy Astrid task provider. While it will continue to be
 * supported, note that it does not expose all of the information in
 * Astrid, nor does it support many editing operations.
 *
 * See the individual methods for a description of what is returned.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Astrid2TaskProvider extends InjectingContentProvider {

	public static final String AUTHORITY = "org.tasks.tasksprovider";

	public static final Uri CONTENT_URI = Uri.parse("content://org.tasks.tasksprovider");

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int MAX_NUMBER_OF_TASKS = 100;

	private final static String NAME = "name";
	private final static String IMPORTANCE_COLOR = "importance_color";
	private final static String IDENTIFIER = "identifier";
	private final static String PREFERRED_DUE_DATE = "preferredDueDate";
	private final static String DEFINITE_DUE_DATE = "definiteDueDate";
	private final static String IMPORTANCE = "importance";
	private final static String ID = "id";

	// fake property for updating that completes a task
	private final static String COMPLETED = "completed";

	private final static String TAGS_ID = "tags_id";

	static String[] TASK_FIELD_LIST = new String[] { NAME, IMPORTANCE_COLOR, PREFERRED_DUE_DATE, DEFINITE_DUE_DATE,
			IMPORTANCE, IDENTIFIER, TAGS_ID };

	static String[] TAGS_FIELD_LIST = new String[] { ID, NAME };

	private static final int URI_TASKS = 0;
	private static final int URI_TAGS = 1;

	private static final String TAG_SEPARATOR = "|";

	@Inject Lazy<TaskService> taskService;
    @Inject Lazy<TagService> tagService;

	static {
		URI_MATCHER.addURI(AUTHORITY, "tasks", URI_TASKS);
		URI_MATCHER.addURI(AUTHORITY, "tags", URI_TAGS);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		super.onCreate();
		return false;
	}

	/**
	 * Note: tag id is no longer a real column, so we pass in a UID
	 * generated from the tag string.
	 *
	 * @return two-column cursor: tag id (string) and tag name
	 */
	public Cursor getTags() {

		TagData[] tags = tagService.get().getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE,
                Criterion.all);

		MatrixCursor ret = new MatrixCursor(TAGS_FIELD_LIST);

        for (TagData tag : tags) {
            Object[] values = new Object[2];
            values[0] = tagNameToLong(tag.getName());
            values[1] = tag.getName();

            ret.addRow(values);
        }

		return ret;
	}

    private long tagNameToLong(String tag) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, e.getMessage());
            return -1;
        }

        m.update(tag.getBytes(), 0, tag.length());
        return new BigInteger(1, m.digest()).longValue();
    }

    /**
	 * Cursor with the following columns
	 * <ol>
	 * <li>task title, string
	 * <li>task importance color, int android RGB color
	 * <li>task due date (was: preferred due date), long millis since epoch
	 * <li>task due date (was: absolute due date), long millis since epoch
	 * <li>task importance, integer from 0 to 3 (0 => most important)
	 * <li>task id, long
	 * <li>task tags, string tags separated by |
	 * </ol>
	 *
	 * @return cursor as described above
	 */
	public Cursor getTasks() {

		MatrixCursor ret = new MatrixCursor(TASK_FIELD_LIST);

		TodorooCursor<Task> cursor = taskService.get().query(Query.select(Task.ID, Task.TITLE,
                Task.IMPORTANCE, Task.DUE_DATE).where(Criterion.and(TaskCriteria.isActive(),
                TaskCriteria.isVisible())).
                orderBy(SortHelper.defaultTaskOrder()).limit(MAX_NUMBER_OF_TASKS));
		try {
    		int[] importanceColors = Task.getImportanceColors(getContext().getResources());
    		for (int i = 0; i < cursor.getCount(); i++) {
    			cursor.moveToNext();
                Task task = new Task(cursor);

                String taskTags = getTagsAsString(task.getId(), TAG_SEPARATOR);

    			Object[] values = new Object[7];
    			values[0] = task.getTitle();
    			values[1] = importanceColors[task.getImportance()];
    			values[2] = task.getDueDate();
    			values[3] = task.getDueDate();
    			values[4] = task.getImportance();
    			values[5] = task.getId();
    			values[6] = taskTags;

    			ret.addRow(values);
    		}
		} finally {
		    cursor.close();
		}

		return ret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor;
		switch (URI_MATCHER.match(uri)) {

		case URI_TASKS:
			cursor = getTasks();
			break;

		case URI_TAGS:
			cursor = getTags();
			break;

		default:
			throw new IllegalStateException("Unrecognized URI:" + uri);
		}

		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
      switch (URI_MATCHER.match(uri)) {

        case URI_TASKS:
            Task task = new Task();

            // map values
            if(values.containsKey(NAME)) {
                task.setTitle(values.getAsString(NAME));
            }
            if(values.containsKey(PREFERRED_DUE_DATE)) {
                task.setDueDate(values.getAsLong(PREFERRED_DUE_DATE));
            }
            if(values.containsKey(DEFINITE_DUE_DATE)) {
                task.setDueDate(values.getAsLong(DEFINITE_DUE_DATE));
            }
            if(values.containsKey(IMPORTANCE)) {
                task.setImportance(values.getAsInteger(IMPORTANCE));
            }
            if(values.containsKey(COMPLETED)) {
                task.setCompletionDate(values.getAsBoolean(COMPLETED) ? DateUtilities.now() : 0);
            }

            // map selection criteria
            String criteria = selection.replace(NAME, Task.TITLE.name).
                replace(PREFERRED_DUE_DATE, Task.DUE_DATE.name).
                replace(DEFINITE_DUE_DATE, Task.DUE_DATE.name).
                replace(IDENTIFIER, Task.ID.name).
                replace(ID, Task.ID.name).
                replace(IMPORTANCE, Task.IMPORTANCE.name);

            return taskService.get().updateBySelection(criteria, selectionArgs, task);

        case URI_TAGS:
            throw new UnsupportedOperationException("tags updating: not yet");

        default:
            throw new IllegalStateException("Unrecognized URI:" + uri);
        }
	}

	public static void notifyDatabaseModification(Context context) {
		try {
		    context.getContentResolver().notifyChange(CONTENT_URI, null);
		} catch (Exception e) {
            Timber.e(e, e.getMessage());
		}
	}

    /**
     * Return tags as a list of strings separated by given separator
     * @return empty string if no tags, otherwise string
     */
    private String getTagsAsString(long taskId, String separator) {
        return Joiner.on(separator).join(tagService.get().getTagNames(taskId));
    }
}
