package com.todoroo.astrid.sync;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagOutstandingDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskOutstandingDao;
import com.todoroo.astrid.data.SyncFlags;
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
	
	protected Task createTask(String title, boolean suppress) {
		Task task = new Task();
		task.setValue(Task.TITLE, title);
		
		if (suppress)
			task.putTransitory(SyncFlags.ACTFM_SUPPRESS_OUTSTANDING_ENTRIES, true);
		taskDao.createNew(task);
		return task;		
	}
	
	protected Task createTask() {
		return createTask(false);
	}
	
	protected Task createTask(boolean suppress) {
		return createTask("new title", suppress);
	}
	
	protected TagData createTagData(String name, boolean suppress) {
		TagData tag = new TagData();
		tag.setValue(TagData.NAME, name);
		
		tagDataDao.createNew(tag);
		return tag;
	}
	
	protected TagData createTagData() {
		return createTagData(false);
	}
	
	protected TagData createTagData(boolean suppress) {
		return createTagData("new tag", suppress);
	}
}
