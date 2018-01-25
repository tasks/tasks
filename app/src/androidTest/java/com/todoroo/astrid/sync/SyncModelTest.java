package com.todoroo.astrid.sync;

import android.support.test.runner.AndroidJUnit4;

import org.tasks.data.TagData;
import com.todoroo.astrid.data.Task;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class SyncModelTest extends NewSyncTestCase {

	@Test
	public void testCreateTaskMakesUuid() {
		Task task = createTask();
		assertFalse(Task.NO_UUID.equals(task.getUuid()));
	}

	@Test
	public void testCreateTagMakesUuid() {
		TagData tag = createTagData();
		assertFalse(Task.NO_UUID.equals(tag.getRemoteId()));
	}

}
