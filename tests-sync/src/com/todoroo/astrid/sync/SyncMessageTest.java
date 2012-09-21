package com.todoroo.astrid.sync;

import com.todoroo.astrid.actfm.sync.messages.ChangesHappened;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskOutstanding;

public class SyncMessageTest extends NewSyncTestCase {
	
	public void testTaskChangesHappenedConstructor() {
		Task t = createTask();
		try {
			ChangesHappened<Task, TaskOutstanding> changes = new ChangesHappened<Task, TaskOutstanding>(t, taskDao, taskOutstandingDao);
			assertTrue(changes.getChanges().size() > 0);
		} catch (Exception e) {
			fail("ChangesHappened constructor threw exception " + e);
		}
	}
	
}
