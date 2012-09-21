package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.OutstandingEntry;

public class OutstandingEntryDao<TYPE extends OutstandingEntry<?>> extends DatabaseDao<TYPE> {

    @Autowired
    private Database database;

    public OutstandingEntryDao(Class<TYPE> modelClass) {
        super(modelClass);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

}
