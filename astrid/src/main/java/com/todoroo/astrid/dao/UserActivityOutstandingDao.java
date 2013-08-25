package com.todoroo.astrid.dao;

import com.todoroo.astrid.data.UserActivityOutstanding;

public class UserActivityOutstandingDao extends OutstandingEntryDao<UserActivityOutstanding> {

    public UserActivityOutstandingDao() {
        super(UserActivityOutstanding.class);
    }

}
