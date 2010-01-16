package com.timsu.astrid.provider;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskModelForProvider;

public class TasksProvider extends ContentProvider {

	private static final String TAG = "MessageProvider";

	public static final String AUTHORITY = "com.timsu.astrid.tasksprovider";

	public static final Uri CONTENT_URI = Uri.parse("content://com.timsu.astrid.tasksprovider");

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	private static final int MAX_NUMBEER_OF_TASKS = 20;

	private final static String IMPORTANCE_COLOR = "importance_color";
	private final static String IDENTIFIER = "identifier";

	static String[] TASK_FIELD_LIST = new String[] { AbstractTaskModel.NAME, IMPORTANCE_COLOR,
			AbstractTaskModel.PREFERRED_DUE_DATE, AbstractTaskModel.DEFINITE_DUE_DATE, AbstractTaskModel.IMPORTANCE, IDENTIFIER };

	private static final int URI_TASKS = 0;
	// private static final int URI_MESSAGES = 1;
	// private static final int URI_MESSAGE = 2;
	// private static final int URI_FOLDERS = 3;

	private static Context ctx = null;

	static {
		URI_MATCHER.addURI(AUTHORITY, "tasks", URI_TASKS);
		// URI_MATCHER.addURI(AUTHORITY, "messages/*", URI_MESSAGES);
		// URI_MATCHER.addURI(AUTHORITY, "message/*", URI_MESSAGE);
		// URI_MATCHER.addURI(AUTHORITY, "folders/*", URI_FOLDERS);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.d(TAG, "delete");

		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// can only delete a message

		// List<String> segments = null;
		// String emailAccount = null;
		// String msgId = null;
		// String msgUId = null;
		//
		// segments = uri.getPathSegments();
		// emailAccount = segments.get(1);
		// msgId = segments.get(2);
		//
		//
		// openOrReopenDatabase(emailAccount);
		//
		// // get messages uid
		// Cursor cursor = null;
		// try {
		// cursor = getAllMessages(null, "( id = " + msgId + " )", null, null);
		// if (cursor != null) {
		// cursor.moveToFirst();
		// msgUId = cursor.getString(cursor.getColumnIndex("uid"));
		// cursor.close();
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// // get localstore parameter
		// Message msg = null;
		// try {
		// Folder lf = LocalStore.getInstance(myAccount.getLocalStoreUri(),
		// mApp, null).getFolder("INBOX");
		// int msgCount = lf.getMessageCount();
		// Log.d(TAG, "folder msg count = " + msgCount);
		// msg = lf.getMessage(msgUId);
		// } catch (MessagingException e) {
		// e.printStackTrace();
		// }
		//
		// // launch command to delete the message
		// if ((myAccount != null) && (msg != null)) {
		// MessagingController.getInstance(mApp).deleteMessage(myAccount,
		// "INBOX", msg, null);
		// }
		//
		// notifyDatabaseModification();

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

		// TaskController taskController = new TaskController(ctx);
		// taskController.ge
		//
		// MatrixCursor ret = new MatrixCursor(TASK_FIELD_LIST);
		//
		// for (int i = 0; i < taskList.size(); i++) {
		// }

		return null;
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

				Object[] values = new Object[6];
				values[0] = taskModel.getName();
				values[1] = ctx.getResources().getColor(taskModel.getImportance().getColorResource());
				values[2] = preferredDueDateTime;
				values[3] = definiteDueDate;
				values[4] = taskModel.getImportance().ordinal();
				values[5] = taskModel.getTaskIdentifier().getId();

				ret.addRow(values);

			}
		}

		return ret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		Log.d(TAG, "query");

		Cursor cursor;
		switch (URI_MATCHER.match(uri)) {
		// case URI_MESSAGES:
		// segments = uri.getPathSegments();
		// emailAccount = segments.get(1);
		//
		// openOrReopenDatabase(emailAccount);
		//
		// cursor = getAllMessages(projection, selection, selectionArgs,
		// sortOrder);
		// break;

		case URI_TASKS:
			cursor = getTasks();
			break;

		// case URI_FOLDERS:
		// segments = uri.getPathSegments();
		// emailAccount = segments.get(1);
		//
		// openOrReopenDatabase(emailAccount);
		//
		// cursor = getFolders(projection, selection, selectionArgs, sortOrder);
		// break;

		default:
			throw new IllegalStateException("Unrecognized URI:" + uri);
		}

		return cursor;
	}

	// private void openOrReopenDatabase(String emailAccount) {
	//
	// String dbPath = null;
	//
	// if ((!emailAccount.equals(mCurrentEmailAccount)) || (mDb == null)) {
	//
	// // look at existing accounts
	// for (Account account :
	// Preferences.getPreferences(getContext()).getAccounts()) {
	// if (account.getEmail().equals(emailAccount)) {
	// dbPath = account.getLocalStoreUri();
	// }
	// }
	//
	// if (dbPath != null) {
	//
	// // save this account as current account
	// mCurrentEmailAccount = emailAccount;
	//
	// // close old database
	// if (mDb != null)
	// mDb.close();
	//
	// // open database
	// String path = Uri.parse(dbPath).getPath();
	// mDb = SQLiteDatabase.openDatabase(path, null,
	// SQLiteDatabase.OPEN_READONLY);
	// }
	// }
	//
	// }

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		Log.d(TAG, "update");

		// // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// // can only set flag to 'SEEN'
		//
		// List<String> segments = null;
		// String emailAccount = null;
		// String msgId = null;
		// String msgUId = null;
		//
		// segments = uri.getPathSegments();
		// emailAccount = segments.get(1);
		// msgId = segments.get(2);
		//
		// openOrReopenDatabase(emailAccount);
		//
		// // get account parameters
		// Account myAccount = null;
		// for (Account account :
		// Preferences.getPreferences(getContext()).getAccounts()) {
		// if (emailAccount.equals(account.getEmail())) {
		// myAccount = account;
		// }
		// }
		//
		// // get messages uid
		// Cursor cursor = null;
		// try {
		// cursor = getAllMessages(null, "( id = " + msgId + " )", null, null);
		// if (cursor != null) {
		// cursor.moveToFirst();
		// msgUId = cursor.getString(cursor.getColumnIndex("uid"));
		// cursor.close();
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// // launch command to delete the message
		// if ((myAccount != null) && (msgUId != null)) {
		// MessagingController.getInstance(mApp).markMessageRead(myAccount,
		// "INBOX", msgUId, true);
		// }
		//
		// notifyDatabaseModification();

		return 0;
	}

	public static void notifyDatabaseModification() {

		Log.d(TAG, "UPDATE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		ctx.getContentResolver().notifyChange(CONTENT_URI, null);

	}

}
