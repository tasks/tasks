package com.todoroo.astrid.sync;

import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

public class SyncModelTest extends NewSyncTestCase {
	
	public void testCreateTaskMakesUuid() {
		Task task = createTask();
		assertFalse(RemoteModel.NO_UUID.equals(task.getUUID()));
	}

	public void testCreateTagMakesUuid() {
		TagData tag = createTagData();
		assertFalse(RemoteModel.NO_UUID.equals(tag.getUUID()));	
	}

}
