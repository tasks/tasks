package com.todoroo.astrid.sync;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.messages.ConstructOutstandingTableFromMasterTable;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class ConstructOutstandingFromMasterTest extends NewSyncTestCase {

	public void testConstructOutstandingConstructsOutstanding() {
		Task t = createTask(true);
		TodorooCursor<TaskOutstanding> to = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES));
		try {
			assertEquals(0, to.getCount());
		} finally {
			to.close();
		}
		
		new ConstructOutstandingTableFromMasterTable<Task, TaskOutstanding>(NameMaps.TABLE_ID_TASKS, taskDao, taskOutstandingDao, Task.CREATION_DATE).execute();
		
		Property<?>[] syncable = NameMaps.syncableProperties(NameMaps.TABLE_ID_TASKS);
		for (Property<?> p : syncable) {
			to = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES).where(TaskOutstanding.COLUMN_STRING.eq(p.name)));
			try {
				assertEquals(1, to.getCount());
				to.moveToFirst();
				String value = t.getValue(p).toString();
				assertEquals(value, to.get(TaskOutstanding.VALUE_STRING));
			} finally {
				to.close();
			}
		}
	}
	
}
