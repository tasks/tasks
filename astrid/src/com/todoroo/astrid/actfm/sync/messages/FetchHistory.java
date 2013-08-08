package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmSyncThread.SyncMessageCallback;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.RemoteModel;

public class FetchHistory<TYPE extends RemoteModel> {

    public FetchHistory(RemoteModelDao<TYPE> dao, LongProperty historyTimeProperty, IntegerProperty historyHasMoreProperty,
                        String table, String uuid, String taskTitle, long modifiedAfter, int offset, SyncMessageCallback done) {
        DependencyInjectionService.getInstance().inject(this);
    }

    public void execute() {
    }
}
