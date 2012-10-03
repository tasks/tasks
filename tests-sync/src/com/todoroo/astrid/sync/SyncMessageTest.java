package com.todoroo.astrid.sync;

import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class SyncMessageTest extends NewSyncTestCase {
	
	public void testTaskChangesHappenedConstructor() {
		Task t = createTask();
		try {
			ChangesHappened<Task, TaskOutstanding> changes = new ChangesHappened<Task, TaskOutstanding>(t.getId(), Task.class, taskDao, taskOutstandingDao);
			assertTrue(changes.numChanges() > 0);
			assertFalse(RemoteModel.NO_UUID.equals(changes.getUUID()));
			assertEquals(t.getValue(Task.UUID), changes.getUUID());
		} catch (Exception e) {
			fail("ChangesHappened constructor threw exception " + e);
		}
	}
	
}
