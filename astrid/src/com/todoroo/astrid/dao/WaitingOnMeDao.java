package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.WaitingOnMe;

public class WaitingOnMeDao extends RemoteModelDao<WaitingOnMe> {

    @Autowired
    private Database database;

    public WaitingOnMeDao() {
        super(WaitingOnMe.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    protected boolean shouldRecordOutstandingEntry(String columnName, Object value) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_WAITING_ON_ME, columnName);
    }

    public WaitingOnMe findByTask(String taskUuid) {
        TodorooCursor<WaitingOnMe> cursor = query(Query.select(WaitingOnMe.PROPERTIES).where(
                Criterion.and(WaitingOnMe.TASK_UUID.eq(taskUuid),
                        Criterion.or(WaitingOnMe.ACKNOWLEDGED.eq(0), WaitingOnMe.ACKNOWLEDGED.isNull()),
                        Criterion.or(WaitingOnMe.DELETED_AT.eq(0), WaitingOnMe.DELETED_AT.isNull()))));
        cursor.moveToFirst();
        return returnFetchResult(cursor);
    }
}
