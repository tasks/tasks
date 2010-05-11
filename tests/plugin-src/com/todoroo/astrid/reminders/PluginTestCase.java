package com.todoroo.astrid.reminders;

import java.util.HashMap;
import java.util.Map.Entry;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class PluginTestCase extends DatabaseTestCase {

    @Autowired
    public TaskDao taskDao;

    @Autowired
    public MetadataDao metadataDao;

    /**
     * Helper method to create a task.
     * @param task
     * @param metadata
     * @return task id
     */
    public long createTask(Task task, HashMap<String, String> metadata) {
        taskDao.save(database, task, false);

        if(metadata != null) {
            Metadata metadataItem = new Metadata();
            metadataItem.setValue(Metadata.TASK, task.getId());
            for(Entry<String, String> entry : metadata.entrySet()) {
                metadataItem.setValue(Metadata.KEY, entry.getKey());
                metadataItem.setValue(Metadata.VALUE, entry.getValue());
                metadataDao.save(database, metadataItem);
            }
        }

        return task.getId();
    }

}
