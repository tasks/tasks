package com.todoroo.astrid.sync;

import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TagOutstanding;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class SyncModelTest extends NewSyncTestCase {
	
	public void testCreateTaskMakesUuid() {
		Task task = createTask();
		assertFalse(RemoteModel.NO_UUID.equals(task.getValue(Task.UUID)));
		assertFalse(TextUtils.isEmpty(task.getValue(Task.PROOF_TEXT)));
	}

	public void testCreateTagMakesUuid() {
		TagData tag = createTagData();
		assertFalse(RemoteModel.NO_UUID.equals(tag.getValue(TagData.UUID)));
		assertFalse(TextUtils.isEmpty(tag.getValue(TagData.PROOF_TEXT)));		
	}
	
	public void testCreateTaskMakesOutstandingProofText() {
		Task task = createTask();
		TodorooCursor<TaskOutstanding> cursor = taskOutstandingDao.query(
				Query.select(TaskOutstanding.PROPERTIES)
				.where(Criterion.and(TaskOutstanding.TASK_ID.eq(task.getId()),
						TaskOutstanding.COLUMN_STRING.eq(RemoteModel.PROOF_TEXT_PROPERTY.name))));
		try {
			assertTrue(cursor.getCount() > 0);
		} finally {
			cursor.close();
		}
	}
	
	public void testCreateTagMakesOutstandingProofText() {
		TagData tag = createTagData();
		TodorooCursor<TagOutstanding> cursor = tagOutstandingDao.query(
				Query.select(TagOutstanding.PROPERTIES)
				.where(Criterion.and(TagOutstanding.TAG_DATA_ID.eq(tag.getId()),
						TagOutstanding.COLUMN_STRING.eq(RemoteModel.PROOF_TEXT_PROPERTY.name))));
		try {
			assertTrue(cursor.getCount() > 0);
		} finally {
			cursor.close();
		}		
	}
	
	public void testChangeTaskMakesOutstandingEntries() {
		Task task = createTask();
		String newTitle = "changing task title";
		task.setValue(Task.TITLE, newTitle);
		
		taskDao.save(task);
		TodorooCursor<TaskOutstanding> cursor = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES)
				.where(Criterion.and(TaskOutstanding.TASK_ID.eq(task.getId()),
						TaskOutstanding.COLUMN_STRING.eq(Task.TITLE.name),
						TaskOutstanding.VALUE_STRING.eq(newTitle))));
		try {
			assertTrue(cursor.getCount() > 0);
		} finally {
			cursor.close();
		}
	}
	
	public void testChangeTagMakesOutstandingEntries() {
		TagData tag = createTagData();
		String newName = "changing tag name";
		tag.setValue(TagData.NAME, newName);
		
		tagDataDao.saveExisting(tag);
		TodorooCursor<TagOutstanding> cursor = tagOutstandingDao.query(Query.select(TagOutstanding.PROPERTIES)
				.where(Criterion.and(TagOutstanding.TAG_DATA_ID.eq(tag.getId()),
						TagOutstanding.COLUMN_STRING.eq(TagData.NAME.name),
						TagOutstanding.VALUE_STRING.eq(newName))));
		try {
			assertTrue(cursor.getCount() > 0);
		} finally {
			cursor.close();
		}
	}
	
	public void testSuppressionFlagSuppressesOutstandingEntries() {
		Task task = createTask(true);
		TodorooCursor<TagOutstanding> cursor = tagOutstandingDao.query(Query.select(TagOutstanding.PROPERTIES)
				.where(TagOutstanding.TAG_DATA_ID.eq(task.getId())));
		try {
			assertEquals(0, cursor.getCount());
		} finally {
			cursor.close();
		}
		
		task.setValue(Task.TITLE, "new title");
		task.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		taskDao.save(task);
		
		cursor = tagOutstandingDao.query(Query.select(TagOutstanding.PROPERTIES)
				.where(TagOutstanding.TAG_DATA_ID.eq(task.getId())));
		try {
			assertEquals(0, cursor.getCount());
		} finally {
			cursor.close();
		}
	}
}
