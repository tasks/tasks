package com.todoroo.astrid.dao;

import android.content.ContentValues;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.UserActivity;

import javax.inject.Inject;

public class UserActivityDao {

    private final RemoteModelDao<UserActivity> dao;

    @Inject
    public UserActivityDao(Database database) {
        dao = new RemoteModelDao<>(database, UserActivity.class);
    }

    public void createNew(UserActivity item) {
        if (!item.containsValue(UserActivity.CREATED_AT)) {
            item.setCreatedAt(DateUtilities.now());
        }
        dao.createNew(item);
    }

    public boolean saveExisting(UserActivity item) {
        ContentValues values = item.getSetValues();
        if (values == null || values.size() == 0) {
            return false;
        }
        return dao.saveExisting(item);
    }

    public void getCommentsForTask(String taskUuid, Callback<UserActivity> callback) {
        dao.query(callback, Query.select(UserActivity.PROPERTIES).where(
                Criterion.and(UserActivity.ACTION.eq(UserActivity.ACTION_TASK_COMMENT),
                        UserActivity.TARGET_ID.eq(taskUuid),
                        UserActivity.DELETED_AT.eq(0))
        ).orderBy(Order.desc("1")));
    }
}
