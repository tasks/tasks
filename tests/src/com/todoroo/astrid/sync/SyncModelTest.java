package com.todoroo.astrid.sync;

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
	}

	public void testCreateTagMakesUuid() {
		TagData tag = createTagData();
		assertFalse(RemoteModel.NO_UUID.equals(tag.getValue(TagData.UUID)));	
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
	
	public void testUpdateMakesAllOutstandingEntries() {
		String title = "Task Important";
		createTask(title, true);
		createTask("Task Not Important", true);
		createTask(title, true);
		
		Task template = new Task();
		template.setValue(Task.IMPORTANCE, Task.IMPORTANCE_DO_OR_DIE);
		
		taskDao.update(Task.TITLE.eq(title), template);
		
		TodorooCursor<TaskOutstanding> cursor = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES));
		try {
			assertEquals(2, cursor.getCount());
			TaskOutstanding to = new TaskOutstanding();
			for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
				to.readPropertiesFromCursor(cursor);
				assertEquals(Task.IMPORTANCE.name, to.getValue(TaskOutstanding.COLUMN_STRING));
				assertEquals(Task.IMPORTANCE_DO_OR_DIE, Integer.parseInt(to.getValue(TaskOutstanding.VALUE_STRING)));
			}
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
