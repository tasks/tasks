package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.History;

public class HistoryDao extends DatabaseDao<History> {

    @Autowired
    private Database database;

    public HistoryDao() {
        super(History.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    public boolean createNew(History item) {
        if (!item.containsValue(History.CREATED_AT))
            item.setValue(History.CREATED_AT, DateUtilities.now());
        return super.createNew(item);
    }

    @Override
    public boolean saveExisting(History item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0)
            return false;
        return super.saveExisting(item);
    }
}
