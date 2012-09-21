package com.todoroo.astrid.sync;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class NewSyncTestCase extends DatabaseTestCase {
	@Autowired 
	protected TaskDao taskDao;
	@Autowired 
	protected TagDataDao tagDataDao;
	
	@Autowired 
	protected TaskOutstandingDao taskOutstandingDao;
	@Autowired 
	protected 
	TagOutstandingDao tagOutstandingDao;
	
	protected Task createTask() {
		Task task = new Task();
		task.setValue(Task.TITLE, "new task");
		
		taskDao.createNew(task);
		return task;
	}
	
	protected TagData createTagData() {
		TagData tag = new TagData();
		tag.setValue(TagData.NAME, "new tag");
		
		tagDataDao.createNew(tag);
		return tag;
	}
}
