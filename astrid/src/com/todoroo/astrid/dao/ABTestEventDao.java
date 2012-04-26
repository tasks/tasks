package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.ABTestEvent;

public class ABTestEventDao extends DatabaseDao<ABTestEvent> {

    @Autowired
    private Database database;

    public ABTestEventDao() {
        super(ABTestEvent.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

}
