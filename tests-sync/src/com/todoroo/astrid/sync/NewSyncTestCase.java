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
	
	protected Task createTask(String title) {
		Task task = new Task();
		task.setValue(Task.TITLE, title);
		
		taskDao.createNew(task);
		return task;		
	}
	
	protected Task createTask() {
		return createTask("new title");
	}
	
	protected TagData createTagData(String name) {
		TagData tag = new TagData();
		tag.setValue(TagData.NAME, name);
		
		tagDataDao.createNew(tag);
		return tag;
	}
	
	protected TagData createTagData() {
		return createTagData("new tag");
	}
}
