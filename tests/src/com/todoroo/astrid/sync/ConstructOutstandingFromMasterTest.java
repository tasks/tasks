package com.todoroo.astrid.sync;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.messages.ConstructTaskOutstandingTableFromMasterTable;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.SyncFlags;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;
import com.todoroo.astrid.tags.TaskToTagMetadata;

public class ConstructOutstandingFromMasterTest extends NewSyncTestCase {

	public void testConstructOutstandingConstructsOutstanding() {
		Task t = createTask(true);
		Metadata m = TaskToTagMetadata.newTagMetadata(t.getId(), t.getUuid(), "Tag", "2");
		m.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		metadataDao.createNew(m);
		TodorooCursor<TaskOutstanding> to = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES));
		try {
			assertEquals(0, to.getCount());
		} finally {
			to.close();
		}

		new ConstructTaskOutstandingTableFromMasterTable(NameMaps.TABLE_ID_TASKS, taskDao, taskOutstandingDao, metadataDao, Task.CREATION_DATE).execute();

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

		to = taskOutstandingDao.query(Query.select(TaskOutstanding.PROPERTIES).where(TaskOutstanding.COLUMN_STRING.eq(NameMaps.TAG_ADDED_COLUMN)));
		try {
		    assertEquals(1, to.getCount());
		    to.moveToFirst();
		    assertEquals("2", to.get(TaskOutstanding.VALUE_STRING));
		} finally {
		    to.close();
		}
	}

}
