package com.timsu.astrid.provider;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskModelForProvider;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class TasksProvider extends ContentProvider {

    static {
        AstridDependencyInjector.initialize();
    }

	private static final String TAG = "MessageProvider";

	private static final boolean LOGD = false;


	public static final String AUTHORITY = "com.timsu.astrid.tasksprovider";

	public static final Uri CONTENT_URI = Uri.parse("content://com.timsu.astrid.tasksprovider");

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int MAX_NUMBEER_OF_TASKS = 30;

	private final static String NAME = "name";
	private final static String IMPORTANCE_COLOR = "importance_color";
	private final static String IDENTIFIER = "identifier";
	private final static String PREFERRED_DUE_DATE = "preferredDueDate";
	private final static String DEFINITE_DUE_DATE = "definiteDueDate";
	private final static String IMPORTANCE = "importance";
	private final static String ID = "id";

	// fake property for updatu=ing that completes a task
	private final static String COMPLETED = "completed";

	private final static String TAGS_ID = "tags_id";

	static String[] TASK_FIELD_LIST = new String[] { NAME, IMPORTANCE_COLOR, PREFERRED_DUE_DATE, DEFINITE_DUE_DATE,
			IMPORTANCE, IDENTIFIER, TAGS_ID };

	static String[] TAGS_FIELD_LIST = new String[] { ID, NAME };

	private static final int URI_TASKS = 0;
	private static final int URI_TAGS = 1;

	private static final String TAG_SEPARATOR = "|";

	private static Context ctx = null;

	static {
		URI_MATCHER.addURI(AUTHORITY, "tasks", URI_TASKS);
		URI_MATCHER.addURI(AUTHORITY, "tags", URI_TAGS);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGD)
			Log.d(TAG, "delete");

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
		ctx = getContext();
		return false;
	}

	public Cursor getTags() {

		LinkedList<TagModelForView> tags = null;

		TagController tagController = new TagController(ctx);
		tagController.open();
		tags = tagController.getAllTags();
		tagController.close();

		MatrixCursor ret = new MatrixCursor(TAGS_FIELD_LIST);

		for (int i = 0; i < tags.size(); i++) {
			Object[] values = new Object[2];
			values[0] = tags.get(i).getTagIdentifier().getId();
			values[1] = tags.get(i).getName();

			ret.addRow(values);
		}

		return ret;
	}

	public Cursor getTasks() {

		int numberOfTasks = MAX_NUMBEER_OF_TASKS;

		TaskController taskController = new TaskController(ctx);
		taskController.open();
		ArrayList<TaskModelForProvider> taskList = taskController.getTasksForProvider(Integer.toString(numberOfTasks));
		taskController.close();

		MatrixCursor ret = new MatrixCursor(TASK_FIELD_LIST);

		for (int i = 0; i < taskList.size(); i++) {
			TaskModelForProvider taskModel = taskList.get(i);

			if (taskModel != null) {

				// get prefered due date time
				long preferredDueDateTime = 0;
				if (taskModel.getPreferredDueDate() != null)
					preferredDueDateTime = taskModel.getPreferredDueDate().getTime();

				// get definite due date time
				long definiteDueDate = 0;
				if (taskModel.getDefiniteDueDate() != null)
					definiteDueDate = taskModel.getDefiniteDueDate().getTime();

				// get tags for task
				LinkedList<TagIdentifier> tags = null;
				TagController tagController = new TagController(ctx);
				tagController.open();
				tags = tagController.getTaskTags(taskModel.getTaskIdentifier());
				String taskTags = "";
				for (TagIdentifier tag : tags) {
					if (taskTags.equals(""))
						taskTags = Long.toString(tag.getId());
					else
						taskTags = taskTags + TAG_SEPARATOR + Long.toString(tag.getId());
				}
				tagController.close();

				Object[] values = new Object[7];
				values[0] = taskModel.getName();
				values[1] = ctx.getResources().getColor(taskModel.getImportance().getColorResource());
				values[2] = preferredDueDateTime;
				values[3] = definiteDueDate;
				values[4] = taskModel.getImportance().ordinal();
				values[5] = taskModel.getTaskIdentifier().getId();
				values[6] = taskTags;

				ret.addRow(values);

			}
		}

		return ret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		if (LOGD)
			Log.d(TAG, "query");

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

		if (LOGD)
			Log.d(TAG, "update");

      switch (URI_MATCHER.match(uri)) {

        case URI_TASKS:
            int updated = 0;

            // handle the "completed" value separately
            if(values.containsKey(COMPLETED)) {
                boolean completed = values.getAsBoolean(COMPLETED);
                values.remove(COMPLETED);
                values.put(TaskModelForProvider.PROGRESS_PERCENTAGE,
                        completed ? TaskModelForProvider.COMPLETE_PERCENTAGE : 0);
            }

            TaskController taskController = new TaskController(ctx);
            taskController.open();
            Cursor c = taskController.getMatchingTasksForProvider(selection, selectionArgs);
            for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                TaskModelForProvider model = new TaskModelForProvider(c);
                model.update(values);
                taskController.saveTask(model, false);
                updated++;
            }
            taskController.close();
            return updated;

        case URI_TAGS:
            throw new UnsupportedOperationException("tags updating: not yet");

        default:
            throw new IllegalStateException("Unrecognized URI:" + uri);
        }
	}

	public static void notifyDatabaseModification() {

		if (LOGD)
			Log.d(TAG, "notifyDatabaseModification");

		ctx.getContentResolver().notifyChange(CONTENT_URI, null);
	}

}
