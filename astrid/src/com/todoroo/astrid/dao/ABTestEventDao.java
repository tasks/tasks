package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.ABTestEvent;

public class ABTestEventDao extends DatabaseDao<ABTestEvent> {

    @Autowired
    private Database database;

    public ABTestEventDao() {
        super(ABTestEvent.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    public void createInitialTestEvent(String testName, String testVariant,
            boolean newUser, boolean activeUser) {
        ABTestEvent event = new ABTestEvent();
        event.setValue(ABTestEvent.TEST_NAME, testName);
        event.setValue(ABTestEvent.TEST_VARIANT, testVariant);
        event.setValue(ABTestEvent.NEW_USER, newUser ? 1 : 0);
        event.setValue(ABTestEvent.ACTIVATED_USER, activeUser ? 1 : 0);
        event.setValue(ABTestEvent.TIME_INTERVAL, ABTestEvent.TIME_INTERVAL_0);
        event.setValue(ABTestEvent.DATE_RECORDED, DateUtilities.now());

        createNew(event);
    }

    public boolean createTestEventWithTimeInterval(String testName, int timeInterval) {
        TodorooCursor<ABTestEvent> existing = query(Query.select(ABTestEvent.PROPERTIES)
                .where(ABTestEvent.TEST_NAME.eq(testName)).orderBy(Order.asc(ABTestEvent.TIME_INTERVAL)));

        try {
            if (existing.getCount() == 0)
                return false;

            existing.moveToLast();
            ABTestEvent item = new ABTestEvent(existing);
            int lastRecordedTimeIntervalIndex = AndroidUtilities.indexOf(
                    ABTestEvent.TIME_INTERVALS, item.getValue(ABTestEvent.TIME_INTERVAL));

            int currentTimeIntervalIndex = AndroidUtilities.indexOf(
                    ABTestEvent.TIME_INTERVALS, timeInterval);

            if (lastRecordedTimeIntervalIndex < 0 || currentTimeIntervalIndex < 0
                    || lastRecordedTimeIntervalIndex >= currentTimeIntervalIndex)
                return false;

            long now = DateUtilities.now();
            for (int i = lastRecordedTimeIntervalIndex + 1; i <= currentTimeIntervalIndex; i++) {
                item.clearValue(ABTestEvent.ID);
                item.setValue(ABTestEvent.TIME_INTERVAL, ABTestEvent.TIME_INTERVALS[i]);
                item.setValue(ABTestEvent.DATE_RECORDED, now);
                createNew(item);
            }
        } finally {
            existing.close();
        }
        return true;
    }

}
