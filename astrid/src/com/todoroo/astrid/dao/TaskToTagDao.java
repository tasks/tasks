package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.TaskToTag;

public class TaskToTagDao extends DatabaseDao<TaskToTag> {

    @Autowired
    private Database database;

    public TaskToTagDao() {
        super(TaskToTag.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

}
