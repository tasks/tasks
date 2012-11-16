package com.todoroo.astrid.actfm.sync.messages;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.DaoReflectionHelpers;
import com.todoroo.astrid.dao.OutstandingEntryDao;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.RemoteModel;

public class ReplayOutstandingEntries<T extends RemoteModel, OE extends OutstandingEntry<T>> {

    private final Class<T> modelClass;
    private final Class<OE> outstandingClass;
    private final RemoteModelDao<T> dao;
    private final OutstandingEntryDao<OE> outstandingDao;

    public ReplayOutstandingEntries(Class<T> modelClass, RemoteModelDao<T> dao, OutstandingEntryDao<OE> outstandingDao) {
        this.modelClass = modelClass;
        this.outstandingClass = DaoReflectionHelpers.getOutstandingClass(modelClass);
        this.dao = dao;
        this.outstandingDao = outstandingDao;
    }

    public void execute() {
        TodorooCursor<OE> outstanding = outstandingDao.query(Query.select(DaoReflectionHelpers.getModelProperties(outstandingClass))
                .orderBy(Order.asc(OutstandingEntry.ENTITY_ID_PROPERTY), Order.asc(OutstandingEntry.CREATED_AT_PROPERTY)));
        try {
            // Do and apply
        } finally {
            outstanding.close();
        }
    }

}
