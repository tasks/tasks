package com.todoroo.astrid.sync;

import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import javax.inject.Inject;

public class NewSyncTestCase extends DatabaseTestCase {

    @Inject TaskDao taskDao;
	@Inject TagDataDao tagDataDao;

	protected Task createTask(String title) {
		Task task = new Task();
		task.setTitle(title);
		task.setImportance(SYNC_TASK_IMPORTANCE);

        taskDao.createNew(task);
		return task;
	}

	public static final String SYNC_TASK_TITLE = "new title";
	public static final int SYNC_TASK_IMPORTANCE = Task.IMPORTANCE_MUST_DO;

	protected Task createTask() {
		return createTask(SYNC_TASK_TITLE);
	}

	protected TagData createTagData(String name) {
		TagData tag = new TagData();
		tag.setName(name);

		tagDataDao.createNew(tag);
		return tag;
	}

	protected TagData createTagData() {
		return createTagData("new tag");
	}
}
