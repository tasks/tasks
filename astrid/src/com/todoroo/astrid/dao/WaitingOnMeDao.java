package com.todoroo.astrid.dao;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
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
}
