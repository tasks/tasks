package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.UserActivity;

public class UserActivityDao extends RemoteModelDao<UserActivity> {

    @Autowired
    private Database database;

    public UserActivityDao() {
        super(UserActivity.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    public boolean createNew(UserActivity item) {
        if (!item.containsValue(UserActivity.CREATED_AT)) {
            item.setCreatedAt(DateUtilities.now());
        }
        return super.createNew(item);
    }

    @Override
    public boolean saveExisting(UserActivity item) {
        ContentValues values = item.getSetValues();
        if(values == null || values.size() == 0) {
            return false;
        }
        return super.saveExisting(item);
    }
}
