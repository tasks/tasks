package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.astrid.data.OutstandingEntry;

public class OutstandingEntryDao<TYPE extends OutstandingEntry<?>> extends DatabaseDao<TYPE> {

    public OutstandingEntryDao(Class<TYPE> modelClass) {
        super(modelClass);
    }

}
