package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.UserActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserActivityDao extends RemoteModelDao<UserActivity> {

    @Inject
    public UserActivityDao(Database database) {
        super(UserActivity.class);
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
        if (values == null || values.size() == 0) {
            return false;
        }
        return super.saveExisting(item);
    }

    public void getCommentsForTask(String taskUuid, Callback<UserActivity> callback) {
        query(callback, Query.select(UserActivity.PROPERTIES).where(
                Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT),
                        UserActivity.TARGET_ID.eq(taskUuid),
                        UserActivity.DELETED_AT.eq(0))
        ).orderBy(Order.desc("1")));
    }
}
